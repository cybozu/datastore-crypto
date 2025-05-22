package com.cybozu.datastore.crypto.keyset

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.cybozu.datastore.crypto.internal.DataStoreCryptoInternalApi
import com.google.crypto.tink.integration.android.AndroidKeystore
import com.google.crypto.tink.subtle.Hex
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import kotlin.properties.ReadOnlyProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

/**
 * A typealias for a [DataStore] that stores [EncryptedKeysetBinary] objects.
 *
 * This typealias simplifies the usage of DataStore for managing encrypted keysets.
 * [EncryptedKeysetBinary] represents a binary of an encrypted keyset, and this DataStore
 * does not perform encryption itself but stores the encrypted data.
 */
@DataStoreCryptoInternalApi
public typealias KeysetDataStore = DataStore<EncryptedKeysetBinary>

/**
 * Generates a DataStore to store encryption keys.
 * This DataStore is used to encrypt the data handled by the DataStore generated with [encryptedDataStore].
 * [EncryptedKeysetBinary] is a binary of [com.google.crypto.tink.proto.Keyset] encrypted by Tink,
 * and the DataStore created by this function does not perform encryption itself.
 *
 * @param fileName The name of the file where it will be saved.
 * @param scope CoroutineScope to run the KeysetDataStore.
 * @return A [ReadOnlyProperty] that provides access to a [KeysetDataStore], which is a DataStore for managing
 * encrypted keysets.
 */
@DataStoreCryptoInternalApi
public fun keysetDataStore(
    fileName: String,
    scope: CoroutineScope,
): ReadOnlyProperty<Context, KeysetDataStore> = dataStore(
    fileName = fileName,
    serializer = EncryptedKeysetBinarySerializer,
    scope = scope
)

private object EncryptedKeysetBinarySerializer : Serializer<EncryptedKeysetBinary> {
    private val byteStringCharset: Charset
        get() = Charsets.UTF_8

    override val defaultValue: EncryptedKeysetBinary = EncryptedKeysetBinary.EMPTY

    override suspend fun readFrom(input: InputStream): EncryptedKeysetBinary {
        val encryptedKeysetString = input.readBytes().toString(byteStringCharset)
        val encryptedKeysetByte = Hex.decode(encryptedKeysetString)
        return EncryptedKeysetBinary(rawKeyset = encryptedKeysetByte)
    }

    override suspend fun writeTo(t: EncryptedKeysetBinary, output: OutputStream) {
        val encryptedKeysetHexString = Hex.encode(t.rawKeyset)
        output.write(encryptedKeysetHexString.toByteArray(byteStringCharset))
    }
}

@DataStoreCryptoInternalApi
@JvmInline
public value class EncryptedKeysetBinary internal constructor(internal val rawKeyset: ByteArray) {

    // Since Keyset is sensitive data, the internal data is not printed
    override fun toString(): String = "EncryptedKeysetBinary@${hashCode().toString(16)}"

    public companion object {
        public val EMPTY: EncryptedKeysetBinary = EncryptedKeysetBinary(ByteArray(size = 0))
    }
}

internal sealed interface KeysetStatus {
    data class KeysetStored(val encryptedKeyset: EncryptedKeysetBinary) : KeysetStatus
    data object KeysetNotStored : KeysetStatus
    data object KeysetStoredButMasterKeyMissing : KeysetStatus
    data object KeysetNotStoredButMasterKeyStored : KeysetStatus
}

internal suspend fun KeysetDataStore.checkStatus(masterKeyAlias: String): KeysetStatus {
    val encryptedKeyset = data.first()
    val masterKeyExists = AndroidKeystore.hasKey(masterKeyAlias)

    return when {
        encryptedKeyset != EncryptedKeysetBinary.EMPTY && masterKeyExists -> KeysetStatus.KeysetStored(encryptedKeyset)
        encryptedKeyset == EncryptedKeysetBinary.EMPTY && masterKeyExists -> KeysetStatus.KeysetNotStoredButMasterKeyStored
        encryptedKeyset != EncryptedKeysetBinary.EMPTY && masterKeyExists.not() -> KeysetStatus.KeysetStoredButMasterKeyMissing
        encryptedKeyset == EncryptedKeysetBinary.EMPTY && masterKeyExists.not() -> KeysetStatus.KeysetNotStored
        else -> error("Do not reach here")
    }
}
