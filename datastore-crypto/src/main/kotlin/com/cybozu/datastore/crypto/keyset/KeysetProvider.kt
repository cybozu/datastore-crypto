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
 * データの暗号処理に使う暗号鍵を提供するクラス。
 *
 * @param keysetDataStore 暗号鍵を保存するDataStore。
 * @param masterKeyAlias [keysetDataStore]で扱う鍵を暗号化するために使用される暗号鍵 (MasterKey) のエイリアス。
 * MasterKey本体はAndroidKeystoreに保存される。
 * @param keysetAssociatedData keysetを暗号化する際に使用するAEADの関連データ。
 * 同一の[masterKeyAlias]で複数のkeysetを保存する場合、keyset暗号化時に使用する関連データは異なる必要がある。
 */
internal class KeysetProvider(
    private val keysetDataStore: KeysetDataStore,
    private val masterKeyAlias: String,
    private val keysetAssociatedData: ByteArray,
) {
    /**
     * 暗号鍵が保存されていればそれを返す。
     * 保存されていなければ新しい暗号鍵を生成して保存した後、それを返す。
     *
     * @return 暗号鍵を操作するための[KeysetHandle]。
     */
    suspend fun getKeyset(): KeysetHandle {
        // AndroidKeystoreはスレッドセーフではないので、Keystoreへアクセスする操作をまるっと同期処理にしている
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
                // この分岐の最も一般的なケースは、同一のMasterKeyで別のKeysetを暗号化するケース。
                // DataStoreAに使ったMasterKeyを別のDataStoreBに使った場合、DataStoreAを作った時点でMasterKeyが生成されている。
                // よって、DataStoreBの初期化時にはMasterKeyのみが存在する状態になる。
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
