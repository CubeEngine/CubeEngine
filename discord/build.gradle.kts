plugins {
    id("org.cubeengine.parent.module")
}

dependencies {
    implementation("com.discord4j:discord4j-core:3.2.6")
// TODO shadow + relocate io.netty -> org.cubeengine.module.discord.reloacted.netty
}