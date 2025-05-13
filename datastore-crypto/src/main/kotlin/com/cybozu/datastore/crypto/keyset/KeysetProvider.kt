package com.cybozu.datastore.crypto.keyset

import androidx.datastore.core.CorruptionException
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.integration.android.AndroidKeystore
import javax.crypto.BadPaddingException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val keystoreOperationMutex = Mutex()

/**
 * A class that provides encryption keys used for data encryption.
 *
 * @param keysetDataStore DataStore to save encryption keys.
 * @param masterKeyAlias Alias of the encryption key (MasterKey) used to encrypt the keys handled by [keysetDataStore].
 * The MasterKey itself is stored in the AndroidKeystore.
 * @param keysetAssociatedData Associated data for AEAD used when encrypting the keyset.
 * If multiple keysets are stored with the same [masterKeyAlias], the associated data used for keyset encryption must be different.
 */
internal class KeysetProvider(
    private val keysetDataStore: KeysetDataStore,
    private val masterKeyAlias: String,
    private val keysetAssociatedData: ByteArray,
) {
    /**
     * Returns the encryption key if it is saved.
     * If not saved, generates a new encryption key, saves it, and then returns it.
     *
     * @return [KeysetHandle] to operate the encryption key.
     */
    suspend fun getKeyset(): KeysetHandle {
        // AndroidKeystore is not thread-safe, so all operations accessing the Keystore are handled synchronously
        return keystoreOperationMutex.withLock {
            getOrCreateEncryptedKeyset()
        }
    }

    private suspend fun getOrCreateEncryptedKeyset(): KeysetHandle {
        val status = keysetDataStore.checkStatus(masterKeyAlias)
        return when (status) {
            is KeysetStatus.KeysetStored -> {
                readKeyset(status.encryptedKeyset)
            }

            KeysetStatus.KeysetNotStored -> {
                val newEncryptedKeyset = generateNewMasterKeyAndKeyset()
                keysetDataStore.updateData { newEncryptedKeyset }
                readKeyset(newEncryptedKeyset)
            }

            KeysetStatus.KeysetNotStoredButMasterKeyStored -> {
                // The most common case for this branch is encrypting different Keysets with the same MasterKey.
                // If you use the MasterKey from DataStoreA in another DataStoreB, the MasterKey is already generated when DataStoreA is created.
                // Therefore, when initializing DataStoreB, only the MasterKey exists.
                val newEncryptedKeyset = generateNewMasterKeyAndKeyset()
                keysetDataStore.updateData { newEncryptedKeyset }
                readKeyset(newEncryptedKeyset)
            }

            KeysetStatus.KeysetStoredButMasterKeyMissing -> {
                throw IllegalStateException("The master key is missing. If the phone is restored from a backup, the master key may be lost.")
            }
        }
    }

    private fun readKeyset(encryptedKeyset: EncryptedKeysetBinary): KeysetHandle =
        try {
            TinkProtoKeysetFormat.parseEncryptedKeyset(
                encryptedKeyset.rawKeyset,
                AndroidKeystore.getAead(masterKeyAlias),
                keysetAssociatedData
            )
        } catch (cause: BadPaddingException) {
            // @see https://github.com/tink-crypto/tink-java/blob/7d14939340c6f6e86991a872b25bb425586afc67/examples/android/helloworld/app/src/main/java/com/helloworld/TinkApplication.java#L132-L136
            throw CorruptionException(
                "Failed to decrypt the keyset. The encrypted keyset is corrupted, or it was encrypted with different master key.",
                cause
            )
        }

    private fun generateNewMasterKeyAndKeyset(): EncryptedKeysetBinary {
        AndroidKeystore.generateNewAes256GcmKey(masterKeyAlias)
        val keysetHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
        val rawKeyset = TinkProtoKeysetFormat.serializeEncryptedKeyset(
            keysetHandle,
            AndroidKeystore.getAead(masterKeyAlias),
            keysetAssociatedData
        )
        return EncryptedKeysetBinary(rawKeyset)
    }
}
