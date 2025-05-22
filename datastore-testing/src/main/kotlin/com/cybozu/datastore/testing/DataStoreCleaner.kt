package com.cybozu.datastore.testing

import android.content.Context
import java.io.File

public fun Context.clearDataStore() {
    File(filesDir, "/datastore").deleteRecursively()
}
