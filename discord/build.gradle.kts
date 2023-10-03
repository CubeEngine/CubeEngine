import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.cubeengine.parent.module")
}

tasks.withType<ShadowJar>().configureEach {
    relocate("io.netty", "org.cubeengine.module.discord.reloacted.netty")
}

dependencies {
    implementation("com.discord4j:discord4j-core:3.2.6")
}