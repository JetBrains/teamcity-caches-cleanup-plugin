package github.nskvortsov.teamcity.cache

import jetbrains.buildServer.agent.AgentLifeCycleListener
import jetbrains.buildServer.agent.BuildAgent
import jetbrains.buildServer.agent.BuildAgentConfiguration
import jetbrains.buildServer.util.EventDispatcher
import jetbrains.buildServer.util.FileUtil
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.*
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File

class PersistentCacheWithCleanersTest {
    private lateinit var agentDispatcherMock: EventDispatcher<AgentLifeCycleListener>
    private lateinit var agentConfigurationMock: BuildAgentConfiguration
    private lateinit var agentMock: BuildAgent
    private lateinit var tempDir: File
    private var listener: AgentLifeCycleListener? = null

    @BeforeMethod
    fun setUp() {
        tempDir = FileUtil.createTempDirectory("test", "cleanup")
        @Suppress("UNCHECKED_CAST") // Mockito doesn't support generics inference https://github.com/mockito/mockito/issues/1531
        agentDispatcherMock = mock(EventDispatcher::class.java) as EventDispatcher<AgentLifeCycleListener>
        `when`(agentDispatcherMock.addListener(any(AgentLifeCycleListener::class.java))).then {
            listener = (it.arguments[0] as AgentLifeCycleListener)
            Unit
        }
        agentConfigurationMock = mock(BuildAgentConfiguration::class.java)
        agentMock = mock(BuildAgent::class.java)
        `when`(agentMock.configuration).thenReturn(agentConfigurationMock)
    }

    @AfterMethod
    fun tearDown() {
        FileUtil.delete(tempDir)
        listener = null
    }

    @Test
    fun `should register preparing listener in event dispatcher when instantiating`() {
        // act
        PersistentCacheWithCleaners(agentDispatcherMock);

        // assert
        verify(agentDispatcherMock).addListener(any());
        assertThat(listener).isNotNull
    }

    @DataProvider
    fun whiteListTestData() = arrayOf(
        arrayOf("some/path", "some/path,%agent.persistent.cache%"),
        arrayOf("D:\\some\\path", "D:\\some\\path,%agent.persistent.cache%"),
        arrayOf("", "%agent.persistent.cache%"),
        arrayOf(" ", "%agent.persistent.cache%"),
        arrayOf("\t", "%agent.persistent.cache%"),
        arrayOf(null, "%agent.persistent.cache%")
    )

    @Test(dataProvider = "whiteListTestData")
    fun `should make preparations when agent initialized`(whiteList: String?, updatedWhiteList: String) {
        // arrange
        PersistentCacheWithCleaners(agentDispatcherMock)
        val cacheDir = File(tempDir, "cacheDir")
        `when`(agentConfigurationMock.getCacheDirectory(any())).thenAnswer { cacheDir }
        val whitelistProperty = "teamcity.artifactDependenciesResolution.whiteList"
        val parameters = if (whiteList == null) mapOf() else mapOf(whitelistProperty to whiteList)
        `when`(agentConfigurationMock.configurationParameters).thenAnswer { parameters }

        // act
        listener?.agentInitialized(agentMock)

        // assert
        assertThat(cacheDir).exists()
        verify(agentConfigurationMock).addSystemProperty("agent.persistent.cache", cacheDir.absolutePath)
        verify(agentConfigurationMock).configurationParameters
        verify(agentConfigurationMock).addConfigurationParameter(whitelistProperty, updatedWhiteList)
    }
}