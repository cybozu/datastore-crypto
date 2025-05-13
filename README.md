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

## Getting Started
Add this library to your project and use the provided APIs to create encrypted DataStore instances for your application data.

TODO: code sample
