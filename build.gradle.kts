import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.0.0")
    }
}

plugins {
    kotlin("jvm") version "1.5.0"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.github.secretx33"
version = "1.0.3.3"

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    maven { url = uri("https://repo.codemc.org/repository/maven-public/") }
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
    maven { url = uri("https://plugins.gradle.org/m2/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://maven.enginehub.org/repo/") }
    maven { url = uri("https://repo.mattstudios.me/artifactory/public") }
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT") // Paper API dependency
    compileOnly(fileTree("libs"))      // Paper server dependency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    val koin_version = "3.0.1"
    implementation("io.insert-koin:koin-core:$koin_version")
    testCompileOnly("io.insert-koin:koin-test:$koin_version")
    implementation("com.github.cryptomorin:XSeries:7.9.1.1")
    implementation("me.mattstudios:triumph-msg-adventure:2.2.4-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.6.0")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.4")
    compileOnly("com.github.TownyAdvanced:Towny:0.97.0.0")
}

tasks.test {
    useJUnitPlatform()
}

// Disables the normal jar task
tasks.jar { enabled = false }

// And enables shadowJar task
artifacts.archives(tasks.shadowJar)

tasks.shadowJar {
    archiveFileName.set(rootProject.name + ".jar")
    val dependencyPackage = "${rootProject.group}.dependencies.${rootProject.name.toLowerCase()}"
    relocate("com.zaxxer.hikari", "${dependencyPackage}.hikari")
    relocate("okio", ".${dependencyPackage}.moshi.okio")
    relocate("org.koin", "${dependencyPackage}.koin")
    relocate("org.slf4j", "${dependencyPackage}.slf4j")
    relocate("kotlin", "${dependencyPackage}.kotlin")
    relocate("kotlinx", "${dependencyPackage}.kotlinx")
    relocate("org.jetbrains", "${dependencyPackage}.jetbrains")
    relocate("org.intellij", "${dependencyPackage}.jetbrains.intellij")
    relocate("com.cryptomorin.xseries", "${dependencyPackage}.xseries")
    relocate("me.mattstudios.msg", "${dependencyPackage}.mfmsg")
    exclude("ScopeJVMKt.class")
    exclude("DebugProbesKt.bin")
    exclude("META-INF/**")
}

tasks.register<proguard.gradle.ProGuardTask>("proguard") {
    configuration("proguard-rules.pro")
}

//tasks.build.get().finalizedBy(tasks.getByName("proguard"))

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }

tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }

tasks.processResources {
    val main_class = "${project.group}.${project.name.toLowerCase()}.${project.name}"
    expand("name" to project.name, "version" to project.version, "mainClass" to main_class)
}
