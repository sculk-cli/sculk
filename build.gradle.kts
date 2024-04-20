plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "tech.jamalam"
version = "1.0.0-alpha.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.clikt)
    implementation(libs.jna)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.mordant)
    implementation(libs.mordant.coroutines)
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "tech.jamalam.MainKt"
}
