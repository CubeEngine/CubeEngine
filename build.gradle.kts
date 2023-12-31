group = "org.cubeengine"
version = "1.0.0-SNAPSHOT"

description = "CubeEngine Project"

val dumpUrls by tasks.registering {
    group = "documentation"

    for (subproject in project.subprojects) {
        mustRunAfter(subproject.tasks)
    }

    val nameWidth = project.subprojects.maxOfOrNull { it.name.length } ?: 0
    val versionWidth = project.subprojects.maxOfOrNull { it.version.toString().length } ?: 0

    doLast {
        for (subproject in project.subprojects) {
            if (subproject.tasks.findByName("publish")?.didWork == true) {
                subproject.layout.buildDirectory.file("jar-url.txt").orNull?.asFile?.let {
                    println("${subproject.name.padStart(nameWidth)}: ${subproject.version.toString().padStart(versionWidth)} ${it.readText()}")
                }
            }
        }
    }
}
