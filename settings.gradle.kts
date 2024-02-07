pluginManagement {
    plugins {
        id("org.ec4j.editorconfig") version "0.0.3"
        id("net.neoforged.gradle.userdev") version "7.0.80"
    }
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven { url = uri("https://maven.neoforged.net/releases") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "CodecLib"
