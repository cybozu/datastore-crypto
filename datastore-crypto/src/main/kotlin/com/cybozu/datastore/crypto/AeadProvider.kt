package com.cybozu.datastore.crypto

import com.cybozu.datastore.crypto.internal.DataStoreCryptoInternalApi
import com.cybozu.datastore.crypto.keyset.KeysetDataStore
import com.cybozu.datastore.crypto.keyset.KeysetProvider
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.config.TinkConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@DataStoreCryptoInternalApi
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
 * AeadProvider is responsible for providing an instance of [Aead], a cryptographic primitive for authenticated encryption.
 *
 * This class uses a [KeysetProvider] to retrieve the cryptographic keyset and creates an [Aead] instance
 * using Tink's cryptographic library. The created [Aead] instance is cached in memory for efficient reuse.
 *
 * Usage:
 * - Call [getAead] to retrieve the [Aead] instance. If no instance exists, it will be created and cached.
 *
 * Note: The constructor is `internal`, so this class is intended to be instantiated indirectly through
 * the public factory function [AeadProvider].
 */
@DataStoreCryptoInternalApi
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
