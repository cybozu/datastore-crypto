import com.android.build.api.dsl.ApplicationExtension
import com.cybozu.datastore.crypto.buildlogic.convention.android
import com.cybozu.datastore.crypto.buildlogic.convention.configureAndroidCommon
import com.cybozu.datastore.crypto.buildlogic.convention.configureKotlinCommon
import com.cybozu.datastore.crypto.buildlogic.convention.configureLicenseCheck
import com.cybozu.datastore.crypto.buildlogic.convention.configureLint
import com.cybozu.datastore.crypto.buildlogic.convention.getPluginId
import com.cybozu.datastore.crypto.buildlogic.convention.kotlinAndroid
import com.cybozu.datastore.crypto.buildlogic.convention.libs
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
