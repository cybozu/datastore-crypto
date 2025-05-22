package com.cybozu.datastore.testing

import java.io.File

public fun File.findFileBy(fileName: String): File? = searchFileRecursively(directory = this, fileName)

private fun searchFileRecursively(directory: File, fileName: String): File? {
    directory.listFiles()?.forEach { file ->
        if (file.isDirectory) {
            searchFileRecursively(file, fileName)?.let { return it }
        } else if (file.name.contains(fileName)) {
            return file
        }
    }
    return null
}
