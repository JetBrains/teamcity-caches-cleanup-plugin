

package github.nskvortsov

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.ObjectUtils.assertNotNull
import github.nskvortsov.teamcity.cleanup.*
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.DirectoryCleanersProviderContext
import jetbrains.buildServer.agent.DirectoryCleanersRegistry
import jetbrains.buildServer.util.FileUtil
import jetbrains.buildServer.util.SystemTimeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assume
import org.mockito.Mockito.*
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File
import java.util.concurrent.TimeUnit

class SimpleCleanerProvidersTest {

    lateinit var registryMap: MutableMap<File, Runnable>
    lateinit var context: DirectoryCleanersProviderContext
    lateinit var registry: DirectoryCleanersRegistry
    lateinit var runningBuild: AgentRunningBuild

    lateinit var tempDir: File
    var oldHome: String? = null

    @BeforeMethod
    fun setUp() {
        tempDir = FileUtil.createTempDirectory("test", "cleanup")
        FileUtil.copyDir(File("src/test/resources/testData"), tempDir)
        oldHome = System.getProperty("user.home")
        System.setProperty("user.home", tempDir.absolutePath)


        registryMap = HashMap<File, Runnable>()
        context = mock(DirectoryCleanersProviderContext::class.java)
        registry = mock(DirectoryCleanersRegistry::class.java)
        runningBuild = mock(AgentRunningBuild::class.java)
        `when`(registry.addCleaner(any(), any(), any())).thenAnswer { registryMap.put(it.arguments[0] as File, it.arguments[2] as Runnable) }
        `when`(registry.addCleaner(any(), any())).thenAnswer { registryMap.put(it.arguments[0] as File, Runnable { (it.arguments[0] as File).delete() } ) }
        `when`(context.runningBuild).thenAnswer { runningBuild }
    }

    @AfterMethod
    fun tearDown() {
        oldHome?.let { System.setProperty("user.home", it) }
        FileUtil.delete(tempDir)
    }

    @Test
    fun testMavenProvider() {
        val provider = MavenCacheCleanerProvider()
        val m2repo = File("${System.getProperty("user.home")}/.m2/repository")
        provider.registerDirectoryCleaners(context, registry)
        assertThat(registryMap).containsKey(m2repo)
        registryMap[m2repo]?.run()
        assertThat(m2repo).doesNotExist()
    }

    @Test
    fun testGradleProvider() {
        val provider = GradleCacheCleanerProvider(SystemTimeService())
        val gradleCache = File("${System.getProperty("user.home")}/.gradle/caches")
        provider.registerDirectoryCleaners(context, registry)
        assertThat(registryMap).containsKey(gradleCache)
        registryMap[gradleCache]?.run()
        assertThat(gradleCache).doesNotExist()
    }

    @Test
    fun testGradleWrapperProvider() {
        val provider = GradleCacheCleanerProvider(SystemTimeService())
        val wrapperCache = File("${System.getProperty("user.home")}/.gradle/wrapper/dists")
        provider.registerDirectoryCleaners(context, registry)
        assertThat(registryMap).containsKey(wrapperCache)
        registryMap[wrapperCache]?.run()
        assertThat(wrapperCache).doesNotExist()
    }

    @Test
    fun testGradleDaemonLogsRemoved() {
        val timeService = SystemTimeService()
        val provider = GradleCacheCleanerProvider(timeService)
        val daemonLogs = File("${System.getProperty("user.home")}/.gradle/daemon")
        File(daemonLogs, "2.5/ancient.out.log").setLastModified(timeService.now() - TimeUnit.DAYS.toMillis(42))

        provider.registerDirectoryCleaners(context, registry)

        assertThat(registryMap).containsKey(File(daemonLogs, "2.5/test.out.log"))
        assertThat(registryMap).containsKey(File(daemonLogs, "2.6/test.out.log"))
        assertThat(registryMap).doesNotContainKey(File(daemonLogs, "2.6/other.txt"))
        // registered for ancient.out.log
        assertThat(registryMap).containsKey(File(daemonLogs, "2.5"))

        registryMap.values.forEach { it.run() }

        assertThat(File(daemonLogs,"2.6/other.txt")).exists()
        assertThat(File(daemonLogs,"2.6")).exists()
        assertThat(File(daemonLogs,"2.5")).doesNotExist()
    }

    @Test
    fun testNPMProvider() {
        val provider = NPMCacheCleanerProvider()
        val repo =
                if (SystemInfo.isWindows) File(System.getenv("APPDATA"), "npm-cache")
                else File("${System.getProperty("user.home")}/.npm")

        val created = SystemInfo.isWindows && !repo.exists() && repo.mkdirs()

        provider.registerDirectoryCleaners(context, registry)
        assertThat(registryMap).containsKey(repo)

        // Do not clean on Windows as it's actual directory
        if (!created) return

        registryMap[repo]?.run()
        assertThat(repo).doesNotExist()
    }

    @Test
    fun testApacheIvyProvider() {
        val provider = ApacheIvyCacheCleanerProvider()
        val repo = File("${System.getProperty("user.home")}/.ivy2/cache")
        provider.registerDirectoryCleaners(context, registry)
        assertThat(registryMap).containsKey(repo)
        registryMap[repo]?.run()
        assertThat(repo).doesNotExist()
    }

    @Test
    fun testHeapDumpsAtHomeProvider() {
        val provider = HeapDumpsAtHomeCleanerProvider()
        val repo = File("${System.getProperty("user.home")}")
        provider.registerDirectoryCleaners(context, registry)
        assertThat(registryMap).containsKey(repo)
        registryMap[repo]?.run()
        assertThat(File(repo, "test.hprof")).doesNotExist()
        assertThat(repo).exists()
    }

    @Test
    fun testSccacheProvider() {
        Assume.assumeFalse(SystemInfo.isWindows) // we could clean the actual cache directory since it's under LOCALAPPDATA
        val provider = SccacheCacheCleanerProvider()
        val repo = SccacheCacheCleanerProvider.getCacheDirectory(System.getProperty("user.home"))?.let(::File)
        assertNotNull(repo)
        provider.registerDirectoryCleaners(context, registry)
        assertThat(registryMap).containsKey(repo)
        registryMap[repo]?.run()
        assertThat(repo).doesNotExist()
    }
}