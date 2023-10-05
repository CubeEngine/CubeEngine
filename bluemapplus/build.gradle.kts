plugins {
    id("org.cubeengine.parent.module")
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.BlueMap-Minecraft:BlueMapAPI:v2.6.1")
}