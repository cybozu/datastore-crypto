import com.android.build.api.dsl.LibraryExtension
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

class PureAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.getPluginId("android-library"))
                apply(libs.getPluginId("kotlin-android"))
            }

            kotlinAndroid {
                explicitApi()
                configureKotlinCommon(this)
            }
            android<LibraryExtension> {
                configureAndroidCommon()
            }

            configureLicenseCheck()
            configureLint()
        }
    }
}
