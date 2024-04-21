plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.ktor.client.core)
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}
