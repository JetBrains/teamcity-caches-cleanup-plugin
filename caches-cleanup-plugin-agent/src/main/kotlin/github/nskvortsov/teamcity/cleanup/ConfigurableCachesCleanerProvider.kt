

package github.nskvortsov.teamcity.cleanup

import jetbrains.buildServer.agent.DirectoryCleanersProvider
import jetbrains.buildServer.agent.DirectoryCleanersProviderContext
import jetbrains.buildServer.agent.DirectoryCleanersRegistry
import org.apache.log4j.Logger
import java.io.File
import java.util.*

class ConfigurableCachesCleanerProvider : DirectoryCleanersProvider {
    companion object {
        val log: Logger = Logger.getLogger(ConfigurableCachesCleanerProvider::class.java)

        const val DIRECTORIES_PROPERTY = "teamcity.cleaners.configurable.directories"
        const val REMOVE_ROOTS = "teamcity.internal.cleaners.configurable.removeCachesRoots"
        const val ENABLED_PROPERTY = "teamcity.cleaners.configurable.enabled"
    }

    override fun registerDirectoryCleaners(context: DirectoryCleanersProviderContext, registry: DirectoryCleanersRegistry) {
        log.debug("Configurable cache cleaner: register dir cleaners")
        val disabled = context.hasExplicitFalse(ENABLED_PROPERTY)
        if (disabled) {
            log.info("Configurable cleaner is disabled, skipping.")
            return
        }
        val property = context.runningBuild.sharedConfigParameters[DIRECTORIES_PROPERTY]
                ?: return log.info("Configurable cleaner skipped: '$DIRECTORIES_PROPERTY' property undefined.")

        val home = System.getProperty("user.home")
                ?: return log.warn("Failed to detect user home directory, system property 'user.home' is not present")

        val candidates: List<String> = property.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        log.debug("Configurable cleaner: dirs to cleanup: $candidates")

        val shouldRemoveRoots = context.runningBuild.sharedConfigParameters[REMOVE_ROOTS] ?: "true"

        for (path in candidates) {
            val dir = File(replaceHomeDir(path, home))
            log.debug("Checking if '$path' exists")
            if (dir.exists() && dir.isDirectory) {
                log.debug("Found '$path', registering cleaner.")
                registry.addCleaner(dir, Date(), Cleaner(dir, log, shouldRemoveRoots.toBoolean()))
            }
        }
    }

    private fun replaceHomeDir(path: String, home: String): String {
        if (path.startsWith("~/")) return home.removeSuffix("/") + "/" + path.removePrefix("~/")
        return path
    }

    override fun getCleanerName() = "Configurable local caches cleaner"
}