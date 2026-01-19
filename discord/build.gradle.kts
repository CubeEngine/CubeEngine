plugins {
    id("org.cubeengine.parent.module")
    id("org.cubeengine.parent.shadowing")
}

dependencies {
    implementation("net.dv8tion:JDA:6.3.0") {
        exclude(module="opus-java")
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind")
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude("org.slf4j")
        exclude("com.google.code.gson")
    }

    tasks.shadowJar {
        relocate("com.github.benmanes.caffeine", "org.cubeengine.relocated.com.github.benmanes.caffeine")
        relocate("io.netty.handler.codec", "org.cubeengine.relocated.io.netty.handler.codec")
    }
}
