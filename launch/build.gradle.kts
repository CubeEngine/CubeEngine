plugins {
    java
    application
}

// repos for modules **using** this convention
repositories {
    mavenCentral()
    maven(uri("https://repo.spongepowered.org/repository/maven-releases"))
}

// TODO why is this needed now?
tasks.named("startScripts") {
    dependsOn(project(":core").tasks.named("shadowJar"))
    dependsOn(project(":discord").tasks.named("shadowJar"))
}


dependencies {
    implementation("org.spongepowered:spongevanilla:1.20.6-11.0.0-RC1613:universal") {
        exclude("org.spongepowered:spongeapi")
    }
    // TODO not entirely sure why this is needed
    implementation("org.yaml:snakeyaml:1.33")
    implementation(project(":core"))
    implementation(project(":vanillaplus"))
    implementation(project(":discord"))
}

tasks.withType<JavaExec>().configureEach {
    mainClass.set("org.spongepowered.vanilla.installer.VersionCheckingMain")
    workingDir = file(projectDir.resolve("server"))
    standardInput = System.`in`

    project.configurations.runtimeClasspath {
        val spongeJar = resolvedConfiguration.getFiles { m -> m.group == "org.spongepowered" && m.name == "spongevanilla" }.firstOrNull()
        if (spongeJar != null) {
            jvmArgs = listOf("-javaagent:$spongeJar")
        } else {
            logger.error("Sponge jar not found!")
        }
    }

    doFirst {
        workingDir.mkdirs()
        workingDir.resolve("eula.txt").writeText("eula=true")
        val ops = listOf("NinjaLaterne", "Faithcaio")
        workingDir.resolve("ops.json").delete()
        workingDir.resolve("ops.txt").writeText(ops.joinToString("\n"))
    }
}
