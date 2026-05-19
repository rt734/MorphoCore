plugins {
    id("morphocore.compose.feature")
}

android {
    namespace = "com.morphocore.feature.detail"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":rendering:rendering-api"))
    implementation(project(":content:content-api"))
    implementation(project(":core:design-system"))
    implementation(project(":theme:theme-api"))
    implementation(project(":rendering:rendering-scene-view"))
    testImplementation(project(":content:content-testing"))
}
