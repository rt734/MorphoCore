plugins {
    id("morphocore.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.morphocore.app"
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":content:content-api"))
    implementation(project(":content:content-impl"))
    implementation(project(":content:content-sources"))
    implementation(project(":rendering:rendering-api"))
    implementation(project(":rendering:rendering-scene-view"))
    implementation(project(":theme:theme-api"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation("androidx.appcompat:appcompat:1.7.0")
}
