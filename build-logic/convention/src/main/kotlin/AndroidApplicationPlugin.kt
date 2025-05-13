import com.android.build.api.dsl.ApplicationExtension
import com.cybozu.cybozux.authentication.buildlogic.convention.android
import com.cybozu.cybozux.authentication.buildlogic.convention.configureAndroidCommon
import com.cybozu.cybozux.authentication.buildlogic.convention.configureKotlinCommon
import com.cybozu.cybozux.authentication.buildlogic.convention.configureLicenseCheck
import com.cybozu.cybozux.authentication.buildlogic.convention.configureLint
import com.cybozu.cybozux.authentication.buildlogic.convention.getPluginId
import com.cybozu.cybozux.authentication.buildlogic.convention.kotlinAndroid
import com.cybozu.cybozux.authentication.buildlogic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.getPluginId("android-application"))
                apply(libs.getPluginId("kotlin-android"))
            }

            kotlinAndroid {
                configureKotlinCommon(this)
            }

            android<ApplicationExtension> {
                configureAndroidCommon()
                defaultConfig {
                    targetSdk = 35
                }
            }

            configureLicenseCheck()
            configureLint()
        }
    }
}
