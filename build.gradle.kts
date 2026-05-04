import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("fabric-loom") version "1.14-SNAPSHOT"
    `maven-publish`
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
}

val minecraft_version: String by project
val loader_version: String by project
val fabric_kotlin_version: String by project
val mod_version: String by project
val maven_group: String by project
val archives_base_name: String by project
val mod_name: String by project
val fabric_version: String by project
val modmenu_version: String by project
val iris_version: String by project
val ktor_version: String by project

group = maven_group
version = mod_version
base.archivesName.set(archives_base_name)

loom { accessWidenerPath.set(file("src/main/resources/soterm.accesswidener")) }

val bundled by configurations.creating

configurations {
    implementation.get().extendsFrom(bundled)
}


repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://maven.parchmentmc.org")
    maven("https://maven.terraformersmc.com/releases")
    maven("https://api.modrinth.com/maven")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.21.10:2025.10.12@zip")
    })

    modImplementation("net.fabricmc:fabric-loader:$loader_version")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")
    modImplementation("net.fabricmc:fabric-language-kotlin:$fabric_kotlin_version")
    modImplementation("com.terraformersmc:modmenu:$modmenu_version")
    modImplementation("maven.modrinth:iris:$iris_version")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    include("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("io.github.llamalad7:mixinextras-fabric:0.4.1")
    annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.4.1")

    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    include("org.java-websocket:Java-WebSocket:1.5.4")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("io.github.classgraph:classgraph:4.8.174")
    include("io.github.classgraph:classgraph:4.8.174")

    implementation("io.ktor:ktor-client-okhttp-jvm:$ktor_version")
    include("io.ktor:ktor-client-okhttp-jvm:$ktor_version")

    implementation("io.ktor:ktor-client-websockets-jvm:$ktor_version")
    include("io.ktor:ktor-client-websockets-jvm:$ktor_version")

    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktor_version")
    include("io.ktor:ktor-client-content-negotiation-jvm:$ktor_version")

    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    include("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")

    bundled("io.github.classgraph:classgraph:4.8.174")
    bundled("io.ktor:ktor-client-okhttp-jvm:$ktor_version")
    bundled("io.ktor:ktor-client-websockets-jvm:$ktor_version")
    bundled("io.ktor:ktor-client-content-negotiation-jvm:$ktor_version")
    bundled("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

kotlin {
    jvmToolchain(21)
}

val intermediateJarsDir = layout.buildDirectory.dir("tmp/intermediateJars")

tasks.jar {
    destinationDirectory.set(intermediateJarsDir)
    from("LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}

tasks.remapJar {
    archiveFileName.set("$mod_name - $version.jar")
    dependsOn(tasks.jar)
    inputFile.set(tasks.jar.flatMap { it.archiveFile })
}

artifacts {
    add("archives", tasks.remapJar)
}

tasks.build {
    dependsOn(tasks.remapJar)
}

tasks.test {
    failOnNoDiscoveredTests = false
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = archives_base_name
            from(components["java"])
        }
    }
}