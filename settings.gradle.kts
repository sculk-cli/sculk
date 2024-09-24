plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "sculk"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("versions.toml"))
        }
    }
}

include(":modules:curseforge")
include(":modules:modrinth")
include(":modules:multimc")
include(":modules:console")
include(":modules:util")
