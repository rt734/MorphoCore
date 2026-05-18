plugins {
    id("morphocore.compose.feature")
}

android {
    namespace = "com.morphocore.feature.movements"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:design-system"))
    implementation(project(":content:content-api"))
    testImplementation(project(":content:content-testing"))
}
