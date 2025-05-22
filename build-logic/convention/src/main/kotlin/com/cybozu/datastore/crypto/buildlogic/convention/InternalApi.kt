package com.cybozu.datastore.crypto.buildlogic.convention

import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions

fun HasConfigurableKotlinCompilerOptions<*>.useDataStoreCryptoInternalApi() {
    compilerOptions.freeCompilerArgs.add("-opt-in=com.cybozu.datastore.crypto.internal.DataStoreCryptoInternalApi")
}
