plugins {
    id("org.cubeengine.parent.core")
}

dependencies {
    compileOnly(project(":core"))
    testImplementation(project(":core"))
}
