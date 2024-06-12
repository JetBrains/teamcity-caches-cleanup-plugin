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
        private const val ArtifactRestrictorAllowedListProperty = "teamcity.artifactDependenciesResolution.allowedList"
        private const val AllowedListSeparator = ","
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
                var allowedList = configuration.configurationParameters[ArtifactRestrictorAllowedListProperty]
                allowedList = if (allowedList.isNullOrBlank())
                    persistentCacheParamReference else
                    listOf(allowedList.trim(), persistentCacheParamReference).joinToString(AllowedListSeparator)
                configuration.addConfigurationParameter(ArtifactRestrictorAllowedListProperty, allowedList)
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