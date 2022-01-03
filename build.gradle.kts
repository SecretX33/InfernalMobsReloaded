import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.github.secretx33"
version = "1.2.5"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://plugins.gradle.org/m2/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.mattstudios.me/artifactory/public")
    maven("https://repo.dustplanet.de/artifactory/libs-release-local")
    maven("https://jitpack.io")
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    // Unit Testing
    testImplementation(kotlin("test-junit5"))
    testImplementation(kotlin("reflect"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    testImplementation("org.mockito:mockito-inline:3.12.4")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.18:1.15.0")
    testImplementation("net.kyori:adventure-api:4.9.3")
    // DI
    val koin_version = "3.1.+"
    implementation("io.insert-koin:koin-core:$koin_version")
    // API dependency
    compileOnly("io.papermc.paper:paper-api:1.18.1-R0.1-SNAPSHOT")
    compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")
    // Bukkit specific dependencies
    implementation("com.github.cryptomorin:XSeries:8.5.0.1")
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
    archiveFileName.set("${rootProject.name}.jar")
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

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
        jvmTarget = "17"
    }
}

tasks.processResources {
    outputs.upToDateWhen { false }
    val main_class = "${project.group}.${project.name.toLowerCase()}.${project.name}"
    expand("name" to project.name, "version" to project.version, "mainClass" to main_class)
}
