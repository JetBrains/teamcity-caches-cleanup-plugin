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

package github.nskvortsov

import github.nskvortsov.teamcity.cleanup.ApacheIvyCacheCleanerProvider
import github.nskvortsov.teamcity.cleanup.GradleCacheCleanerProvider
import github.nskvortsov.teamcity.cleanup.MavenCacheCleanerProvider
import github.nskvortsov.teamcity.cleanup.NPMCacheCleanerProvider
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.DirectoryCleanersProviderContext
import jetbrains.buildServer.agent.DirectoryCleanersRegistry
import jetbrains.buildServer.util.FileUtil
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.*
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File
import java.util.*
import kotlin.properties.Delegates

class SimpleCleanerProvidersTest {

    var registryMap: MutableMap<File, Runnable> by Delegates.notNull()
    var context: DirectoryCleanersProviderContext by Delegates.notNull()
    var registry: DirectoryCleanersRegistry by Delegates.notNull()
    var runningBuild: AgentRunningBuild by Delegates.notNull()

    var tempDir: File by Delegates.notNull()
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
        System.setProperty("user.home", oldHome)
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
        val provider = GradleCacheCleanerProvider()
        val gradleCache = File("${System.getProperty("user.home")}/.gradle/caches")
        provider.registerDirectoryCleaners(context, registry)
        assertThat(registryMap).containsKey(gradleCache)
        registryMap[gradleCache]?.run()
        assertThat(gradleCache).doesNotExist()
    }

    @Test
    fun testGradleWrapperProvider() {
        val provider = GradleCacheCleanerProvider()
        val wrapperCache = File("${System.getProperty("user.home")}/.gradle/wrapper/dists")
        provider.registerDirectoryCleaners(context, registry)
        assertThat(registryMap).containsKey(wrapperCache)
        registryMap[wrapperCache]?.run()
        assertThat(wrapperCache).doesNotExist()
    }

    @Test
    fun testGradleDaemonLogsRemoved() {
        val provider = GradleCacheCleanerProvider()
        val daemonLogs = File("${System.getProperty("user.home")}/.gradle/daemon")
        provider.registerDirectoryCleaners(context, registry)
        assertThat(registryMap).containsKey(File(daemonLogs, "2.5/test.out.log"))
        assertThat(registryMap).containsKey(File(daemonLogs, "2.6/test.out.log"))
        assertThat(registryMap).doesNotContainKey(File(daemonLogs, "2.6/other.txt"))
    }

    @Test
    fun testNPMProvider() {
        val provider = NPMCacheCleanerProvider()
        val repo = File("${System.getProperty("user.home")}/.npm")
        provider.registerDirectoryCleaners(context, registry)
        assertThat(registryMap).containsKey(repo)
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
}