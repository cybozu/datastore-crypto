package com.cybozu.datastore.crypto.preferences

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.cybozu.datastore.crypto.AeadProvider
import com.cybozu.datastore.crypto.keyset.keysetDataStore
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * Creates a [DataStore] that securely stores [Preferences] in encrypted form.
 * The DataStore created by this function is intended for use in a single process only.
 *
 * @param name The name of the Preferences. [name] is used as the file name for storage.
 * For details on the file name, refer to [preferencesDataStoreFile].
 * @param masterKeyAlias The alias of the encryption key (MasterKey) used to encrypt [Preferences].
 * The MasterKey itself is stored in the AndroidKeystore.
 *
 * Other interfaces are based on [androidx.datastore.preferences.preferencesDataStore].
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
