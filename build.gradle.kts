import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("net.fabricmc.fabric-loom")
    `maven-publish`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val minecraft_version: String by project
val loader_version: String by project
val fabric_kotlin_version: String by project
val mod_version: String by project
val maven_group: String by project
val mod_name: String by project
val fabric_version: String by project
val modmenu_version: String by project
val iris_version: String by project
val ktor_version: String by project

version = mod_version
group = maven_group
base { archivesName.set(mod_name) }

loom { accessWidenerPath.set(file("src/main/resources/soterm.accesswidener")) }

val bundled by configurations.creating

configurations {
    implementation.get().extendsFrom(bundled)
}

repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://maven.terraformersmc.com/releases")
    maven("https://api.modrinth.com/maven")
    maven("https://jitpack.io")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")

    implementation("net.fabricmc:fabric-loader:$loader_version")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")
    implementation("net.fabricmc:fabric-language-kotlin:$fabric_kotlin_version")

    runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
    compileOnly("maven.modrinth:iris:$iris_version")
    compileOnly("com.terraformersmc:modmenu:$modmenu_version")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    include("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("io.github.llamalad7:mixinextras-fabric:0.4.1")
    annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.4.1")

    bundled("io.github.classgraph:classgraph:4.8.174")
    bundled("io.ktor:ktor-client-cio:$ktor_version")
    bundled("io.ktor:ktor-client-websockets-jvm:$ktor_version")
    bundled("io.ktor:ktor-client-content-negotiation-jvm:$ktor_version")
    bundled("io.ktor:ktor-client-encoding:$ktor_version")
    bundled("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
}

afterEvaluate {
    bundled.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
        artifact.moduleVersion.id.let { id ->
            dependencies.add("include", "${id.group}:${id.name}:${id.version}")
        }
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile> {
    options.release.set(25)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
}

kotlin {
    jvmToolchain(25)
}

tasks.named<Jar>("jar") {
    archiveFileName.set("$mod_name-$mod_version - $minecraft_version.jar")

    from("LICENSE") {
        rename { "${it}_${mod_name}" }
    }
}

tasks.test {
    failOnNoDiscoveredTests = false
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = mod_name
            from(components["java"])
        }
    }
}
