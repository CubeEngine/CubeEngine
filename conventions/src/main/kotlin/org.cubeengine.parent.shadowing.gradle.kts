import org.spongepowered.gradle.ore.task.PublishToOreTask

plugins {
    `java-library` apply false
    id("org.spongepowered.gradle.ore") apply false
    id("com.github.johnrengelman.shadow")
}


tasks.jar {
    archiveClassifier.set("light")
}

tasks.shadowJar {
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<PublishToOreTask>().configureEach {
    dependsOn.remove(tasks.jar)
    dependsOn(tasks.shadowJar)
}

project.gradle.projectsEvaluated {
    oreDeployment {
        // must exist
        defaultPublication {
            publishArtifacts.setFrom(tasks.shadowJar.map { it.outputs })
        }
    }
}