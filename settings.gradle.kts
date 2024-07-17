rootProject.name = "cubeengine"

pluginManagement {
    includeBuild("conventions")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.cubeengine.org")
        maven("https://repo.spongepowered.org/repository/maven-public")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

include(
        "core",
        "bluemapplus",
        "docs",
        "kickban",
        "locker",
        "multiverse",
        "netherportals",
        "portals",
        "protector",
        "roles",
        "rolecontrol",
        "teleport",
        "travel",
        "vanillaplus",
        "worldcontrol",
        "worlds",
        "zoned",
        "bigdata",
        "chat",
        "chopchop",
        "discord",
        "elevator",
        "fly",
        "headvillager",
        "itemduct",
        "kits",
        "mechanism",
        "observe",
        "powertools",
        "spawn",
        "spawner",
        "squelch",
        "tablist",
        "terra",
        "traders",
        "vigil",
        "writer",
        "launch",
)
