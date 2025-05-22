package com.cybozu.datastore.crypto.buildlogic.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

fun Project.useDataStoreCryptoInternalApi() {
    dependencies {
        add("implementation", project(":internal"))
    }
    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
        compilerOptions.freeCompilerArgs.add("-opt-in=com.cybozu.datastore.crypto.internal.DataStoreCryptoInternalApi")
    }
}
