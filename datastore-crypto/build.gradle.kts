plugins {
    alias(libs.plugins.datastore.crypto.pure.android.library)
    alias(libs.plugins.datastore.crypto.maven.publish)
}

android {
    namespace = "com.cybozu.datastore.crypto"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    api(libs.androidx.datastore)
    api(libs.androidx.datastore.preferences)
    implementation(libs.tink.android)

    androidTestImplementation(libs.bundles.android.instrumented.test)
    androidTestImplementation(libs.kotest.assertions.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
