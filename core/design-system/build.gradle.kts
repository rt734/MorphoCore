plugins {
    id("morphocore.compose.library")
}

android {
    namespace = "com.morphocore.core.designsystem"
}

dependencies {
    api(project(":core:domain"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
}
