package com.cybozu.cybozux.authentication.datastore.crypto.keyset

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cybozu.cybozux.authentication.datastore.crypto.util.clearDataStore
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
    fun `KeysetDataStoreは保存されているデータが無いとき、空のKeysetを返す`() = runTest {
        context.testWithKeysetDataStore(fileName = "empty_keyset") { keysetDataStore ->
            keysetDataStore.data.first() shouldBe EncryptedKeysetBinary.EMPTY
        }
    }

    @Test
    fun `Keysetを読み書きできる`() = runTest {
        val newKeyset = EncryptedKeysetBinary(byteArrayOf(1, 2, 3))

        context.testWithKeysetDataStore(fileName = "read_write_keyset") { keysetDataStore ->
            keysetDataStore.updateData { newKeyset }
            keysetDataStore.data.first() shouldBe newKeyset
        }
    }

    @Test
    fun `Keysetが保存されていてMasterKeyも存在するとき、保存されたKeysetを返す`() = runTest {
        AndroidKeystore.generateNewAes256GcmKey(testMasterKeyAlias)
        val storedKeyset = EncryptedKeysetBinary(byteArrayOf(1, 2, 3))

        context.testWithKeysetDataStore(fileName = "stored_keyset") { keysetDataStore ->
            keysetDataStore.updateData { storedKeyset }

            val status = keysetDataStore.checkStatus(masterKeyAlias = testMasterKeyAlias)
            status shouldBe KeysetStatus.KeysetStored(storedKeyset)
        }
    }

    @Test
    fun `Keysetが保存されていてMasterKeyが存在しないとき、KeysetStoredButMasterKeyMissingを返す`() = runTest {
        AndroidKeystore.deleteKey(testMasterKeyAlias)
        val storedKeyset = EncryptedKeysetBinary(byteArrayOf(1, 2, 3))

        context.testWithKeysetDataStore(fileName = "stored_keyset_but_master_key_missing") { keysetDataStore ->
            keysetDataStore.updateData { storedKeyset }

            val status = keysetDataStore.checkStatus(masterKeyAlias = testMasterKeyAlias)
            status shouldBe KeysetStatus.KeysetStoredButMasterKeyMissing
        }
    }

    @Test
    fun `Keysetが保存されていなくてMasterKeyが存在するとき、KeysetMissingButMasterKeyStoredを返す`() = runTest {
        AndroidKeystore.generateNewAes256GcmKey(testMasterKeyAlias)

        context.testWithKeysetDataStore(fileName = "missing_keyset_but_master_key_stored") { keysetDataStore ->
            val status = keysetDataStore.checkStatus(masterKeyAlias = testMasterKeyAlias)
            status shouldBe KeysetStatus.KeysetNotStoredButMasterKeyStored
        }
    }

    @Test
    fun `Keysetが保存されていなくてMasterKeyも存在しないとき、KeysetNotStoredを返す`() = runTest {
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
