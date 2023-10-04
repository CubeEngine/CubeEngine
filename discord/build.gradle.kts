plugins {
    id("org.cubeengine.parent.module")
}

dependencies {
    implementation("com.discord4j:discord4j-core:3.2.6") {
        exclude(group = "io.netty", module = "netty-transport-native-epoll")
        exclude(group = "io.netty", module = "netty-resolver-dns-native-macos")
    }
}