plugins { id("morphocore.android.library") }

android {
    namespace = "com.morphocore.content.sources"
}

dependencies {
    implementation(project(":content:content-api"))
    implementation(project(":core:common"))
}
