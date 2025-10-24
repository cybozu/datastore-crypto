# DataStore Crypto

DataStore Crypto is a Kotlin library for Android that provides encrypted [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) implementations. It is designed as a response to the Feature Request for encrypted DataStore support in DataStore (see: [Feature Request: Support for encryption in Datastore](https://issuetracker.google.com/issues/167697691)).

It enables secure storage of key-value and typed data using DataStore, with transparent encryption and decryption of data at rest.

## Features

### Encrypted DataStore Support
DataStore Crypto provides both encrypted Preferences DataStore and Proto DataStore implementations. This allows you to securely store key-value pairs and typed data with the same API as AndroidX DataStore.

### Secure Key Management
The library utilizes Android Keystore for secure key management, ensuring that encryption keys are protected by the device's hardware-backed security features.

### No AndroidX Security Crypto Dependency
DataStore Crypto does not use [AndroidX Security Crypto](https://developer.android.com/reference/kotlin/androidx/security/crypto/package-summary), which is now deprecated. Instead, it provides secure encryption using [Tink](https://github.com/tink-crypto/tink-java), the same cryptographic library used internally by Security Crypto.

### Easy Migration and Compatibility
The API is designed to be simple and compatible with AndroidX DataStore, making it easy to migrate existing code or adopt encrypted storage for new projects.

### Use Cases
DataStore Crypto is suitable for storing sensitive information such as tokens, credentials, and user settings that require confidentiality and integrity.

## Usage

### Requirements
Android 8.1 (API level 27) or higher

### Downloads
![Maven Central Version](https://img.shields.io/maven-central/v/com.cybozu.datastore.crypto/datastore-crypto)

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    // Preferences DataStore
    implementation("com.cybozu.datastore.crypto:datastore-crypto-preferences:<version>")

    // or
    // Proto DataStore
    implementation("com.cybozu.datastore.crypto:datastore-crypto:<version>")
}
```

### Basic Setup
DataStore Crypto provides drop-in replacements for AndroidX DataStore with automatic encryption:

```kotlin
// Preferences DataStore
val Context.encryptedUserPrefs by encryptedPreferencesDataStore(
    name = "user_preferences",
    masterKeyAlias = "preferences_master_key"
)

// Proto DataStore
val Context.encryptedUserData by encryptedDataStore(
    fileName = "user_data.pb",
    serializer = UserDataSerializer,
    masterKeyAlias = "proto_master_key"
)
```

### Automatic Encryption
All data is automatically encrypted when written and decrypted when read. 
You use the same DataStore API, but your data is securely stored:

```kotlin
// Writing data - automatically encrypted before saving to disk
encryptedUserPrefs.edit { preferences ->
    preferences[stringPreferencesKey("api_token")] = "secret_token_123"
}

// Reading data - automatically decrypted when loaded
val token = encryptedUserPrefs.data.first()[stringPreferencesKey("api_token")]
// token = "secret_token_123" (decrypted value)

// The actual file on disk contains encrypted binary data
val prefsFile = File(context.filesDir, "datastore/user_preferences.preferences_pb")
// prefsFile.readText() would show encrypted binary data, not "secret_token_123"
```

### Master Key and Alias
The master key serves as the ultimate encryption key that DataStore Crypto uses to encrypt and decrypt your data. 
It is stored in Android Keystore, an OS-controlled secure area that applications cannot directly access. 
Cryptographic operations using the master key are not handled directly within the app, but are delegated to and processed by the Android OS.

The `masterKeyAlias` parameter serves as a unique identifier for each master key in Android Keystore.
