plugins { id("morphocore.kotlin.library") }

dependencies {
    implementation(project(":content:content-api"))
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.core)
}
