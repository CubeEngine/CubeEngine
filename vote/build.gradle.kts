plugins {
    id("org.cubeengine.parent.module")
}

val nuvotifierVersion: String by project.properties

repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.vexsoftware:nuvotifier-sponge8:${nuvotifierVersion}")
    compileOnly("com.vexsoftware:nuvotifier-common:${nuvotifierVersion}")
    compileOnly("com.vexsoftware:nuvotifier-api:${nuvotifierVersion}")
}
