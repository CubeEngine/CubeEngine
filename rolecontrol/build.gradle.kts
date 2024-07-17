import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.configurationcache.extensions.capitalized

plugins {
    id("org.cubeengine.parent.module")
    id("org.cubeengine.parent.shadowing")
}

val extraJars by configurations.registering {
    isCanBeConsumed = true
}

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

val nativeLibs by configurations.registering

val archs = listOf("x86_64", "aarch64")

dependencies {
    compileOnly(project(":roles"))
    implementation("tel.schich:libdatachannel-java:1.0-SNAPSHOT") {
        exclude("org.slf4j")
    }
    for (arch in archs) {
        nativeLibs(group = "tel.schich", name = "libdatachannel-java", version = "1.0-SNAPSHOT", classifier = arch)
    }
}

for (arch in archs) {
    val taskSuffix = arch.split("[_-]".toRegex()).joinToString(separator = "") { it.capitalized() }
    val shadow = tasks.register<ShadowJar>("shadowWithNativeFor$taskSuffix") {
        from(tasks.shadowJar)
        from(nativeLibs.get().resolvedConfiguration.resolvedArtifacts.find { it.classifier == arch }?.file)
        archiveClassifier = arch
    }

    artifacts.add(extraJars.name, shadow)

    tasks.build {
        dependsOn(shadow)
    }
}
