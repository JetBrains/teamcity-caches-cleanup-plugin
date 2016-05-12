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
        log.debug("Gradle cache cleaner: register dir cleaners")
        val disabled = context.hasExplicitFalse("teamcity.cleaners.gradle.enabled")
        if (disabled) {
            log.info("Gradle cache cleaner is disabled, skipping")
            return
        }
        System.getProperty("user.home")?.let {
            val gradleCache = File(it + "/.gradle/caches")
            log.debug("Checking if [${gradleCache.absolutePath}] exists")
            if (gradleCache.exists()) {
                log.debug("Gradle cache found, registering cleaner.")
                registry.addCleaner(gradleCache, Date(), Cleaner(gradleCache, log))
            }
        }
    }

    override fun getCleanerName() = "Gradle local cache cleaner"
}

class GradleWrapperDistsCleanerProvider : DirectoryCleanersProvider {

    val log = Logger.getLogger(GradleCacheCleanerProvider::class.java)
    val name = "Gradle Wrapper dist cleaner"

    override fun registerDirectoryCleaners(context: DirectoryCleanersProviderContext, registry: DirectoryCleanersRegistry) {

        log.debug("$name: register cleaners")
        val disabled = context.hasExplicitFalse("teamcity.cleaners.gradle.enabled")
        if (disabled) {
            log.info("$name disabled, skipping")
            return
        }
        System.getProperty("user.home")?.let {
            val wrapperCache = File(it + "/.gradle/wrapper/dists")
            log.debug("Checking if [${wrapperCache.absolutePath}] exists")
            if (wrapperCache.exists()) {
                log.debug("$name: ${wrapperCache.name} found, registering cleaner.")
                registry.addCleaner(wrapperCache, Date(), Cleaner(wrapperCache, log))
            }
        }
    }

    override fun getCleanerName() = name;
}

fun DirectoryCleanersProviderContext.hasExplicitFalse(key: String): Boolean {
    val strValue = runningBuild.sharedConfigParameters[key]
    return strValue?.let { it.equals("false", ignoreCase = true) } ?: false
}

class Cleaner(val dir: File, val log: Logger): Runnable {
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
