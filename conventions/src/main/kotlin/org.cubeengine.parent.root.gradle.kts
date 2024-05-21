import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec

plugins {
    java
    id("org.spongepowered.gradle.repository")
}

val projectJvmTarget = "21"
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(projectJvmTarget))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

// repos for modules **using** this convention
repositories {
    mavenCentral()
    sponge.releases()
    sponge.snapshots()
    mavenLocal()
}
