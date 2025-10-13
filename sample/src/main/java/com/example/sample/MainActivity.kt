package com.example.sample

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cybozu.datastore.crypto.preferences.encryptedPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.example.sample.ui.theme.DataStoreCryptoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Example of using Preferences DataStore */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_by_datastore")
val TEXT_KEY = stringPreferencesKey("saved_text")

suspend fun saveToDataStore(saveText: String, context: Context): Unit = withContext(Dispatchers.IO) {
    context.dataStore.edit { preferences ->
        preferences[TEXT_KEY] = saveText
    }
}

suspend fun readFromDataStore(context: Context): String = withContext(Dispatchers.IO) {
    context.dataStore.data
        .map { preferences -> preferences[TEXT_KEY] ?: "" }
        .first()
}


/** Example of using Preferences DataStore */
val TEXT_KEY_CRYPTO = stringPreferencesKey("saved_text_crypto")
val Context.encryptedUserPrefs by encryptedPreferencesDataStore(
    name = "setting_by_datastore-crypto",
    masterKeyAlias = "setting_by_datastore-crypto_key"
)


suspend fun saveToEncryptedDataStore(saveText: String, context: Context): Unit = withContext(Dispatchers.IO) {
    context.encryptedUserPrefs.edit { prefs ->
        prefs[TEXT_KEY_CRYPTO] = saveText
    }
}

suspend fun readFromEncryptedDataStore(context: Context): String = withContext(Dispatchers.IO) {
    context.encryptedUserPrefs.data
        .map { preferences -> preferences[TEXT_KEY_CRYPTO] ?: "" }
        .first()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DataStoreCryptoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SampleScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun SampleScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    var encryptedInputText by remember { mutableStateOf("") }
    var encryptedResultText by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SampleContent(
            modifier = Modifier,
            description = "Preferences DataStore",
            inputText = inputText,
            resultText = resultText,
            onInputTextChange = { inputText = it },
            onWriteClick = {
                scope.launch {
                    saveToDataStore(inputText, context)
                }
            },
            onReadClick = {
                scope.launch {
                    resultText = readFromDataStore(context)
                }
            }
        )

        SampleContent(
            modifier = Modifier,
            description = "Encrypted Preferences DataStore",
            inputText = encryptedInputText,
            resultText = encryptedResultText,
            onInputTextChange = { encryptedInputText = it },
            onWriteClick = {
                scope.launch {
                    saveToEncryptedDataStore(encryptedInputText, context)
                }
            },
            onReadClick = {
                scope.launch {
                    encryptedResultText = readFromEncryptedDataStore(context)
                }
            }
        )
    }
}

@Composable
fun SampleContent(
    modifier: Modifier = Modifier,
    description: String,
    inputText: String,
    resultText: String,
    onInputTextChange: (String) -> Unit,
    onWriteClick: () -> Unit,
    onReadClick: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = description)
        TextField(
            value = inputText,
            onValueChange = onInputTextChange,
            modifier = Modifier
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onWriteClick) {
                Text(text = "write")
            }
            Button(onClick = onReadClick) {
                Text(text = "read")
            }
        }
        Text(
            text = "result: $resultText",
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DataStoreCryptoTheme {
        SampleScreen()
    }
}