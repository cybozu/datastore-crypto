package com.cybozu.cybozux.authentication.datastore.crypto.util

import android.content.Context
import java.io.File

internal fun Context.clearDataStore() {
    File(filesDir, "/datastore").deleteRecursively()
}
