package com.cybozu.datastore.crypto.buildlogic.convention

import com.android.build.api.dsl.CommonExtension

fun CommonExtension<*, *, *, *, *, *>.configureAndroidCommon() {
    compileSdk = 35

    defaultConfig {
        minSdk = 27
    }

    buildFeatures {
        buildConfig = true
    }
}
