plugins {
    id("morphocore.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.morphocore.app"
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:design-system"))
    implementation(project(":content:content-api"))
    implementation(project(":content:content-impl"))
    implementation(project(":content:content-sources"))
    implementation(project(":rendering:rendering-api"))
    implementation(project(":rendering:rendering-scene-view"))
    implementation(project(":theme:theme-api"))
    implementation(project(":theme:theme-impl"))
    implementation(project(":feature:feature-browse"))
    implementation(project(":feature:feature-movements"))
    implementation(project(":feature:feature-detail"))
    implementation(project(":feature:feature-settings"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation("androidx.appcompat:appcompat:1.7.0")
}
