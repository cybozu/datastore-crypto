package com.cybozu.cybozux.authentication.datastore.crypto.preferences

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.cybozu.cybozux.authentication.datastore.crypto.AeadProvider
import com.cybozu.cybozux.authentication.datastore.crypto.keyset.keysetDataStore
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * [Preferences]を暗号化してセキュアに保存する[DataStore]を生成する。
 * この関数で作成されるDataStoreは単一のプロセスでのみの利用を前提としている。
 *
 * @param name Preferencesの名前。[name]は保存ファイルの名前に使用される。
 * ファイル名の詳細は[preferencesDataStoreFile]を参照すること。
 * @param masterKeyAlias [Preferences]を暗号化するために使用される暗号鍵 (MasterKey) のエイリアス。
 * MasterKey本体はAndroidKeystoreに保存される。
 *
 * その他のインターフェースは[androidx.datastore.preferences.preferencesDataStore]に準拠する。
 */
public fun encryptedPreferencesDataStore(
    name: String,
    masterKeyAlias: String,
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    produceMigrations: (Context) -> List<DataMigration<Preferences>> = { listOf() },
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
): ReadOnlyProperty<Context, DataStore<Preferences>> = EncryptedPreferencesDataStoreDelegate(
    name,
    masterKeyAlias,
    corruptionHandler,
    produceMigrations,
    scope
)

internal class EncryptedPreferencesDataStoreDelegate(
    private val name: String,
    private val masterKeyAlias: String,
    private val corruptionHandler: ReplaceFileCorruptionHandler<Preferences>?,
    private val produceMigrations: (Context) -> List<DataMigration<Preferences>>,
    private val scope: CoroutineScope,
) : ReadOnlyProperty<Context, DataStore<Preferences>> {
    private val lock = Any()

    @GuardedBy("lock")
    @Volatile
    private var instance: DataStore<Preferences>? = null

    private val Context.keysetDataStore by keysetDataStore(
        fileName = "${name}_keyset",
        scope = scope
    )

    override fun getValue(thisRef: Context, property: KProperty<*>): DataStore<Preferences> = instance ?: synchronized(lock) {
        if (instance == null) {
            val applicationContext = thisRef.applicationContext
            instance = PreferenceDataStoreFactory.create(
                storage = OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = PreferencesEncryptedSerializer(
                        aeadProvider = AeadProvider(
                            keysetDataStore = applicationContext.keysetDataStore,
                            masterKeyAlias = masterKeyAlias,
                            keysetAssociatedData = name.toByteArray()
                        )
                    ),
                    producePath = {
                        applicationContext.preferencesDataStoreFile(name).absolutePath.toPath()
                    }
                ),
                corruptionHandler = corruptionHandler,
                migrations = produceMigrations(applicationContext),
                scope = scope
            )
        }
        instance!!
    }
}
