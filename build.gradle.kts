import java.io.FileOutputStream
import java.util.*

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

allprojects {
    group = "tech.jamalam"
    version = "1.0.0-alpha.1"

    repositories {
        mavenCentral()
    }
}

dependencies {
    implementation(libs.clikt)
    implementation(libs.jna)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.mordant)
    implementation(libs.mordant.coroutines)

    implementation(project(":modules:modrinth"))
    implementation(project(":modules:curseforge"))
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "tech.jamalam.MainKt"
}

val generatedResourcesDir = "${layout.buildDirectory}/generated-resources"
sourceSets.getByName("main").output.dir(generatedResourcesDir)

tasks {
    // People without API keys can use the proxy at curse.tools for development purposes.
    // In JARs built by me there will be an API key embedded in the JAR. I can't do much about you
    // extracting the JAR and using my API key, but please don't.
    create("createCurseforgeCredentialsFile") {
        val fileName = "curseforge-credentials.properties"
        outputs.file("$generatedResourcesDir/$fileName")

        val properties = Properties()
        properties["api_url"] = if (System.getenv()["CURSEFORGE_NEW_API_KEY"] != null) {
            "https://api.curseforge.com/v1"
        } else {
            "https://api.curse.tools/v1/cf"
        }
        properties["api_key"] = System.getenv()["CURSEFORGE_NEW_API_KEY"] ?: "unauthenticated"

        doLast {
            properties.store(FileOutputStream(File("$generatedResourcesDir/$fileName")), null)
        }
    }

    getByName("processResources") {
        dependsOn("createCurseforgeCredentialsFile")
    }
}
