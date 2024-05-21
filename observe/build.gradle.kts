plugins {
    id("org.cubeengine.parent.module")
}

val prometheusVersion: String by project.properties;

dependencies {

    implementation("org.spongepowered:observer:1.0-SNAPSHOT")
    // Exposition HTTPServer
    implementation("io.netty:netty-codec-http:4.1.93.Final")
    // Monitoring
    implementation("io.prometheus:simpleclient:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
}

// TODO why is this needed now?
tasks.named("test") {
    dependsOn(project(":core").tasks.named("shadowJar"))
}