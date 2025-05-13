package com.cybozu.cybozux.authentication.buildlogic.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

fun <T> Project.configureKotlinCommon(kotlinExtension: T) where T : KotlinProjectExtension, T : HasConfigurableKotlinCompilerOptions<*> {
    kotlinExtension.apply {
        compilerOptions {
            allWarningsAsErrors = propOrDef(propertyName = "warningsAsErrors", defaultValue = "false").toBoolean()
            freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
        }
    }

    java {
        toolchain {
            languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(21))
        }
    }
}
