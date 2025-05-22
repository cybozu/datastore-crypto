plugins {
    alias(libs.plugins.datastore.crypto.android.library)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.cybozu.datastore.crypto.preferences"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    api(project(":datastore-crypto"))
    api(libs.androidx.datastore.preferences)
    implementation(libs.tink.android)

    androidTestImplementation(project(":datastore-testing"))
    androidTestImplementation(libs.bundles.android.instrumented.test)
    androidTestImplementation(libs.kotest.assertions.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
