plugins {
    id("morphocore.android.library")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.morphocore.content.sources"
}

dependencies {
    implementation(project(":content:content-api"))
    implementation(project(":core:common"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
