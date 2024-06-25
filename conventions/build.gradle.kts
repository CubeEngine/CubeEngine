plugins {
    `kotlin-dsl`
}

// repos **used by** this convention
repositories {
    mavenCentral()
    gradlePluginPortal()
}

// pull plugins as implementation dependencies here:
dependencies {
//    api(plugin("org.jetbrains.kotlin.jvm", "1.7.20"))
    api(plugin("io.github.gradle-nexus.publish-plugin", "1.3.0"))
    api(plugin("org.spongepowered.gradle.plugin", "2.2.0"))
    api(plugin("org.cadixdev.licenser", "0.6.1"))
    api(plugin("com.github.johnrengelman.shadow", "8.1.1"))
    api(plugin("org.spongepowered.gradle.ore", "2.2.0"))
    api(plugin("org.spongepowered.gradle.repository", "2.2.0"))
}

fun plugin(id: String, version: String) = "$id:$id.gradle.plugin:$version"
