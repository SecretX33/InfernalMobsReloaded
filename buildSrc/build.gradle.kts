repositories {
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    `kotlin-dsl` apply true
}

dependencies {
    api("com.github.johnrengelman.shadow:com.github.johnrengelman.shadow.gradle.plugin:7.0.0")
    api("org.ow2.asm:asm:9.2")
    api("org.ow2.asm:asm-util:9.2")
}
