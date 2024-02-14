plugins {
    id ("com.github.minecraftschurlimods.helperplugin")
}

dependencies {
    implementation(helper.neoforge())
    compileOnly("org.jetbrains:annotations:23.0.0")
}

helper.publication.pom {
    organization {
        name = "Minecraftschurli Mods"
        url = "https://github.com/MinecraftschurliMods"
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
}
