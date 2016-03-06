TeamCity Agent Cleanup Plugin
=========================

# About

A simple plugin, that helps to get free space for builds by deleting `~/.m2/repository` or `~/.gradle/caches` folders.
These locations are known to grow very large over time, especially on long-living build agent.

# Build
`mvn package`

# Install
Just drop zip `target\teamcity-cleanup-plugin.zip` into `<TeamCity_data_dir>/plugins`
