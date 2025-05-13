plugins {
    alias(libs.plugins.cybozux.authentication.pure.android.library)
    alias(libs.plugins.cybozux.authentication.maven.publish)
}

android {
    namespace = "com.cybozu.cybozux.authentication.datastore.crypto"

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
