package com.cybozu.datastore.crypto.buildlogic.convention

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.useDataStoreCryptoInternalApi() {
    tasks.withType(KotlinCompile::class.java).configureEach {
        compilerOptions.freeCompilerArgs.add("-opt-in=com.cybozu.datastore.crypto.DataStoreCryptoInternalApi")
    }
}
