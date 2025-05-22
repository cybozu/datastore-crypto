plugins {
    alias(libs.plugins.datastore.crypto.android.library)
    alias(libs.plugins.datastore.crypto.maven.publish)
}

android {
    namespace = "com.cybozu.datastore.crypto"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":internal"))

    api(libs.androidx.datastore)
    implementation(libs.tink.android)

    androidTestImplementation(project(":datastore-testing"))
    androidTestImplementation(libs.bundles.android.instrumented.test)
    androidTestImplementation(libs.kotest.assertions.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
