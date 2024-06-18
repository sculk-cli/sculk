import org.gradle.jvm.tasks.Jar
import java.io.FileOutputStream
import java.util.*

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

allprojects {
    group = "tech.jamalam"

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

    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)

    implementation(project(":modules:modrinth"))
    implementation(project(":modules:curseforge"))
    implementation(project(":modules:multimc"))
    implementation(project(":modules:util"))
}

kotlin {
    jvmToolchain(17)
}

val generatedResourcesDir = layout.buildDirectory.dir("generated-resources")
sourceSets.getByName("main").output.dir(generatedResourcesDir)

tasks {
    // People without API keys can use the proxy at curse.tools for development purposes.
    // In JARs built by me there will be an API key embedded in the JAR. I can't do much about you
    // extracting the JAR and using my API key, but please don't.
    create("createCurseforgeCredentialsFile") {
        val fileName = "curseforge-credentials.properties"
        outputs.file(generatedResourcesDir.get().asFile.resolve(fileName))

        val properties = Properties()
        properties["api_url"] = if (System.getenv()["CURSEFORGE_NEW_API_KEY"] != null) {
            "api.curseforge.com"
        } else {
            "api.curse.tools"
        }
        properties["api_base_path"] = if (System.getenv()["CURSEFORGE_NEW_API_KEY"] != null) {
            "/v1"
        } else {
            "/v1/cf"
        }
        properties["api_key"] = System.getenv()["CURSEFORGE_NEW_API_KEY"] ?: "unauthenticated"

        doLast {
            properties.store(
                FileOutputStream(generatedResourcesDir.get().asFile.resolve(fileName)),
                null
            )
        }
    }

    create("createVersionFile") {
        val fileName = "version"
        outputs.file(generatedResourcesDir.get().asFile.resolve(fileName))

        doLast {
            FileOutputStream(generatedResourcesDir.get().asFile.resolve(fileName)).use {
                it.write(project.version.toString().toByteArray())
            }
        }
    }

    getByName("processResources") {
        dependsOn("createCurseforgeCredentialsFile")
        dependsOn("createVersionFile")
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        manifest {
            attributes["Main-Class"] = "tech.jamalam.MainKt"
        }

        fromConfiguration(configurations.runtimeClasspath)
        fromConfiguration(configurations.compileClasspath)
    }
}

fun Jar.fromConfiguration(configuration: NamedDomainObjectProvider<Configuration>) {
    from(configuration.get().map {
        if (it.isDirectory) {
            it
        } else zipTree(it)
    })
}
