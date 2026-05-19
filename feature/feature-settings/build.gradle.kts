plugins {
    id("morphocore.compose.feature")
}

android {
    namespace = "com.morphocore.feature.settings"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":theme:theme-api"))
}
