package com.cybozu.datastore.crypto.keyset

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.crypto.tink.integration.android.AndroidKeystore
import com.google.crypto.tink.subtle.Hex
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import kotlin.properties.ReadOnlyProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

internal typealias KeysetDataStore = DataStore<EncryptedKeysetBinary>

/**
 * 暗号鍵を保管するDataStoreを生成する。
 * このDataStoreは[encryptedDataStore]で生成したDataStoreが扱うデータを暗号化するために使用する。
 * [EncryptedKeysetBinary]はTinkによって暗号化された[com.google.crypto.tink.proto.Keyset]のバイナリであり、
 * この関数で作られたDataStore自身は暗号処理を行わない。
 *
 * @param fileName 保存先であるファイルの名前。
 * @param scope KeysetDataStoreを動かすCoroutineScope。
 */
internal fun keysetDataStore(
    fileName: String,
    scope: CoroutineScope,
): ReadOnlyProperty<Context, KeysetDataStore> =
    dataStore(
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

@JvmInline
internal value class EncryptedKeysetBinary(val rawKeyset: ByteArray) {

    // Keysetはセンシティブなデータなので、中のデータはPrintしない
    override fun toString(): String = "EncryptedKeysetBinary@${hashCode().toString(16)}"

    companion object {
        val EMPTY: EncryptedKeysetBinary = EncryptedKeysetBinary(ByteArray(size = 0))
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
