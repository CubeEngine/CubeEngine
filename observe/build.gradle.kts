plugins {
    id("org.cubeengine.parent.module")
    id("org.cubeengine.parent.shadowing")
}

val prometheusVersion: String by project.properties;

dependencies {

    implementation("org.spongepowered:observer:1.0-SNAPSHOT")
    // Exposition HTTPServer
    implementation("io.netty:netty-codec-http:4.1.97.Final") {
        exclude(group = "io.netty", module = "netty-codec")
        exclude(group = "io.netty", module = "netty-transport")
        exclude(group = "io.netty", module = "netty-common")
        exclude(group = "io.netty", module = "netty-buffer")
        exclude(group = "io.netty", module = "netty-handler")
    }
    compileOnly("io.netty:netty-codec:4.1.97.Final")
    compileOnly("io.netty:netty-buffer:4.1.97.Final")
    compileOnly("io.netty:netty-transport:4.1.97.Final")
    compileOnly("io.netty:netty-common:4.1.97.Final")
    compileOnly("io.netty:netty-handler:4.1.97.Final")
    testImplementation("io.netty:netty-codec:4.1.97.Final")
    // Monitoring
    implementation("io.prometheus:simpleclient:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
}
