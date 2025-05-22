import com.android.build.api.dsl.LibraryExtension
import com.cybozu.datastore.crypto.buildlogic.convention.android
import com.cybozu.datastore.crypto.buildlogic.convention.configureAndroidCommon
import com.cybozu.datastore.crypto.buildlogic.convention.configureKotlinCommon
import com.cybozu.datastore.crypto.buildlogic.convention.configureLicenseCheck
import com.cybozu.datastore.crypto.buildlogic.convention.configureLint
import com.cybozu.datastore.crypto.buildlogic.convention.getPluginId
import com.cybozu.datastore.crypto.buildlogic.convention.kotlinAndroid
import com.cybozu.datastore.crypto.buildlogic.convention.libs
import com.cybozu.datastore.crypto.buildlogic.convention.useDataStoreCryptoInternalApi
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.getPluginId("android-library"))
                apply(libs.getPluginId("kotlin-android"))
            }

            kotlinAndroid {
                explicitApi()
                configureKotlinCommon(this)
                useDataStoreCryptoInternalApi()
            }
            android<LibraryExtension> {
                configureAndroidCommon()
            }

            configureLicenseCheck()
            configureLint()
        }
    }
}
