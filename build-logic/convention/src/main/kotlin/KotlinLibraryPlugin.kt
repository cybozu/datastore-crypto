import com.cybozu.cybozux.authentication.buildlogic.convention.configureKotlinCommon
import com.cybozu.cybozux.authentication.buildlogic.convention.configureLicenseCheck
import com.cybozu.cybozux.authentication.buildlogic.convention.configureLint
import com.cybozu.cybozux.authentication.buildlogic.convention.getPluginId
import com.cybozu.cybozux.authentication.buildlogic.convention.kotlinJvm
import com.cybozu.cybozux.authentication.buildlogic.convention.libs
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
