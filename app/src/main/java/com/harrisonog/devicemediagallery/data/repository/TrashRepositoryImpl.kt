package com.harrisonog.devicemediagallery.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import com.harrisonog.devicemediagallery.data.local.dao.TrashItemDao
import com.harrisonog.devicemediagallery.data.local.entities.TrashItemEntity
import com.harrisonog.devicemediagallery.domain.model.MediaItem
import com.harrisonog.devicemediagallery.domain.model.TrashItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import android.util.Log
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TrashRepository"

@Singleton
class TrashRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver,
    private val trashItemDao: TrashItemDao
) : TrashRepository {

    private val trashDir: File by lazy {
        File(context.filesDir, "trash").also { it.mkdirs() }
    }

    /**
     * Validates that a URI points to a file within the trash directory.
     * Prevents path traversal attacks by ensuring the canonical path is within trashDir.
     * @return The validated File if it's within trashDir, null otherwise.
     */
    private fun getValidatedTrashFile(trashUri: Uri): File? {
        return try {
            // Extract filename from URI - should be just the filename, not a path
            val filename = trashUri.lastPathSegment ?: return null

            // Reject if filename contains path separators (prevent "../" attacks)
            if (filename.contains("/") || filename.contains("\\")) {
                Log.w(TAG, "Invalid filename contains path separator: $filename")
                return null
            }

            // Construct file using trashDir as base (trusted) + filename (validated)
            val file = File(trashDir, filename)

            // Double-check canonical path is within trash directory
            val canonicalTrashDir = trashDir.canonicalFile
            val canonicalFile = file.canonicalFile

            if (!canonicalFile.startsWith(canonicalTrashDir)) {
                Log.w(TAG, "Path traversal attempt detected: $filename")
                return null
            }

            file
        } catch (e: Exception) {
            Log.e(TAG, "Error validating trash file: $trashUri", e)
            null
        }
    }

    /**
     * Validates that a restoration path is safe for writing files.
     * Ensures the path is within external storage and not in restricted directories.
     * @return The validated File if it's a safe restoration path, null otherwise.
     */
    private fun getValidatedRestorePath(originalPath: String?): File? {
        if (originalPath.isNullOrBlank()) return null

        return try {
            val file = File(originalPath)
            val canonicalFile = file.canonicalFile
            val canonicalPath = canonicalFile.absolutePath

            // Must be under /storage/emulated/0/ or similar external storage
            val externalStoragePath = Environment.getExternalStorageDirectory().canonicalPath

            if (!canonicalPath.startsWith(externalStoragePath)) {
                Log.w(TAG, "Invalid restore path outside external storage: $originalPath")
                return null
            }

            // Reject restoration to sensitive directories
            val restrictedPaths = listOf(
                "/Android/data/",
                "/Android/obb/"
            )

            if (restrictedPaths.any { canonicalPath.contains(it) }) {
                Log.w(TAG, "Cannot restore to restricted path: $originalPath")
                return null
            }

            file
        } catch (e: Exception) {
            Log.e(TAG, "Error validating restore path: $originalPath", e)
            null
        }
    }

    override fun getTrashItems(): Flow<List<TrashItem>> {
        return trashItemDao.getAllTrashItems().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getTrashCount(): Int {
        return trashItemDao.getTrashCount()
    }

    override suspend fun moveToTrash(
        mediaItems: List<MediaItem>,
        onProgress: ProgressCallback?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Calculate total bytes for progress reporting
            val totalBytes = mediaItems.sumOf { it.size }
            var bytesWritten = 0L

            for (item in mediaItems) {
                coroutineContext.ensureActive()

                // Copy file to trash folder
                val trashFileName = "${UUID.randomUUID()}_${item.fileName}"
                val trashFile = File(trashDir, trashFileName)

                // Use larger buffer for videos (64KB), smaller for images (8KB)
                val bufferSize = if (item.mimeType.startsWith("video/")) 65536 else 8192

                contentResolver.openInputStream(item.uri)?.use { input ->
                    FileOutputStream(trashFile).use { output ->
                        val buffer = ByteArray(bufferSize)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } >= 0) {
                            coroutineContext.ensureActive()
                            output.write(buffer, 0, bytesRead)
                            bytesWritten += bytesRead
                            onProgress?.invoke(bytesWritten, totalBytes)
                        }
                    }
                } ?: return@withContext Result.failure(Exception("Could not open file: ${item.fileName}"))

                // Create trash entity
                val entity = TrashItemEntity(
                    originalUri = item.uri.toString(),
                    trashUri = trashFile.toUri().toString(),
                    originalPath = item.path,
                    fileName = item.fileName,
                    mimeType = item.mimeType,
                    size = item.size
                )
                trashItemDao.insert(entity)

                // Delete from MediaStore
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // On Android 11+, we need to use MediaStore.createDeleteRequest
                        // For now, we'll try direct deletion which works for files owned by the app
                        contentResolver.delete(item.uri, null, null)
                    } else {
                        contentResolver.delete(item.uri, null, null)
                    }
                } catch (e: SecurityException) {
                    // If we can't delete from MediaStore due to permissions,
                    // the file is still in trash. Log this but don't fail.
                    // The original file will remain but user can manage it from trash.
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreFromTrash(items: List<TrashItem>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (item in items) {
                val trashFile = getValidatedTrashFile(item.trashUri) ?: continue
                if (!trashFile.exists()) {
                    trashItemDao.deleteById(item.originalUri.toString())
                    continue
                }

                // Get and validate the original path
                val originalPath = trashItemDao.getById(item.originalUri.toString())?.originalPath
                val originalFile = getValidatedRestorePath(originalPath)
                if (originalFile == null) {
                    Log.w(TAG, "Invalid restore path, skipping: $originalPath")
                    trashItemDao.deleteById(item.originalUri.toString())
                    continue
                }

                val originalDir = originalFile.parentFile

                // If original directory doesn't exist, try to create it
                originalDir?.mkdirs()

                // Determine destination file (handle duplicates)
                val destinationFile = if (originalFile.exists()) {
                    // File already exists at original location, generate new name
                    generateUniqueFile(originalDir, item.fileName)
                } else {
                    originalFile
                }

                // Copy from trash back to original location
                trashFile.copyTo(destinationFile, overwrite = false)

                // Scan the file to add it back to MediaStore
                scanFile(destinationFile)

                // Delete from trash
                trashFile.delete()
                trashItemDao.deleteById(item.originalUri.toString())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun permanentlyDelete(items: List<TrashItem>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (item in items) {
                val trashFile = getValidatedTrashFile(item.trashUri)
                trashFile?.delete()
                trashItemDao.deleteById(item.originalUri.toString())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun emptyTrash(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete all files in trash directory
            trashDir.listFiles()?.forEach { it.delete() }
            // Delete all database entries
            trashItemDao.deleteAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExpiredItems(): Unit = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val expiredItems = trashItemDao.getExpiredItems(currentTime)

        // Delete database entries first (atomic operation) to avoid orphaned records
        trashItemDao.deleteExpired(currentTime)

        // Then delete files - if this fails, data is still consistent
        // Files can be cleaned up later since DB entries are already gone
        for (entity in expiredItems) {
            val trashFile = getValidatedTrashFile(Uri.parse(entity.trashUri))
            if (trashFile != null) {
                if (!trashFile.delete()) {
                    Log.w(TAG, "Failed to delete expired trash file: ${entity.trashUri}")
                }
            }
        }
        Unit
    }

    private fun generateUniqueFile(directory: File?, fileName: String): File {
        val dir = directory ?: trashDir
        var file = File(dir, fileName)
        var counter = 1

        val baseName = fileName.substringBeforeLast(".")
        val extension = fileName.substringAfterLast(".", "")

        while (file.exists()) {
            val newName = if (extension.isNotEmpty()) {
                "${baseName}_$counter.$extension"
            } else {
                "${baseName}_$counter"
            }
            file = File(dir, newName)
            counter++
        }

        return file
    }

    private fun scanFile(file: File) {
        // Use MediaScannerConnection to add file back to MediaStore
        android.media.MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            null,
            null
        )
    }
}
