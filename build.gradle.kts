import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `maven-publish`
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    id("dev.architectury.loom") version "0.12.0.+"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

loom {
    accessWidenerPath.set(project.file("src/main/resources/notenoughupdates.accesswidener"))
    runConfigs {
        removeIf { it.name != "client" }
    }
    launches {
        named("client") {
            property("devauth.enabled", "true")
            property("fabric.log.level", "info")
            property("notenoughupdates.debug", "true")
        }
    }
}

repositories {
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://maven.shedaniel.me")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://api.modrinth.com/maven") {
        content {
            includeGroup("maven.modrinth")
        }
    }
    mavenLocal()
}

val shadowMe by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

val transInclude by configurations.creating {
    exclude(group = "com.mojang")
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains.kotlinx")
    isTransitive = true
}

dependencies {
    // Minecraft dependencies
    "minecraft"("com.mojang:minecraft:${project.property("minecraft_version")}")
    "mappings"(loom.officialMojangMappings())

    // Fabric dependencies
    modImplementation("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("fabric_kotlin_version")}")

    // Actual dependencies
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-api:${rootProject.property("rei_version")}")
    shadowMe("io.github.moulberry:neurepoparser:0.0.1")
    fun ktor(mod: String) = "io.ktor:ktor-$mod-jvm:${project.property("ktor_version")}"
    transInclude(implementation(ktor("client-core"))!!)
    transInclude(implementation(ktor("client-java"))!!)
    transInclude(implementation(ktor("serialization-kotlinx-json"))!!)
    transInclude(implementation(ktor("client-content-negotiation"))!!)

    // Dev environment preinstalled mods
    modRuntimeOnly("me.shedaniel:RoughlyEnoughItems-fabric:${project.property("rei_version")}")
    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:${project.property("devauth_version")}")
    modRuntimeOnly("maven.modrinth:modmenu:${project.property("modmenu_version")}")

    transInclude.resolvedConfiguration.resolvedArtifacts.forEach {
        include(it.moduleVersion.id.toString())
    }
}


version = rootProject.property("mod_version").toString()
group = rootProject.property("maven_group").toString()

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// could not set to 17, up to 16
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "16"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "16"
}

tasks.shadowJar {
    configurations = listOf(shadowMe)
    archiveClassifier.set("dev-thicc")
}

tasks.remapJar {
    injectAccessWidener.set(true)
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
    dependsOn(tasks.shadowJar)
    archiveClassifier.set("thicc")
}
