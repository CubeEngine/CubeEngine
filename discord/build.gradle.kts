plugins {
    id("org.cubeengine.parent.module")
    id("org.cubeengine.parent.shadowing")
}

dependencies {
    implementation("com.discord4j:discord4j-core:3.3.0-RC1") {
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
        exclude(group = "io.netty", module = "netty-codec")
        exclude(group = "io.netty", module = "netty-transport")
        exclude(group = "io.netty", module = "netty-common")
        exclude(group = "io.netty", module = "netty-buffer")
        exclude(group = "io.netty", module = "netty-handler")
        exclude(group = "io.netty", module = "netty-transport-native-epoll")
        exclude(group = "io.netty", module = "netty-transport-native-unix-common")
        exclude(group = "io.netty", module = "netty-resolver-dns")
        exclude(group = "org.checkerframework", module = "checker-qual")
    }

    tasks.shadowJar {
        relocate("com.github.benmanes.caffeine", "org.cubeengine.relocated.com.github.benmanes.caffeine")
        relocate("io.netty.handler.codec", "org.cubeengine.relocated.io.netty.handler.codec")
    }
}
