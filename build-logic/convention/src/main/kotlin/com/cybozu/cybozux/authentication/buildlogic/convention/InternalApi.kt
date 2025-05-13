package com.cybozu.cybozux.authentication.buildlogic.convention

import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions

fun HasConfigurableKotlinCompilerOptions<*>.useCybozuxAuthenticationInternalApi() {
    compilerOptions.freeCompilerArgs.add("-opt-in=com.cybozu.cybozux.authentication.core.CybozuxAuthenticationInternalApi")
}
