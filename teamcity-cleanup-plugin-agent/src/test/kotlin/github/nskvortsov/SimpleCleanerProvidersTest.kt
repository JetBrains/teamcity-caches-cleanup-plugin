package github.nskvortsov

import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.DirectoryCleanersProviderContext
import jetbrains.buildServer.agent.DirectoryCleanersRegistry
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.*
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File
import java.util.*
import kotlin.properties.Delegates

@Test
class SimpleCleanerProvidersTest {

    var registryMap: MutableMap<File, Date> by Delegates.notNull()
    var context: DirectoryCleanersProviderContext by Delegates.notNull()
    var registry: DirectoryCleanersRegistry by Delegates.notNull()
    var runningBuild: AgentRunningBuild by Delegates.notNull()

    @BeforeMethod
    fun setUp() {
        registryMap = HashMap<File, Date>()
        context = mock(DirectoryCleanersProviderContext::class.java)
        registry = mock(DirectoryCleanersRegistry::class.java)
        runningBuild = mock(AgentRunningBuild::class.java)
        `when`(registry.addCleaner(any(),any())).thenAnswer { registryMap.put(it.arguments[0] as File, it.arguments[1] as Date) }
        `when`(context.runningBuild).thenAnswer { runningBuild }
    }

    fun testMavenProvider() {
        val provider = MavenCacheCleanerProvider()
        provider.registerDirectoryCleaners(context, registry)
        assertThat(registryMap).containsKey(File("${System.getProperty("user.home")}/.m2/repository"))
    }
}