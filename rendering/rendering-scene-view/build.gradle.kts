plugins { id("morphocore.compose.library") }

android {
    namespace = "com.morphocore.rendering.sceneview"
}

dependencies {
    implementation(project(":rendering:rendering-api"))
    implementation(project(":core:domain"))
    implementation(libs.sceneview)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
}
