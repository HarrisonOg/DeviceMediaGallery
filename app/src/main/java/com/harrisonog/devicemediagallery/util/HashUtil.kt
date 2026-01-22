package com.harrisonog.devicemediagallery.util

import android.content.ContentResolver
import android.net.Uri
import java.io.InputStream
import java.security.MessageDigest

object HashUtil {
    private const val BUFFER_SIZE = 8 * 1024 // 8KB buffer for memory efficiency

    fun calculateMd5(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(BUFFER_SIZE)
        inputStream.use { stream ->
            var bytesRead = stream.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = stream.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun calculateMd5FromUri(contentResolver: ContentResolver, uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                calculateMd5(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }
}
