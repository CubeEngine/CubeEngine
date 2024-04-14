plugins {
    id("org.cubeengine.parent.core")
}

dependencies {
    // Configurations
    implementation("org.cubeengine:reflect-yaml") {
        // we use the version pulled by minecraft
        exclude("org.yaml", "snakeyaml")
    }
    // Translations
    implementation("org.cubeengine:i18n")
    // Message formatting
    implementation("org.cubeengine:dirigent")
    // Other stuff
    implementation("org.ocpsoft.prettytime:prettytime")
}
