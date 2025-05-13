package com.cybozu.datastore.crypto.keyset

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cybozu.datastore.crypto.util.clearDataStore
import com.google.crypto.tink.integration.android.AndroidKeystore
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class KeysetDataStoreTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val testMasterKeyAlias = "test_master_key"

    @Before
    fun setUp() {
        context.clearDataStore()
        AndroidKeystore.deleteKey(testMasterKeyAlias)
    }

    @Test
    fun when_there_is_no_saved_data_in_keysetdatastore_returns_an_empty_keyset() = runTest {
        context.testWithKeysetDataStore(fileName = "empty_keyset") { keysetDataStore ->
            keysetDataStore.data.first() shouldBe EncryptedKeysetBinary.EMPTY
        }
    }

    @Test
    fun can_read_and_write_keyset() = runTest {
        val newKeyset = EncryptedKeysetBinary(byteArrayOf(1, 2, 3))

        context.testWithKeysetDataStore(fileName = "read_write_keyset") { keysetDataStore ->
            keysetDataStore.updateData { newKeyset }
            keysetDataStore.data.first() shouldBe newKeyset
        }
    }

    @Test
    fun when_keyset_is_saved_and_masterkey_exists_returns_the_saved_keyset() = runTest {
        AndroidKeystore.generateNewAes256GcmKey(testMasterKeyAlias)
        val storedKeyset = EncryptedKeysetBinary(byteArrayOf(1, 2, 3))

        context.testWithKeysetDataStore(fileName = "stored_keyset") { keysetDataStore ->
            keysetDataStore.updateData { storedKeyset }

            val status = keysetDataStore.checkStatus(masterKeyAlias = testMasterKeyAlias)
            status shouldBe KeysetStatus.KeysetStored(storedKeyset)
        }
    }

    @Test
    fun when_keyset_is_saved_but_masterkey_is_missing_returns_keysetstoredbutmasterkeymissing() = runTest {
        AndroidKeystore.deleteKey(testMasterKeyAlias)
        val storedKeyset = EncryptedKeysetBinary(byteArrayOf(1, 2, 3))

        context.testWithKeysetDataStore(fileName = "stored_keyset_but_master_key_missing") { keysetDataStore ->
            keysetDataStore.updateData { storedKeyset }

            val status = keysetDataStore.checkStatus(masterKeyAlias = testMasterKeyAlias)
            status shouldBe KeysetStatus.KeysetStoredButMasterKeyMissing
        }
    }

    @Test
    fun when_keyset_is_not_saved_but_masterkey_is_stored_returns_keysetnotstoredbutmasterkeystored() = runTest {
        AndroidKeystore.generateNewAes256GcmKey(testMasterKeyAlias)

        context.testWithKeysetDataStore(fileName = "missing_keyset_but_master_key_stored") { keysetDataStore ->
            val status = keysetDataStore.checkStatus(masterKeyAlias = testMasterKeyAlias)
            status shouldBe KeysetStatus.KeysetNotStoredButMasterKeyStored
        }
    }

    @Test
    fun when_neither_keyset_nor_masterkey_is_stored_returns_keysetnotstored() = runTest {
        AndroidKeystore.deleteKey(testMasterKeyAlias)

        context.testWithKeysetDataStore(fileName = "missing_keyset_and_master_key") { keysetDataStore ->
            val status = keysetDataStore.checkStatus(masterKeyAlias = testMasterKeyAlias)
            status shouldBe KeysetStatus.KeysetNotStored
        }
    }
}

private suspend fun Context.testWithKeysetDataStore(
    fileName: String,
    block: suspend (KeysetDataStore) -> Unit,
) {
    val holder = KeysetDataStoreHolder(fileName)
    with(holder) {
        block(keysetDataStore)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class KeysetDataStoreHolder(fileName: String) {
    val Context.keysetDataStore by keysetDataStore(
        fileName = fileName,
        scope = CoroutineScope(UnconfinedTestDispatcher() + Job())
    )
}
