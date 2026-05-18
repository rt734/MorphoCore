plugins { id("morphocore.kotlin.library") }
dependencies {
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.core)
}
