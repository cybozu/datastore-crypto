plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint.gradle)
}

group = "com.cybozu.cybozux.authentication.buildlogic"

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
            id = libs.plugins.cybozux.authentication.android.application.get().pluginId
            implementationClass = "AndroidApplicationPlugin"
        }
        register("cybozuxAuthAndroidLibrary") {
            id = libs.plugins.cybozux.authentication.android.library.get().pluginId
            implementationClass = "CybozuxAuthAndroidLibraryPlugin"
        }
        register("pureAndroidLibrary") {
            id = libs.plugins.cybozux.authentication.pure.android.library.get().pluginId
            implementationClass = "PureAndroidLibraryPlugin"
        }
        register("kotlinLibrary") {
            id = libs.plugins.cybozux.authentication.kotlin.library.get().pluginId
            implementationClass = "KotlinLibraryPlugin"
        }
        register("screenshotTest") {
            id = libs.plugins.cybozux.authentication.screenshot.test.get().pluginId
            implementationClass = "ScreenshotTestPlugin"
        }
        register("mavenPublish") {
            id = libs.plugins.cybozux.authentication.maven.publish.get().pluginId
            implementationClass = "MavenPublishingPlugin"
        }
    }
}
