package com.cybozu.cybozux.authentication.datastore.crypto

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.core.okio.OkioSerializer
import java.io.ByteArrayOutputStream
import java.security.GeneralSecurityException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import okio.BufferedSink
import okio.BufferedSource

/**
 * [plainDataSerializer] で読み書きするデータを暗号化/復号する[androidx.datastore.core.okio.OkioSerializer]の実装。
 */
@OptIn(ExperimentalEncodingApi::class)
internal class GenericEncryptedSerializer<T>(
    private val plainDataSerializer: Serializer<T>,
    private val aeadProvider: AeadProvider,
) : OkioSerializer<T> {
    override val defaultValue: T
        get() = plainDataSerializer.defaultValue

    override suspend fun readFrom(source: BufferedSource): T {
        val encryptedData = Base64.Default.decode(source = source.readByteArray())
        val decryptedData = try {
            aeadProvider.getAead().decrypt(
                encryptedData,
                null // associatedData
            )
        } catch (cause: GeneralSecurityException) {
            throw CorruptionException("Failed to decrypt data", cause)
        }
        return plainDataSerializer.readFrom(decryptedData.inputStream())
    }

    override suspend fun writeTo(t: T, sink: BufferedSink) {
        val plainByteData = ByteArrayOutputStream()
        plainDataSerializer.writeTo(t, plainByteData)

        val encryptedData = try {
            aeadProvider.getAead().encrypt(
                plainByteData.toByteArray(),
                null // associatedData
            )
        } catch (cause: GeneralSecurityException) {
            throw CorruptionException("Failed to encrypt data", cause)
        }
        val encodedAndEncryptedData = Base64.Default.encodeToByteArray(encryptedData)
        sink.write(encodedAndEncryptedData)
    }
}
