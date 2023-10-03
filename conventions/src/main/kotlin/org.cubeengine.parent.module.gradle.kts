import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.XmlParser
import org.spongepowered.gradle.ore.task.PublishToOreTask
import java.io.ByteArrayOutputStream

plugins {
    `java-library`
    `maven-publish`
    signing
    id("org.cadixdev.licenser")
    id("com.github.johnrengelman.shadow")
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

val releasesRepoUrl = uri("https://maven.cubyte.org/repository/releases")
val snapshotsRepoUrl = uri("https://maven.cubyte.org/repository/snapshots")
val spongeReleasesRepoUrl = uri("https://repo.spongepowered.org/repository/maven-releases")
val spongeSnapshotsRepoUrl = uri("https://repo.spongepowered.org/repository/maven-snapshots")

// repos for modules **using** this convention
repositories {
    mavenCentral()
    maven(releasesRepoUrl)
    maven(snapshotsRepoUrl)
    maven(spongeReleasesRepoUrl)
    maven(spongeSnapshotsRepoUrl)
    mavenLocal()
}

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
    testImplementation("org.slf4j:slf4j-simple:2.0.3")
    testImplementation("org.spongepowered:spongeapi:$spongeVersion")

    // LibCube Plugin Dependency
    val libCubeVersion = project.properties["libCubeVersion"]
    if (libCubeVersion != null) {
        compileOnly("org.cubeengine:libcube:$libCubeVersion")
        testImplementation("org.cubeengine:libcube:$libCubeVersion")

        shadow("org.cubeengine:reflect-yaml")
        shadow("org.cubeengine:i18n")
        shadow("org.cubeengine:dirigent")
        shadow("org.ocpsoft.prettytime:prettytime")
    }

    constraints {
        listOf(configurations.shadow, configurations.implementation).forEach { config ->
            add(config.name, "org.cubeengine:reflect-yaml:3.0.1")
            add(config.name, "org.cubeengine:i18n:1.0.4")
            add(config.name, "org.cubeengine:dirigent:5.0.2")
            add(config.name, "org.ocpsoft.prettytime:prettytime:5.0.4.Final")
        }
    }
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

val projectJvmTarget = "17"
java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(projectJvmTarget))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

fun Project.isSnapshot() = version.toString().endsWith("-SNAPSHOT")

project.gradle.projectsEvaluated {
    publishing {
        repositories {
            maven {
                name = "cubyte"
                url = if (project.isSnapshot()) snapshotsRepoUrl else releasesRepoUrl
                credentials(PasswordCredentials::class)
            }
        }
    }

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
            publishArtifacts.from(tasks.shadowJar.map { it.outputs })
        }
    }
}


publishing {
    publications {
        publications.create<MavenPublication>("cubyte") {
            project.shadow.component(this)
            artifact(tasks.getByName("sourcesJar"))
            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://cubeengine.org")
                licenses {
                    license {
                        name.set("GNU General Public License Version 3")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("pschichtel")
                        name.set("Phillip Schichtel")
                        email.set("phillip@schich.tel")
                    }
                    developer {
                        id.set("faithcaio")
                        name.set("Anselm Brehme")
                    }
                    developer {
                        id.set("boeserwolf91")
                        name.set("Stefan Wolf")
                    }
                    developer {
                        id.set("totokaka")
                        name.set("Tobias Laundal")
                    }
                }
                scm {
                    url.set("https://github.com/CubeEngine/core")
                    connection.set("scm:git:https://github.com/CubeEngine/core")
                    developerConnection.set("scm:git:git@github.com:CubeEngine/core")
                }
            }
        }
    }
}

signing {
    if (project.findProperty("profile") == "release") {
        useGpgCmd()
        sign(publishing.publications)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.publish {
    dependsOn(tasks.check)

    val outputFile = project.layout.buildDirectory.file("jar-url.txt").get().asFile

    outputs.file(outputFile)

    doLast {
        val repoUrl = if (project.isSnapshot()) snapshotsRepoUrl else releasesRepoUrl

        val versionUrl = "$repoUrl/${project.group.toString().replace('.', '/')}/${project.name}/${project.version}"
        val parsed = XmlParser().parse("$versionUrl/maven-metadata.xml")

        fun Node.children(name: String): NodeList = get(name) as NodeList
        fun Node.children(name: String, n: Int): Node = (get(name) as NodeList)[n] as Node
        fun Node.child(name: String): Node = children(name).first() as Node
        fun Node.firstStringValue() = (value() as Iterable<*>).iterator().next() as String

        val lastSnapshot = parsed.child("versioning").children("snapshotVersions", 0).child("snapshotVersion").child("value").firstStringValue()
        val jarUrl = "$versionUrl/${project.name}-${lastSnapshot}.jar"
        println("Project ${project.name}: ${project.version} \t$jarUrl")
        outputFile.writeText(jarUrl)
    }
}

tasks.withType<PublishToOreTask>().configureEach {
    dependsOn(tasks.build)
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