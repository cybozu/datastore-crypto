plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint.gradle)
}

group = "com.cybozu.datastore.crypto.buildlogic"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.licensee.gradle.plugin)
    compileOnly(libs.ktlint.gradle.plugin)
}

ktlint {
    version = libs.versions.ktlint
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.datastore.crypto.android.application.get().pluginId
            implementationClass = "AndroidApplicationPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.datastore.crypto.android.library.get().pluginId
            implementationClass = "AndroidLibraryPlugin"
        }
        register("kotlinLibrary") {
            id = libs.plugins.datastore.crypto.kotlin.library.get().pluginId
            implementationClass = "KotlinLibraryPlugin"
        }
    }
}
