package com.cybozu.datastore.crypto

import com.cybozu.datastore.crypto.keyset.KeysetDataStore
import com.cybozu.datastore.crypto.keyset.KeysetProvider
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.config.TinkConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public fun AeadProvider(
    keysetDataStore: KeysetDataStore,
    masterKeyAlias: String,
    keysetAssociatedData: ByteArray,
): AeadProvider = AeadProvider(
    keysetProvider = KeysetProvider(
        keysetDataStore = keysetDataStore,
        masterKeyAlias = masterKeyAlias,
        keysetAssociatedData = keysetAssociatedData
    )
)

/**
 * Provides an instance of [Aead].
 * If there is no [Aead] instance, it will be created using [keysetProvider] and cached in memory.
 */
public class AeadProvider internal constructor(
    private val keysetProvider: KeysetProvider,
) {
    private val writeCacheMutex = Mutex()
    private var cachedAead: Aead? = null

    /**
     * Returns an instance of [Aead].
     * If there is no [Aead] instance, it will be created using [keysetProvider] and cached in memory.
     */
    public suspend fun getAead(): Aead = cachedAead ?: writeCacheMutex.withLock {
        createAead().also {
            cachedAead = it
        }
    }

    private suspend fun createAead(): Aead {
        TinkConfig.register()
        val keysetHandle = keysetProvider.getKeyset()
        return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }
}
