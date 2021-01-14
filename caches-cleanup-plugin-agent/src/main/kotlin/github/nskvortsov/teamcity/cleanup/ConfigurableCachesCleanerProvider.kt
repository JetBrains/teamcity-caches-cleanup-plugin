/*
 * Copyright 2016-2021 JetBrains s.r.o.
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
import org.apache.log4j.Logger
import java.io.File
import java.util.*

class ConfigurableCachesCleanerProvider : DirectoryCleanersProvider {
    companion object {
        val log: Logger = Logger.getLogger(ConfigurableCachesCleanerProvider::class.java)

        const val DIRECTORIES_PROPERTY = "teamcity.cleaners.configurable.directories"
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

        for (path in candidates) {
            val dir = File(replaceHomeDir(path, home))
            log.debug("Checking if '$path' exists")
            if (dir.exists() && dir.isDirectory) {
                log.debug("Found '$path', registering cleaner.")
                registry.addCleaner(dir, Date(), Cleaner(dir, log))
            }
        }
    }

    private fun replaceHomeDir(path: String, home: String): String {
        if (path.startsWith("~/")) return home.removeSuffix("/") + "/" + path.removePrefix("~/")
        return path
    }

    override fun getCleanerName() = "Configurable local caches cleaner"
}