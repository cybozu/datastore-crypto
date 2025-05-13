package com.cybozu.datastore.crypto.buildlogic.convention

import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions

fun HasConfigurableKotlinCompilerOptions<*>.useDatastoreCryptoInternalApi() {
    compilerOptions.freeCompilerArgs.add("-opt-in=com.cybozu.datastore.crypto.core.DatastoreCryptoInternalApi")
}
