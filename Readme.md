[![team project](http://jb.gg/badges/team.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

TeamCity Caches Cleanup Plugin
=========================

# About
A simple plugin, that helps to get free space for builds by deleting `~/.m2/repository` or `~/.gradle/caches` folders.
These locations are known to grow very large over time, especially on long-living build agent.

Also it provides a simple cache with following properties:
* it can be used by build steps via reference to `system.agent.persistent.cache` TeamCity parameter
* it survives agent restarts
* it will be cleaned if free space is required. Top level folders will be deleted one by one based on last update time, until enough space is free.

## Warning!
:zap: This plugin will delete files from build agent!


# Build
* CI status: <a href="https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityPluginsByJetBrains_TeamcityCleanupPlugin_Build&guest=1">
  <img src="https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:TeamCityPluginsByJetBrains_TeamcityCleanupPlugin_Build/statusIcon.svg"/>
</a>
* To build locally, checkout and run `mvn package`

# Install
Just drop zip `target\caches-cleanup-plugin.zip` into `<TeamCity_data_dir>/plugins`
