package github.nskvortsov.teamcity.cache

import jetbrains.buildServer.agent.*
import jetbrains.buildServer.parameters.ReferencesResolverUtil
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
        private const val WhitelistSeparator = ","
        private const val PersistentCacheParamName = "agent.persistent.cache"
    }

    init {
        agentDispatcher.addListener(object : AgentLifeCycleAdapter() {
            override fun agentInitialized(agent: BuildAgent) {
                val configuration = agent.configuration

                cacheDirectory = configuration.getCacheDirectory(".persistent_cache")
                FileUtil.createDir(cacheDirectory)

                configuration.addSystemProperty(PersistentCacheParamName, cacheDirectory.absolutePath)
                // Ensure it's possible to download artifact dependencies into persistent cache
                // Restrictor introduced in TeamCity 2018.1
                val persistentCacheParamReference = ReferencesResolverUtil.makeReference(PersistentCacheParamName)
                var whiteList = configuration.configurationParameters[ArtifactRestrictorWhitelistProperty]
                whiteList = if (whiteList.isNullOrBlank())
                    persistentCacheParamReference else
                    listOf(whiteList.trim(), persistentCacheParamReference).joinToString(WhitelistSeparator)
                configuration.addConfigurationParameter(ArtifactRestrictorWhitelistProperty, whiteList)
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