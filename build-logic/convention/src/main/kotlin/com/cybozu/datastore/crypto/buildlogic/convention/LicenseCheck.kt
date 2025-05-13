package com.cybozu.datastore.crypto.buildlogic.convention

import app.cash.licensee.LicenseeExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

fun Project.configureLicenseCheck() {
    pluginManager.apply(libs.getPluginId("licensee"))

    licensee {
        // https://bozuman.cybozu.com/k/8979/show#record=11
        allow("Apache-2.0")

        // https://bozuman.cybozu.com/k/8979/show#record=12
        allow("MIT")

        // https://bozuman.cybozu.com/k/8979/show#record=10
        allow("BSD-3-Clause")
    }
}

private fun Project.licensee(action: LicenseeExtension.() -> Unit) = configure(action)
