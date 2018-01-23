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