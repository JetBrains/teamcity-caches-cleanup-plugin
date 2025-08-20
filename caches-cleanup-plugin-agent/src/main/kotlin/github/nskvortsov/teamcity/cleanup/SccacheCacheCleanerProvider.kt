/*
 * Copyright 2016-2025 JetBrains s.r.o.
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

import com.intellij.openapi.util.SystemInfo
import jetbrains.buildServer.agent.DirectoryCleanersProvider
import jetbrains.buildServer.agent.DirectoryCleanersProviderContext
import jetbrains.buildServer.agent.DirectoryCleanersRegistry
import org.apache.log4j.Logger
import java.io.File
import java.util.*

class SccacheCacheCleanerProvider : DirectoryCleanersProvider {
    companion object {
        private var log: Logger = Logger.getLogger(SccacheCacheCleanerProvider::class.java)

        internal fun getCacheDirectory(home: String?): String? {
            return when {
                SystemInfo.isWindows -> System.getenv("LOCALAPPDATA")?.let { "$it\\Mozilla\\sccache" }
                SystemInfo.isMac -> home?.let { "$it/Library/Caches/Mozilla.sccache" }
                SystemInfo.isLinux -> home?.let { "$it/.cache/sccache" }
                else -> {
                    log.info("Unknown OS (${System.getProperty("os.name")}), sccache cleaner is disabled.");
                    null
                }
            }
        }
    }

    override fun registerDirectoryCleaners(
        context: DirectoryCleanersProviderContext,
        registry: DirectoryCleanersRegistry
    ) {
        log.debug("sccache cleaner: register dir cleaners")
        val disabled = context.hasExplicitFalse("teamcity.cleaners.sccache.enabled")
        if (disabled) {
            log.info("sccache cleaner is disabled by property.")
            return
        }
        val home = System.getProperty("user.home")
        val path: String? = getCacheDirectory(home)
        if (path == null) {
            log.info("Cannot determine path to sccache cache directory, sccache cleaner is disabled.")
            return
        }
        val file = File(path)
        if (file.exists()) {
            log.info("sccache cache directory found, registering cleaner.")
            registry.addCleaner(file, Date(), Cleaner(file, log))
            return
        }
    }

    override fun getCleanerName() = "sccache local cache cleaner"
}