plugins {
    id("morphocore.kotlin.library")
    alias(libs.plugins.kotlin.serialization)
}
dependencies {
    implementation(project(":content:content-api"))
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
