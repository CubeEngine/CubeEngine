plugins {
    id("org.cubeengine.parent.module")
}

repositories {
    maven ( "https://repo.bluecolored.de/releases" )
}

dependencies {
    compileOnly ("de.bluecolored.bluemap:BlueMapAPI:2.7.2")
}