package com.cybozu.cybozux.authentication.datastore.crypto.util

import java.io.File

// :coreのテストで同様の関数が定義されているが、:datastore-cryptoはいずれ別のライブラリに切り出すため、別実装としている
internal fun File.findFileBy(fileName: String): File? = searchFileRecursively(directory = this, fileName)

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
