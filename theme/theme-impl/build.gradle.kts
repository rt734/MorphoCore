plugins {
    id("morphocore.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.morphocore.theme.impl"
}

dependencies {
    implementation(project(":theme:theme-api"))
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
