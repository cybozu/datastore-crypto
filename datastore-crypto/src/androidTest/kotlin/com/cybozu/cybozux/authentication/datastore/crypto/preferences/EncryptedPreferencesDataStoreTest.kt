package com.cybozu.cybozux.authentication.datastore.crypto.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.cybozu.cybozux.authentication.datastore.crypto.util.clearDataStore
import com.cybozu.cybozux.authentication.datastore.crypto.util.findFileBy
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldNotContainIgnoringCase
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

class EncryptedPreferencesDataStoreTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val intKey = intPreferencesKey("int_key")
    private val doubleKey = doublePreferencesKey("double_key")
    private val stringKey = stringPreferencesKey("string_key")
    private val booleanKey = booleanPreferencesKey("boolean_key")
    private val floatKey = floatPreferencesKey("float_key")
    private val longKey = longPreferencesKey("long_key")
    private val stringSetKey = stringSetPreferencesKey("string_set_key")
    private val byteArrayKey = byteArrayPreferencesKey("byte_array_key")

    private val allTypePreferences = listOf(
        intKey pair 19940117,
        doubleKey pair 1994.0117,
        stringKey pair "Hello world",
        booleanKey pair true,
        floatKey pair 1994.0117f,
        longKey pair 19940117L,
        stringSetKey pair setOf("Hello world"),
        byteArrayKey pair byteArrayOf(1, 9, 9, 4, 0, 1, 1, 7)
    )

    @Before
    fun setUp() {
        context.clearDataStore()
    }

    @Test
    fun `データの読み書きができる`() = runTest {
        context.testWithPreferencesDataStore(name = "read_write_preferences") { testDataStore ->
            allTypePreferences.forEach { testPreferenceType ->
                val initialData = testDataStore.data.first()
                initialData[testPreferenceType.key].shouldBeNull()

                testDataStore.edit { it.set(testPreferenceType) }

                val modifiedData = testDataStore.data.first()
                modifiedData[testPreferenceType.key] shouldBe testPreferenceType.value
            }
        }
    }

    @Test
    fun `ファイルに書き込む内容は直接データを解釈できる形式ではない`() = runTest {
        val secretName = "secret_preferences"
        context.testWithPreferencesDataStore(name = secretName) { testDataStore ->
            allTypePreferences.forEach { testPreferenceType ->
                testDataStore.edit { it.set(testPreferenceType) }

                val secretFile = context.filesDir.findFileBy(secretName)
                secretFile?.readText()
                    .shouldNotContainIgnoringCase(testPreferenceType.value.toString())
                    .shouldNotBeBlank()
            }
        }
    }

    @Test
    fun `同一のmasterKeyAliasを指定した2つのDataStoreが同時に存在しても、暗号化できる`() {
        val sharedMasterKeyAlias = "shared_master_key"
        parallelMultiEncryptedDataStoreTest(
            name1 = "same_master_key_preference1",
            masterKeyAlias1 = sharedMasterKeyAlias,
            name2 = "same_master_key_preference2",
            masterKeyAlias2 = sharedMasterKeyAlias
        )
    }

    @Test
    fun `別々のmasterKeyAliasを指定した2つのDataStoreが同時に存在しても、暗号化できる`() {
        parallelMultiEncryptedDataStoreTest(
            name1 = "other_master_key_preference1",
            masterKeyAlias1 = "master_key1",
            name2 = "other_master_key_preference2",
            masterKeyAlias2 = "master_key2"
        )
    }

    private fun parallelMultiEncryptedDataStoreTest(
        name1: String,
        masterKeyAlias1: String,
        name2: String,
        masterKeyAlias2: String,
    ) = runTest {
        val job1 = launch {
            context.testWithPreferencesDataStore(name = name1, masterKeyAlias = masterKeyAlias1) { testDataStore1 ->
                testDataStore1.edit { it[stringKey] = "data1" }

                val latestData = testDataStore1.data.first()
                latestData[stringKey] shouldBe "data1"

                val storedFile = context.filesDir.findFileBy(name1)
                storedFile?.readText()
                    .shouldNotContainIgnoringCase("data1")
                    .shouldNotBeBlank()
            }
        }

        val job2 = launch {
            context.testWithPreferencesDataStore(name = name2, masterKeyAlias = masterKeyAlias2) { testDataStore2 ->
                testDataStore2.edit { it[stringKey] = "data2" }

                val latestData = testDataStore2.data.first()
                latestData[stringKey] shouldBe "data2"

                val storedFile = context.filesDir.findFileBy(name2)
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

        context.testWithPreferencesDataStore(name = "same_preferences_datastore", dataStoreJob = dataStore1Job) { testDataStore1 ->
            testDataStore1.edit { it[stringKey] = "Hello world" }
        }

        // アプリプロセスの終了を擬似的に再現
        // 現在ファイルを開いているDataStoreを終了させる
        dataStore1Job.complete()

        context.testWithPreferencesDataStore(name = "same_preferences_datastore") { testDataStore2 ->
            testDataStore2.data.first()[stringKey] shouldBe "Hello world"
        }
    }
}

private suspend fun Context.testWithPreferencesDataStore(
    name: String,
    masterKeyAlias: String = "${name}_master_key",
    dataStoreJob: Job = Job(),
    block: suspend (DataStore<Preferences>) -> Unit,
) {
    val holder = PreferencesDataStoreHolder(name, masterKeyAlias, dataStoreJob)
    with(holder) {
        block(testDataStore)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class PreferencesDataStoreHolder(
    name: String,
    masterKeyAlias: String,
    dataStoreJob: Job,
) {
    val Context.testDataStore by encryptedPreferencesDataStore(
        name = name,
        masterKeyAlias = masterKeyAlias,
        scope = CoroutineScope(UnconfinedTestDispatcher() + dataStoreJob)
    )
}

private data class PreferencesPair<T>(
    val key: Preferences.Key<T>,
    val value: T,
)

private infix fun <T> Preferences.Key<T>.pair(value: T): PreferencesPair<T> = PreferencesPair(this, value)

private fun <T> MutablePreferences.set(pair: PreferencesPair<T>) {
    this[pair.key] = pair.value
}
