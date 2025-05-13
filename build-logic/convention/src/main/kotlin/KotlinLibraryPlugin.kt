import com.cybozu.datastore.crypto.buildlogic.convention.configureKotlinCommon
import com.cybozu.datastore.crypto.buildlogic.convention.configureLicenseCheck
import com.cybozu.datastore.crypto.buildlogic.convention.configureLint
import com.cybozu.datastore.crypto.buildlogic.convention.getPluginId
import com.cybozu.datastore.crypto.buildlogic.convention.kotlinJvm
import com.cybozu.datastore.crypto.buildlogic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("java-library")
                apply(libs.getPluginId("kotlin-jvm"))
            }
            kotlinJvm {
                explicitApi()
                configureKotlinCommon(this)
            }
            configureLicenseCheck()
            configureLint()
        }
    }
}
