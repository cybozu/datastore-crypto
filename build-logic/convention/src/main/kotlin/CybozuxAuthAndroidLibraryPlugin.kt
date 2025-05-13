import com.cybozu.cybozux.authentication.buildlogic.convention.kotlinAndroid
import com.cybozu.cybozux.authentication.buildlogic.convention.useCybozuxAuthenticationInternalApi
import org.gradle.api.Plugin
import org.gradle.api.Project

class CybozuxAuthAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(PureAndroidLibraryPlugin::class.java)
            }
            kotlinAndroid {
                useCybozuxAuthenticationInternalApi()
            }
        }
    }
}
