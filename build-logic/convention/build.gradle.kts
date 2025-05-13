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
    compileOnly(libs.roborazzi.gradle.plugin)
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
        register("cybozuxAuthAndroidLibrary") {
            id = libs.plugins.datastore.crypto.android.library.get().pluginId
            implementationClass = "CybozuxAuthAndroidLibraryPlugin"
        }
        register("pureAndroidLibrary") {
            id = libs.plugins.datastore.crypto.pure.android.library.get().pluginId
            implementationClass = "PureAndroidLibraryPlugin"
        }
        register("kotlinLibrary") {
            id = libs.plugins.datastore.crypto.kotlin.library.get().pluginId
            implementationClass = "KotlinLibraryPlugin"
        }
        register("screenshotTest") {
            id = libs.plugins.datastore.crypto.screenshot.test.get().pluginId
            implementationClass = "ScreenshotTestPlugin"
        }
        register("mavenPublish") {
            id = libs.plugins.datastore.crypto.maven.publish.get().pluginId
            implementationClass = "MavenPublishingPlugin"
        }
    }
}
