package com.cybozu.datastore.crypto.preferences

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import com.cybozu.datastore.crypto.AeadProvider
import java.io.ByteArrayOutputStream
import java.security.GeneralSecurityException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source

/**
 * [Preferences]を暗号化/復号する[androidx.datastore.core.okio.OkioSerializer]の実装
 */
@OptIn(ExperimentalEncodingApi::class)
internal class PreferencesEncryptedSerializer(
    private val aeadProvider: AeadProvider,
) : OkioSerializer<Preferences> {
    override val defaultValue: Preferences
        get() = PreferencesSerializer.defaultValue

    override suspend fun readFrom(source: BufferedSource): Preferences {
        val encryptedData = Base64.Default.decode(source = source.readByteArray())
        val decryptedData = try {
            aeadProvider.getAead().decrypt(
                encryptedData,
                null // associatedData
            )
        } catch (cause: GeneralSecurityException) {
            throw CorruptionException("Failed to decrypt data", cause)
        }
        return PreferencesSerializer.readFrom(source = decryptedData.inputStream().source().buffer())
    }

    override suspend fun writeTo(t: Preferences, sink: BufferedSink) {
        val plainByteData = serializePreferences(t)

        val encryptedData = try {
            aeadProvider.getAead().encrypt(
                plainByteData,
                null // associatedData
            )
        } catch (cause: GeneralSecurityException) {
            throw CorruptionException("Failed to encrypt data", cause)
        }
        val encodedAndEncryptedData = Base64.Default.encodeToByteArray(encryptedData)
        sink.write(encodedAndEncryptedData)
    }

    private suspend fun serializePreferences(preferences: Preferences): ByteArray {
        val serializedData = ByteArrayOutputStream()
        val bufferedSink = serializedData.sink().buffer()
        PreferencesSerializer.writeTo(t = preferences, sink = bufferedSink)

        // データをOutputStreamに書き込むためにflush()を呼び出す
        // PreferencesSerializerはflush()を呼び出さない
        bufferedSink.flush()

        return serializedData.toByteArray()
    }
}
