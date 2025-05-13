import com.cybozu.datastore.crypto.buildlogic.convention.kotlinAndroid
import com.cybozu.datastore.crypto.buildlogic.convention.useDatastoreCryptoInternalApi
import org.gradle.api.Plugin
import org.gradle.api.Project

class DatastoreCryptoAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(PureAndroidLibraryPlugin::class.java)
            }
            kotlinAndroid {
                useDatastoreCryptoInternalApi()
            }
        }
    }
}
