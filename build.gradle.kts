import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.1.1") {
            exclude("com.android.tools.build")
        }
    }
}

plugins {
    kotlin("jvm") version "1.5.31"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "com.github.secretx33"
version = "1.2.2"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://plugins.gradle.org/m2/")
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.mattstudios.me/artifactory/public")
    maven("https://repo.dustplanet.de/artifactory/libs-release-local")
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.+")
    // Unit Testing
    testImplementation(kotlin("test-junit5"))
    testImplementation(kotlin("reflect"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    testImplementation("org.mockito:mockito-inline:3.12.4")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.17:1.7.0")
//    testImplementation("net.kyori:adventure-platform-bukkit:4.0.0-SNAPSHOT")
//    testImplementation(fileTree("libs"))
    testImplementation("net.kyori:adventure-api:4.8.1")
    // DI
    val koin_version = "3.1.+"
    implementation("io.insert-koin:koin-core:$koin_version")
    // API dependency
    compileOnly("io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT") // Paper API dependency
    compileOnly(fileTree("libs"))     // Paper server dependency
    // Bukkit specific dependencies
    implementation("com.github.cryptomorin:XSeries:8.4.0")
    implementation("me.mattstudios:triumph-msg-adventure:2.2.4-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.7.0")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.5")
    compileOnly("com.github.TownyAdvanced:Towny:0.97.1.0")
    compileOnly("de.dustplanet:silkspawners:7.1.0") {
        exclude(group = "*")
    }
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

val javaVersion = JavaVersion.VERSION_16.toString()

tasks.withType<JavaCompile> {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
        jvmTarget = javaVersion
    }
}

tasks.processResources {
    val main_class = "${project.group}.${project.name.toLowerCase()}.${project.name}"
    expand("name" to project.name, "version" to project.version, "mainClass" to main_class)
}
