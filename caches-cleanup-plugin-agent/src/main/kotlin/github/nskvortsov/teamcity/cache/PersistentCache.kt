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

package github.nskvortsov.teamcity.cache

import jetbrains.buildServer.agent.*
import jetbrains.buildServer.util.EventDispatcher
import jetbrains.buildServer.util.FileUtil
import java.io.File
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Nikita.Skvortsov
 * date: 09.04.2016.
 */

class PersistentCacheWithCleaners(agentDispatcher: EventDispatcher<AgentLifeCycleListener>) : DirectoryCleanersProvider {
    var cacheDirectory: File by Delegates.notNull()

    init {
        agentDispatcher.addListener(object: AgentLifeCycleAdapter() {
            override fun agentInitialized(agent: BuildAgent) {
                cacheDirectory = agent.configuration.getCacheDirectory(".persistent_cache")
                FileUtil.createDir(cacheDirectory)
                agent.configuration.addSystemProperty("agent.persistent.cache", cacheDirectory.absolutePath)
            }
        })
    }

    override fun registerDirectoryCleaners(context: DirectoryCleanersProviderContext, registry: DirectoryCleanersRegistry) {
        cacheDirectory.listFiles().forEach {
            registry.addCleaner(it, Date(it.lastModified()))
        }
    }

    override fun getCleanerName() = "Persistent agent cache cleaner"
}