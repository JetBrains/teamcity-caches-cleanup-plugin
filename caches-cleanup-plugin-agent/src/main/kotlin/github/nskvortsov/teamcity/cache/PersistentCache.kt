

package github.nskvortsov.teamcity.cache

import jetbrains.buildServer.agent.*
import jetbrains.buildServer.util.EventDispatcher
import jetbrains.buildServer.util.FileUtil
import java.io.File
import java.util.*

/**
 * Created by Nikita.Skvortsov
 * date: 09.04.2016.
 */

class PersistentCacheWithCleaners(agentDispatcher: EventDispatcher<AgentLifeCycleListener>) : DirectoryCleanersProvider {
    lateinit var cacheDirectory: File

    companion object {
        private const val ArtifactRestrictorWhitelistProperty = "teamcity.artifactDependenciesResolution.whiteList"
    }

    init {
        agentDispatcher.addListener(object: AgentLifeCycleAdapter() {
            override fun agentInitialized(agent: BuildAgent) {
                val configuration = agent.configuration

                cacheDirectory = configuration.getCacheDirectory(".persistent_cache")
                FileUtil.createDir(cacheDirectory)

                configuration.addSystemProperty("agent.persistent.cache", cacheDirectory.absolutePath)
                // Ensure it's possible to download artifact dependencies into persistent cache
                // Restrictor introduced in TeamCity 2018.1
                configuration.addConfigurationParameter(ArtifactRestrictorWhitelistProperty,
                        configuration.configurationParameters.getOrDefaultCustom(ArtifactRestrictorWhitelistProperty, "")
                                + ";%agent.persistent.cache%")
            }
        })
    }

    override fun registerDirectoryCleaners(context: DirectoryCleanersProviderContext, registry: DirectoryCleanersRegistry) {
        cacheDirectory.listFiles()?.forEach {
            registry.addCleaner(it, Date(it.lastModified()))
        }
    }

    override fun getCleanerName() = "Persistent agent cache cleaner"
}

private fun <K,V> MutableMap<K,V>.getOrDefaultCustom(key: Any?, defaultValue: V?): V? {
    val v = get(key)
    if (v != null) return v
    if (containsKey(key)) return null
    return defaultValue
}