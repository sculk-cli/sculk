plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.jansi)
    implementation(libs.jna)
    implementation(libs.jna.platform)
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}
