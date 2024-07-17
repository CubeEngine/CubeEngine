import com.google.gson.Gson

plugins {
    id("org.cubeengine.parent.root")
    application
}

val spongeVersion: String by project.properties

dependencies {
    implementation("org.spongepowered:spongevanilla:1.21-12.0.0-RC1710:universal")
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

    data class Op(val uuid: String, val name: String, val level: Int = 4, val bypassesPlayerLimit: Boolean = true)

    doFirst {
        workingDir.mkdirs()
        workingDir.resolve("eula.txt").writeText("eula=true")
        val ops = listOf(
            Op(uuid = "fcd2958f-44f1-4a77-a788-d99e88eeeaaf", name = "NinjaLaterne"),
            Op(uuid = "5d33570d-7901-474c-9005-3eeed10b7a55", name = "Faithcaio"),
        )

        workingDir.resolve("ops.json").writeText(Gson().toJson(ops))
    }
}
