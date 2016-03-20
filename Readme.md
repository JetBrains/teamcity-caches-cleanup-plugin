TeamCity Caches Cleanup Plugin
=========================

# About
A simple plugin, that helps to get free space for builds by deleting `~/.m2/repository` or `~/.gradle/caches` folders.
These locations are known to grow very large over time, especially on long-living build agent.

## Warning!
:zap: This plugin will delete files from build agent!


# Build
* CI status: <a href="https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityPluginsByJetBrains_TeamcityCleanupPlugin_Build&guest=1">
  <img src="https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:TeamCityPluginsByJetBrains_TeamcityCleanupPlugin_Build/statusIcon.svg"/>
</a>
* To build locally, checkout and run `mvn package`

# Install
Just drop zip `target\caches-cleanup-plugin.zip` into `<TeamCity_data_dir>/plugins`
