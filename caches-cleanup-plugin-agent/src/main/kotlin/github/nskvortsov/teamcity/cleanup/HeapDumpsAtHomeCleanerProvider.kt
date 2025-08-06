package github.nskvortsov.teamcity.cleanup

import jetbrains.buildServer.agent.DirectoryCleanersProvider
import jetbrains.buildServer.agent.DirectoryCleanersProviderContext
import jetbrains.buildServer.agent.DirectoryCleanersRegistry
import jetbrains.buildServer.util.FileUtil
import org.apache.log4j.Logger
import java.io.File
import java.util.*

class HeapDumpsAtHomeCleanerProvider : DirectoryCleanersProvider {
    companion object {
        val log: Logger = Logger.getLogger(HeapDumpsAtHomeCleanerProvider::class.java)

        val filter: (File, String) -> Boolean = { _, name -> name.endsWith(".hprof") }
    }

    override fun registerDirectoryCleaners(
        context: DirectoryCleanersProviderContext,
        registry: DirectoryCleanersRegistry
    ) {
        log.debug("heap dumps at home cleaner: register dir cleaners")
        val disabled = context.hasExplicitFalse("teamcity.cleaners.heapdumps.enabled")
        if (disabled) {
            log.info("heap dumps at home cleaner is disabled, skipping.")
            return
        }
        val home = System.getProperty("user.home")?.let { File(it) }
            ?: return log.warn("Failed to detect user home directory, system property 'user.home' is not present")

        log.debug("Checking if hprof files present in the home directory")
        val dumps = home.list(filter)
        if (dumps != null && dumps.isNotEmpty()) {
            log.debug("Heap dumps found, registering cleaner.")
            registry.addCleaner(home, Date(), HeapDumpsCleaner(home, log))
        }
    }

    override fun getCleanerName() = "heap dumps at home cleaner"
}

class HeapDumpsCleaner(private val dir: File, private val log: Logger) : Runnable {
    override fun run() {
        log.debug("Removing hprof files from ${dir.absolutePath}")
        val dumps = dir.listFiles(HeapDumpsAtHomeCleanerProvider.filter) ?: return
        for (file in dumps) {
            log.debug("Removing hprof file ${file.absolutePath}")
            FileUtil.delete(file)
        }
    }
}