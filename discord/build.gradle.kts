plugins {
    id("org.cubeengine.parent.module")
    id("org.cubeengine.parent.shadowing")
}

dependencies {
    implementation("net.dv8tion:JDA:5.3.0") {
        exclude(module="opus-java")
    }

    tasks.shadowJar {
        relocate("com.github.benmanes.caffeine", "org.cubeengine.relocated.com.github.benmanes.caffeine")
        relocate("io.netty.handler.codec", "org.cubeengine.relocated.io.netty.handler.codec")
    }
}
