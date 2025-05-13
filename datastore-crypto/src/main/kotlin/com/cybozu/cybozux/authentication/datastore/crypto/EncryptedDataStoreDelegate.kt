package com.cybozu.cybozux.authentication.datastore.crypto

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.dataStoreFile
import com.cybozu.cybozux.authentication.datastore.crypto.keyset.keysetDataStore
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * データを暗号化してセキュアに扱う[DataStore]を生成する。
 * この関数で作成されるDataStoreは単一のプロセスでのみの利用を前提としている。
 *
 * @param fileName 保存先であるファイルの名前。
 * @param serializer 暗号化対象のデータを読み書きするための[Serializer]。
 * このserializerは暗号処理を考慮せず、平文のままデータを扱っていることを期待する。
 * @param masterKeyAlias [T]を暗号化するために使用する暗号鍵 (MasterKey) のエイリアス。
 * MasterKey本体はAndroidKeystoreに保存される。
 *
 * その他のインターフェースは[androidx.datastore.dataStore]に準拠する。
 */
public fun <T> encryptedDataStore(
    fileName: String,
    serializer: Serializer<T>,
    masterKeyAlias: String,
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
    produceMigrations: (Context) -> List<DataMigration<T>> = { listOf() },
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
): ReadOnlyProperty<Context, DataStore<T>> = EncryptedDataStoreSingletonDelegate(
    fileName,
    serializer,
    masterKeyAlias,
    corruptionHandler,
    produceMigrations,
    scope
)

internal class EncryptedDataStoreSingletonDelegate<T>(
    private val fileName: String,
    private val serializer: Serializer<T>,
    private val masterKeyAlias: String,
    private val corruptionHandler: ReplaceFileCorruptionHandler<T>?,
    private val produceMigrations: (Context) -> List<DataMigration<T>>,
    private val scope: CoroutineScope,
) : ReadOnlyProperty<Context, DataStore<T>> {
    private val lock = Any()

    @GuardedBy("lock")
    @Volatile
    private var instance: DataStore<T>? = null

    private val Context.keysetDataStore by keysetDataStore(
        fileName = "${fileName}_keyset",
        scope = scope
    )

    override fun getValue(thisRef: Context, property: KProperty<*>): DataStore<T> = instance ?: synchronized(lock) {
        if (instance == null) {
            val applicationContext = thisRef.applicationContext
            instance = DataStoreFactory.create(
                storage = OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = GenericEncryptedSerializer(
                        plainDataSerializer = serializer,
                        aeadProvider = AeadProvider(
                            keysetDataStore = applicationContext.keysetDataStore,
                            masterKeyAlias = masterKeyAlias,
                            keysetAssociatedData = fileName.toByteArray()
                        )
                    ),
                    producePath = {
                        applicationContext.dataStoreFile(fileName).absolutePath.toPath()
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
