plugins {
    id("org.cubeengine.parent.core")
    id("org.cubeengine.parent.shadowing")
}

dependencies {
    // Configurations
    api("org.cubeengine:reflect-yaml:3.0.1") {
        // we use the version pulled by minecraft
        exclude("org.yaml", "snakeyaml")
    }
    // Translations
    api("org.cubeengine:i18n:1.0.4")
    // Message formatting
    api("org.cubeengine:dirigent:5.0.2")
    // Other stuff
    api("org.ocpsoft.prettytime:prettytime:5.0.4.Final")
}
