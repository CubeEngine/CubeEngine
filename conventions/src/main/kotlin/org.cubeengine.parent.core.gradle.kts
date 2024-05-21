import org.spongepowered.gradle.ore.task.PublishToOreTask
import java.io.ByteArrayOutputStream

plugins {
    id("org.cubeengine.parent.root")
    `java-library`
    publishing
    signing
    id("org.cadixdev.licenser")
    id("org.spongepowered.gradle.ore")
}

val pluginGroupId: String by project.properties
val pluginDescription: String by project.properties
val pluginVersion: String by project.properties
val spongeVersion: String by project.properties
val pluginIsSnapshot: String by project.properties
val moduleId: String by project.properties
val moduleName: String by project.properties

val spongeMajorVersion: String = spongeVersion.substring(0, spongeVersion.indexOf('.'))
val snapshotVersion = if (pluginIsSnapshot.toBoolean()) "-SNAPSHOT" else ""

group = pluginGroupId
version = "$spongeMajorVersion.$pluginVersion$snapshotVersion"
description = pluginDescription

dependencies {
    // sponge
    compileOnly("org.spongepowered:spongeapi:$spongeVersion")

    val pluginGenVersion = "1.0.8"
    compileOnly("org.cubeengine:plugin-gen:$pluginGenVersion")
    annotationProcessor("org.cubeengine:plugin-gen:$pluginGenVersion")

    // Testing
    val junitVersion = "5.9.1"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.slf4j:slf4j-simple:2.0.3")
    testImplementation("org.spongepowered:spongeapi:$spongeVersion")
}

tasks.test {
    useJUnitPlatform()
}

fun getGitCommit(): String? {
    return try {
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine = "git rev-parse HEAD".split(" ")
            standardOutput = byteOut
        }
        byteOut.toString("UTF-8").trim()
    } catch (e: Exception) {
        // ignore
        null
    }
}

val orgName = "CubeEngine"
val orgUrl = "https://cubeengine.org"

fun annotationProcessorArg(name: String, value: Any?) = value?.let { "-A$name=$it" }
fun pluginGenArg(name: String, value: Any?) = annotationProcessorArg("cubeengine.module.$name", value)

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(
        listOfNotNull(
            "-parameters",
            pluginGenArg("version", project.version),
            pluginGenArg("sourceversion", getGitCommit()),
            pluginGenArg("description", project.description),
            pluginGenArg("id", moduleId),
            pluginGenArg("name", moduleName),
            pluginGenArg("team", orgName),
            pluginGenArg("url", orgUrl),
            pluginGenArg("libcube.version", project.properties["libCubeVersion"]),
            pluginGenArg("sponge.version", spongeVersion),
        )
    )
}

tasks.withType<Test> {
    useJUnitPlatform()
}

fun Project.isSnapshot() = version.toString().endsWith("-SNAPSHOT")

project.gradle.projectsEvaluated {
    oreDeployment {
        val moduleId: String by project.properties
        val oreStaging = project.properties["oreStaging"]?.toString()?.toBoolean() ?: false
        val oreCreateForumPost =  project.properties["oreCreateForumPost"]?.toString()?.toBoolean() ?: false

        if (oreStaging) {
            oreEndpoint("https://staging-ore.spongeproject.net")
        } else {
            oreEndpoint("https://ore.spongepowered.org")
        }

        // must exist
        defaultPublication {
            projectId.set("cubeengine-$moduleId")
            createForumPost.set(oreCreateForumPost)
            versionBody.set(project.description) // TODO actually provide a changelog
            channel.set(if (project.isSnapshot()) "Dev" else "Release")
            publishArtifacts.setFrom(tasks.jar.map { it.outputs })
        }
    }
}

signing {
    if (project.findProperty("cubeengine-profile") == "release") {
        useGpgCmd()
        sign(publishing.publications["github"])
    }
}

tasks.withType<PublishToOreTask>().configureEach {
    dependsOn(tasks.jar)
}

tasks.classes.configure {
    dependsOn(tasks.licenseFormat)
}

license {
    header(file("../header.txt"))
    newLine(false)
    exclude("**/*.info")
    exclude("assets/**")
    exclude("*.kts")
    exclude("**/*.json")
    exclude("**/*.properties")
    exclude("*.txt")
}
