import java.time.Instant
import java.time.format.DateTimeFormatter

plugins {
    `java-library`
    eclipse
    idea
    `maven-publish`
    id("org.ec4j.editorconfig")
    id("net.neoforged.gradle.userdev")
}
//=============================================

editorconfig {
    excludes = listOf("**/run/**", "**/out/**", "**/.idea/**", "**/gradlew*")
    isExcludeNonSourceFiles = true
}

group = "${project.properties["lib_group"]}"
version = "${project.properties["mc_version"]}-${project.properties["lib_version"]}"
base.archivesName = "${project.properties["lib_name"]}"

if (System.getenv("RELEASE_TYPE") != null) {
    status = System.getenv("RELEASE_TYPE").lowercase()
    if (status == "snapshot") status = (status as String).uppercase()
} else {
    status = "SNAPSHOT"
}

if (status != "release") {
    version = "${version}-${status}"
}

java {
    withSourcesJar()
    withJavadocJar()

    toolchain {
        languageVersion = JavaLanguageVersion.of((project.properties["java_version"] as String).toInt())
        vendor = if (System.getenv("CI").toBoolean()) { JvmVendorSpec.ADOPTIUM } else { JvmVendorSpec.JETBRAINS }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "Minecraftschurli Maven"
        url = uri("https://minecraftschurli.ddns.net/repository/maven-public")
    }
}

dependencies {
    implementation("net.neoforged:neoforge:${project.properties["neo_version"]}")
    compileOnly("org.jetbrains:annotations:23.0.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.javadoc {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).tags = listOf(
        "side:a:Side:",
        "apiNote:a:API Note:",
        "implSpec:a:Implementation Requirements:",
        "implNote:a:Implementation Note:"
    )
    if (JavaVersion.current().isJava9Compatible) {
        (options as CoreJavadocOptions).addBooleanOption("html5", true)
    }
}

tasks.withType<Jar>().configureEach {
    from("LICENSE")
    manifest {
        attributes(mapOf(
            "Maven-Artifact"         to "${project.group}:${project.base.archivesName.get()}:${project.version}",
            "Specification-Title"    to base.archivesName.get(),
            "Specification-Vendor"   to project.properties["lib_vendor"],
            "Specification-Version"  to "1",
            "Implementation-Title"   to base.archivesName.get(),
            "Implementation-Version" to project.properties["lib_version"],
            "Implementation-Vendor"  to project.properties["lib_vendor"],
            "Built-On-Java"          to "${System.getProperty("java.vm.version")} (${System.getProperty("java.vm.vendor")})",
            "Built-On-Minecraft"     to project.properties["mc_version"],
            "Built-On"               to project.properties["neo_version"],
            "Timestamp"              to DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            "FMLModType"             to "GAMELIBRARY",
            "LICENSE"                to "MIT"
        ))
    }
}

artifacts {
    archives(tasks.jar)
    archives(tasks.named("sourcesJar"))
    archives(tasks.named("javadocJar"))
}

publishing {
    publications.create<MavenPublication>(base.archivesName.get()+"ToMaven") {
        groupId = project.group as String
        artifactId = base.archivesName.get()
        version = project.version as String
        from(components.getByName("java"))
        pom {
            name = project.name
            url = (project.properties["url"] as String)
            packaging = "jar"
            scm {
                connection = "scm:git:git://github.com/${project.properties["github"]}.git"
                developerConnection = "scm:git:git@github.com:${project.properties["github"]}.git"
                url = "https://github.com/${project.properties["github"]}"
            }
            issueManagement {
                system = "github"
                url = "https://github.com/${project.properties["github"]}.git/issues"
            }
            organization {
                name = "Minecraftschurli Mods"
                url = "https://github.com/Minecraftschurli"
            }
            developers {
                developer {
                    id = "minecraftschurli"
                    name = "Minecraftschurli"
                    email = "minecraftschurli@gmail.com"
                    organization = "Minecraftschurli Mods"
                    organizationUrl = "https://github.com/Minecraftschurli"
                    timezone = "Europe/Vienna"
                }
            }
            licenses {
                license {
                    name = "MIT"
                    url = "https://github.com/${project.properties["github"]}/blob/main/LICENSE"
                    distribution = "repo"
                }
            }
        }
    }
    repositories {
        maven {
            if ((System.getenv("MAVEN_USER") != null) &&
                (System.getenv("MAVEN_PASSWORD") != null) &&
                (System.getenv("MAVEN_URL") != null)
            ) {
                url = uri(System.getenv("MAVEN_URL"))
                credentials {
                    username = System.getenv("MAVEN_USER")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            } else {
                println("Using repo folder")
                url = uri(project.layout.buildDirectory.dir("repo"))
            }
        }
    }
}

tasks.publish {
    dependsOn(tasks.check)
}
