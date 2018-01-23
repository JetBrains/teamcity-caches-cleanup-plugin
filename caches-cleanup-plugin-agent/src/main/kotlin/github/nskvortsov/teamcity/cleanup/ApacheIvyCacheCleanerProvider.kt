/*
 * Copyright 2016-2018 JetBrains s.r.o.
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

class ApacheIvyCacheCleanerProvider : DirectoryCleanersProvider {
    companion object {
        val log: Logger = Logger.getLogger(ApacheIvyCacheCleanerProvider::class.java)
    }

    override fun registerDirectoryCleaners(context: DirectoryCleanersProviderContext, registry: DirectoryCleanersRegistry) {
        log.debug("Apache Ivy cache cleaner: register dir cleaners")
        val disabled = context.hasExplicitFalse("teamcity.cleaners.ivy.enabled")
        if (disabled) {
            log.info("Apache Ivy repository cleaner is disabled, skipping.")
            return
        }
        val home = System.getProperty("user.home")
                ?: return log.warn("Failed to detect user home directory, system property 'user.home' is not present")

        val ivyRepo = File("$home/.ivy2/cache")
        log.debug("Checking if '${ivyRepo.absolutePath}' exists")
        if (ivyRepo.exists()) {
            log.debug("Apache Ivy cache found, registering cleaner.")
            registry.addCleaner(ivyRepo, Date(), Cleaner(ivyRepo, log))
        }
    }

    override fun getCleanerName() = "Apache Ivy local cache cleaner"
}