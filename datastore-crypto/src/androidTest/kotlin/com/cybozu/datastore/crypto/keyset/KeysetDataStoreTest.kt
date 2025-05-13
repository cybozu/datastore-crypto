package com.cybozu.datastore.crypto.keyset

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cybozu.datastore.crypto.util.clearDataStore
import com.cybozu.datastore.crypto.keyset.KeysetDataStore
import com.cybozu.datastore.crypto.keyset.EncryptedKeysetBinary
import com.cybozu.datastore.crypto.keyset.checkStatus
import com.cybozu.datastore.crypto.keyset.KeysetStatus
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
    fun `When there is no saved data in KeysetDataStore, returns an empty Keyset`() = runTest {
        context.testWithKeysetDataStore(fileName = "empty_keyset") { keysetDataStore ->
            keysetDataStore.data.first() shouldBe EncryptedKeysetBinary.EMPTY
        }
    }

    @Test
    fun `Can read and write Keyset`() = runTest {
        val newKeyset = EncryptedKeysetBinary(byteArrayOf(1, 2, 3))

        context.testWithKeysetDataStore(fileName = "read_write_keyset") { keysetDataStore ->
            keysetDataStore.updateData { newKeyset }
            keysetDataStore.data.first() shouldBe newKeyset
        }
    }

    @Test
    fun `When Keyset is saved and MasterKey exists, returns the saved Keyset`() = runTest {
        AndroidKeystore.generateNewAes256GcmKey(testMasterKeyAlias)
        val storedKeyset = EncryptedKeysetBinary(byteArrayOf(1, 2, 3))

        context.testWithKeysetDataStore(fileName = "stored_keyset") { keysetDataStore ->
            keysetDataStore.updateData { storedKeyset }

            val status = keysetDataStore.checkStatus(masterKeyAlias = testMasterKeyAlias)
            status shouldBe KeysetStatus.KeysetStored(storedKeyset)
        }
    }

    @Test
    fun `When Keyset is saved but MasterKey is missing, returns KeysetStoredButMasterKeyMissing`() = runTest {
        AndroidKeystore.deleteKey(testMasterKeyAlias)
        val storedKeyset = EncryptedKeysetBinary(byteArrayOf(1, 2, 3))

        context.testWithKeysetDataStore(fileName = "stored_keyset_but_master_key_missing") { keysetDataStore ->
            keysetDataStore.updateData { storedKeyset }

            val status = keysetDataStore.checkStatus(masterKeyAlias = testMasterKeyAlias)
            status shouldBe KeysetStatus.KeysetStoredButMasterKeyMissing
        }
    }

    @Test
    fun `When Keyset is not saved but MasterKey is stored, returns KeysetNotStoredButMasterKeyStored`() = runTest {
        AndroidKeystore.generateNewAes256GcmKey(testMasterKeyAlias)

        context.testWithKeysetDataStore(fileName = "missing_keyset_but_master_key_stored") { keysetDataStore ->
            val status = keysetDataStore.checkStatus(masterKeyAlias = testMasterKeyAlias)
            status shouldBe KeysetStatus.KeysetNotStoredButMasterKeyStored
        }
    }

    @Test
    fun `When neither Keyset nor MasterKey is stored, returns KeysetNotStored`() = runTest {
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
