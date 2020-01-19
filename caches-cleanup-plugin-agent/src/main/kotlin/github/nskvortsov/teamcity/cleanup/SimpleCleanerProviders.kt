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
import org.apache.log4j.Logger
import java.io.File
import java.util.*

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

class GradleCacheCleanerProvider : DirectoryCleanersProvider {

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
            daemonLogs.walkTopDown().maxDepth(3).filter { it.name.endsWith("out.log") }.forEach {
                registry.addCleaner(it, Date(it.lastModified()))
                count++
            }
            log.debug("Finished, found and registered for cleaning $count files")

        }
    }

    override fun getCleanerName() = "Gradle local cache cleaner"
}


fun DirectoryCleanersProviderContext.hasExplicitFalse(key: String): Boolean {
    val strValue = runningBuild.sharedConfigParameters[key]
    return strValue?.equals("false", ignoreCase = true) ?: false
}

class Cleaner(private val dir: File, private val log: Logger): Runnable {
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
