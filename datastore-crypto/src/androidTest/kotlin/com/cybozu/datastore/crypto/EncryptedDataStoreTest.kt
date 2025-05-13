package com.cybozu.datastore.crypto

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.test.core.app.ApplicationProvider
import com.cybozu.datastore.crypto.util.clearDataStore
import com.cybozu.datastore.crypto.util.findFileBy
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldNotContainIgnoringCase
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class EncryptedDataStoreTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        context.clearDataStore()
    }

    @Test
    fun `データの読み書きができる`() = runTest {
        context.testWithDataStore(fileName = "read_write_test", defaultValue = "default") { testDataStore ->
            testDataStore.data.first() shouldBe "default"
            testDataStore.updateData { "new data" }
            testDataStore.data.first() shouldBe "new data"
        }
    }

    @Test
    fun `ファイルに書き込む内容は直接データを解釈できる形式ではない`() = runTest {
        context.testWithDataStore(fileName = "my_password_file") { testDataStore ->
            testDataStore.updateData { "password1234" }
            val passwordFile = context.filesDir.findFileBy("my_password_file")
            passwordFile?.readText()
                .shouldNotContainIgnoringCase("password1234")
                .shouldNotBeBlank()
        }
    }

    @Test
    fun `同一のmasterKeyAliasを指定した2つのDataStoreが同時に存在しても、暗号化できる`() {
        val sharedMasterKeyAlias = "shared_master_key"
        parallelMultiEncryptedDataStoreTest(
            fileName1 = "same_master_key_datastore1",
            masterKeyAlias1 = sharedMasterKeyAlias,
            fileName2 = "same_master_key_datastore2",
            masterKeyAlias2 = sharedMasterKeyAlias
        )
    }

    @Test
    fun `別々のmasterKeyAliasを指定した2つのDataStoreが同時に存在しても、暗号化できる`() {
        parallelMultiEncryptedDataStoreTest(
            fileName1 = "other_master_key_datastore1",
            masterKeyAlias1 = "master_key1",
            fileName2 = "other_master_key_datastore2",
            masterKeyAlias2 = "master_key2"
        )
    }

    private fun parallelMultiEncryptedDataStoreTest(
        fileName1: String,
        masterKeyAlias1: String,
        fileName2: String,
        masterKeyAlias2: String,
    ) = runTest {
        val job1 = launch {
            context.testWithDataStore(fileName = fileName1, masterKeyAlias = masterKeyAlias1) { testDataStore1 ->
                testDataStore1.updateData { "data1" }
                testDataStore1.data.first() shouldBe "data1"

                val storedFile = context.filesDir.findFileBy(fileName1)
                storedFile?.readText()
                    .shouldNotContainIgnoringCase("data1")
                    .shouldNotBeBlank()
            }
        }

        val job2 = launch {
            context.testWithDataStore(fileName = fileName2, masterKeyAlias = masterKeyAlias2) { testDataStore2 ->
                testDataStore2.updateData { "data2" }
                testDataStore2.data.first() shouldBe "data2"

                val storedFile = context.filesDir.findFileBy(fileName2)
                storedFile?.readText()
                    .shouldNotContainIgnoringCase("data2")
                    .shouldNotBeBlank()
            }
        }

        joinAll(job1, job2)
    }

    @Test
    fun `データを永続的に保存できている`() = runTest {
        val dataStore1Job = Job()
        context.testWithDataStore(fileName = "same_datastore", dataStoreJob = dataStore1Job) { testDataStore1 ->
            testDataStore1.updateData { "Hello world" }
        }

        // アプリプロセスの終了を擬似的に再現
        // 現在ファイルを開いているDataStoreを終了させる
        dataStore1Job.complete()

        context.testWithDataStore(fileName = "same_datastore") { testDataStore2 ->
            testDataStore2.data.first() shouldBe "Hello world"
        }
    }
}

private suspend fun Context.testWithDataStore(
    fileName: String,
    masterKeyAlias: String = "${fileName}_master_key",
    defaultValue: String = "default",
    dataStoreJob: Job = Job(),
    block: suspend (DataStore<String>) -> Unit,
) {
    val holder = DataStoreHolder(fileName, masterKeyAlias, defaultValue, dataStoreJob)
    with(holder) {
        block(testDataStore)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class DataStoreHolder(
    fileName: String,
    masterKeyAlias: String,
    defaultValue: String,
    dataStoreJob: Job,
) {
    val Context.testDataStore by encryptedDataStore(
        fileName = fileName,
        serializer = StringSerializer(defaultValue),
        masterKeyAlias = masterKeyAlias,
        scope = CoroutineScope(UnconfinedTestDispatcher() + dataStoreJob)
    )
}

private class StringSerializer(override val defaultValue: String) : Serializer<String> {
    override suspend fun readFrom(input: InputStream): String = input.readBytes().decodeToString()

    override suspend fun writeTo(t: String, output: OutputStream) {
        output.write(t.encodeToByteArray())
    }
}
