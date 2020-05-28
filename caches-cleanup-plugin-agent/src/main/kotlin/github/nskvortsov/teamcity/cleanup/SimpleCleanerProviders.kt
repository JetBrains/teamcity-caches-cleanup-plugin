/*
 * Copyright 2016-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package github.nskvortsov.teamcity.cleanup

import jetbrains.buildServer.agent.DirectoryCleanersProvider
import jetbrains.buildServer.agent.DirectoryCleanersProviderContext
import jetbrains.buildServer.agent.DirectoryCleanersRegistry
import jetbrains.buildServer.util.FileUtil
import jetbrains.buildServer.util.TimeService
import org.apache.log4j.Logger
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class MavenCacheCleanerProvider : DirectoryCleanersProvider {

    var log = Logger.getLogger(MavenCacheCleanerProvider::class.java)

    override fun registerDirectoryCleaners(context: DirectoryCleanersProviderContext, registry: DirectoryCleanersRegistry) {
        log.debug("Maven cache cleaner: register dir cleaners")
        val disabled = context.hasExplicitFalse("teamcity.cleaners.maven.enabled")
        if (disabled) {
            log.info("Maven repository cleaner is disabled, skipping.")
            return
        }
        System.getProperty("user.home")?.let { home ->
            val m2repo = File("$home/.m2/repository")
            log.debug("Checking if [${m2repo.absolutePath}] exists")
            if (m2repo.exists()) {
                log.debug("Maven cache found, registering cleaner.")
                registry.addCleaner(m2repo, Date(), Cleaner(m2repo, log))
            }
        }
    }

    override fun getCleanerName() = "Maven local cache cleaner"
}

class GradleCacheCleanerProvider(private val TimeService: TimeService) : DirectoryCleanersProvider {

    val log = Logger.getLogger(GradleCacheCleanerProvider::class.java)

    override fun registerDirectoryCleaners(context: DirectoryCleanersProviderContext, registry: DirectoryCleanersRegistry) {
        log.debug("Gradle cleaner: register dir cleaners")
        val disabled = context.hasExplicitFalse("teamcity.cleaners.gradle.enabled")
        if (disabled) {
            log.info("Gradle cleaner is disabled, skipping")
            return
        }
        System.getProperty("user.home")?.let {
            val gradleCache = File(it + "/.gradle/caches")
            log.debug("Checking if [${gradleCache.absolutePath}] exists")
            if (gradleCache.exists()) {
                log.debug("Gradle cache found, registering cleaner.")
                registry.addCleaner(gradleCache, Date(), Cleaner(gradleCache, log))
            }

            val wrapperCache = File(it + "/.gradle/wrapper/dists")
            log.debug("Checking if [${wrapperCache.absolutePath}] exists")
            if (wrapperCache.exists()) {
                log.debug("Gradle wrapper distributions found, registering cleaner.")
                registry.addCleaner(wrapperCache, Date(), Cleaner(wrapperCache, log))
            }

            val daemonLogs = File(it + "/.gradle/daemon")
            log.debug("Looking for Gradle daemon logs.")
            var count = 0

            val now = TimeService.now()

            (daemonLogs.listFiles() ?: emptyArray()).filter { dir -> dir.isDirectory }.forEach { dir ->
                val isLogFile: (File) -> Boolean = { file -> file.name.endsWith("out.log") }
                val logs = (dir.listFiles() ?: emptyArray()).filter(isLogFile)

                // Let's split logs and register several cleaners:
                //  * older than 7 days
                //  * older than 1 day
                //  * older than 12 hours
                //  * each one else as separate cleaner
                val groups = splitLogFiles(logs, now)

                @Suppress("NAME_SHADOWING")
                for ((type, logs) in groups.entries) {
                    if (type == LogAge.FRESH) {
                        // register each file separately
                        logs.forEach { log ->
                            registry.addCleaner(log, Date(log.lastModified()), LogFileCleaner(log, dir))
                        }
                    } else {
                        // register group
                        registry.addCleaner(dir, Date(now - type.millis), LogGroupCleaner(logs, dir))
                    }
                }
                if (groups.isNotEmpty()) {
                    count++
                }
            }
            log.debug("Finished, found and registered for cleaning $count daemon directories")

        }
    }

    override fun getCleanerName() = "Gradle local cache cleaner"
}

private enum class LogAge(private val offset: Long) {
    OLDER_THAN_7D(7 * 24),
    OLDER_THAN_24H(24),
    OLDER_THAN_12H(12),
    FRESH(0);

    val millis: Long
        get() {
            return TimeUnit.HOURS.toMillis(offset)
        }
}

private fun splitLogFiles(logs: List<File>, now: Long): Map<LogAge, List<File>> {
    return logs.groupBy {
        val diff = now - it.lastModified()
        LogAge.values().firstOrNull { age -> diff > age.millis } ?: LogAge.FRESH
    }
}

private class LogGroupCleaner(private val logs: List<File>, private val dir: File) : Runnable {
    override fun run() {
        FileUtil.deleteFiles(logs)
        if (dir.list()?.isEmpty() == true) {
            FileUtil.delete(dir)
        }
    }

}

private class LogFileCleaner(private val log: File, private val dir: File) : Runnable {
    override fun run() {
        FileUtil.delete(log)
        if (dir.list()?.isEmpty() == true) {
            FileUtil.delete(dir)
        }
    }
}


fun DirectoryCleanersProviderContext.hasExplicitFalse(key: String): Boolean {
    val strValue = runningBuild.sharedConfigParameters[key]
    return strValue?.equals("false", ignoreCase = true) ?: false
}

class Cleaner(private val dir: File, private val log: Logger) : Runnable {
    override fun run() {
        log.debug("Removing ${dir.absolutePath}")
        val dirOld = File("$dir.old")
        val movedSuccessfully = FileUtil.moveDirWithContent(dir, dirOld,
                { log.info("Failed to rename to ${dirOld.name}: $it") })
        if (movedSuccessfully) {
            log.debug("Rename successful, deleting ${dirOld.name}")
            FileUtil.delete(dirOld)
        }
    }
}
