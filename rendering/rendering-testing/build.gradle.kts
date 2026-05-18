plugins { id("morphocore.kotlin.library") }

dependencies {
    implementation(project(":rendering:rendering-api"))
    implementation(project(":core:domain"))
}
