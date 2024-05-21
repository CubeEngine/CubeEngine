plugins {
    id("org.cubeengine.parent.root")
    application
}

val spongeVersion: String by project.properties

dependencies {
    implementation("org.spongepowered:spongevanilla:1.20.6-11.0.0-RC1613:universal")
    implementation("org.spongepowered:spongeapi:$spongeVersion")
    implementation(project(":core"))
    implementation(project(":vanillaplus"))
    implementation(project(":discord"))
}

tasks.withType<JavaExec>().configureEach {
    mainClass.set("org.spongepowered.vanilla.installer.InstallerMain")
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
