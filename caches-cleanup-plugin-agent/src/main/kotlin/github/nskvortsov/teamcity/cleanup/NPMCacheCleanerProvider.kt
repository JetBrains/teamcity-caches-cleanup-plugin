

package github.nskvortsov.teamcity.cleanup

import com.intellij.openapi.util.SystemInfo
import jetbrains.buildServer.agent.DirectoryCleanersProvider
import jetbrains.buildServer.agent.DirectoryCleanersProviderContext
import jetbrains.buildServer.agent.DirectoryCleanersRegistry
import org.apache.log4j.Logger
import java.io.File
import java.util.*

class NPMCacheCleanerProvider : DirectoryCleanersProvider {
    companion object {
        val log: Logger = Logger.getLogger(NPMCacheCleanerProvider::class.java)

        private fun getAppDataOrHome(home: String): File {
            val location = System.getenv("APPDATA")
            if (location != null && File(location).exists()) return File(location)
            return File(home)
        }
    }

    override fun registerDirectoryCleaners(context: DirectoryCleanersProviderContext, registry: DirectoryCleanersRegistry) {
        log.debug("NPM cache cleaner: register dir cleaners")
        val disabled = context.hasExplicitFalse("teamcity.cleaners.npm.enabled")
        if (disabled) {
            log.info("NPM repository cleaner is disabled, skipping.")
            return
        }
        val home = System.getProperty("user.home")
                ?: return log.warn("Failed to detect user home directory, system property 'user.home' is not present")

        val npmRepo: File =
                if (SystemInfo.isWindows) {
                    File(getAppDataOrHome(home), "npm-cache")
                } else {
                    File("$home/.npm")
                }
        log.debug("Checking if [${npmRepo.absolutePath}] exists")
        if (npmRepo.exists()) {
            log.debug("NPM cache found at '${npmRepo.path}', registering cleaner.")
            registry.addCleaner(npmRepo, Date(), Cleaner(npmRepo, log))
        }
    }

    override fun getCleanerName() = "NPM local cache cleaner"
}