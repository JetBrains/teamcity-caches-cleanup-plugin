package github.nskvortsov.teamcity.cleanup

import jetbrains.buildServer.agent.DirectoryCleanersProvider
import jetbrains.buildServer.agent.DirectoryCleanersProviderContext
import jetbrains.buildServer.agent.DirectoryCleanersRegistry
import java.io.File
import java.util.*

class MavenCacheCleanerProvider : DirectoryCleanersProvider {

    var log = Logger.getLogger(MavenCacheCleanerProvider::class.java)

    override fun registerDirectoryCleaners(context: DirectoryCleanersProviderContext, registry: DirectoryCleanersRegistry) {
        val disabled = context.hasExplicitFalse("teamcity.cleaners.maven.enabled")
        if (disabled) {
            log.info("Maven repository cleaner is disabled, skipping.")
            return
        }
        System.getProperty("user.home")?.let {
            val m2repo = File(it + "/.m2/repository")
            if (m2repo.exists()) {
                registry.addCleaner(m2repo, Date())
            }
        }
    }

    override fun getCleanerName() = "Maven local cache cleaner"
}

class GradleCacheCleanerProvider : DirectoryCleanersProvider {

    val log = Logger.getLogger(GradleCacheCleanerProvider::class.java)

    override fun registerDirectoryCleaners(context: DirectoryCleanersProviderContext, registry: DirectoryCleanersRegistry) {
        val disabled = context.hasExplicitFalse("teamcity.cleaners.gradle.enabled")
        if (disabled) {
            log.info("Gradle cache cleaner is disabled, skipping")
            return
        }
        System.getProperty("user.home")?.let {
            val gradleCache = File(it + "/.gradle/caches")
            if (gradleCache.exists()) {
                registry.addCleaner(gradleCache, Date())
            }
        }
    }

    override fun getCleanerName() = "Gradle local cache cleaner"
}

fun DirectoryCleanersProviderContext.hasExplicitFalse(key: String): Boolean {
    val strValue = runningBuild.sharedConfigParameters[key]
    return strValue?.let { it.equals("false", ignoreCase = true) } ?: false
}


