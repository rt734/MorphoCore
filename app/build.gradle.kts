plugins { id("morphocore.android.application") }

android {
    namespace = "com.morphocore.app"
}
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":content:content-api"))
    implementation(project(":content:content-impl"))
    implementation(project(":content:content-sources"))
    implementation(project(":rendering:rendering-api"))
    implementation(project(":theme:theme-api"))
    implementation("androidx.appcompat:appcompat:1.7.0")
}
