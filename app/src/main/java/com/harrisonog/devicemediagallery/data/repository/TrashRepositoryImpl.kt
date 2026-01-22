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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrashRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver,
    private val trashItemDao: TrashItemDao
) : TrashRepository {

    private val trashDir: File by lazy {
        File(context.filesDir, "trash").also { it.mkdirs() }
    }

    override fun getTrashItems(): Flow<List<TrashItem>> {
        return trashItemDao.getAllTrashItems().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getTrashCount(): Int {
        return trashItemDao.getTrashCount()
    }

    override suspend fun moveToTrash(mediaItems: List<MediaItem>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (item in mediaItems) {
                // Copy file to trash folder
                val trashFileName = "${UUID.randomUUID()}_${item.fileName}"
                val trashFile = File(trashDir, trashFileName)

                contentResolver.openInputStream(item.uri)?.use { input ->
                    FileOutputStream(trashFile).use { output ->
                        input.copyTo(output)
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
                val trashFile = File(item.trashUri.path ?: continue)
                if (!trashFile.exists()) {
                    trashItemDao.deleteById(item.originalUri.toString())
                    continue
                }

                // Get the original directory
                val originalPath = trashItemDao.getById(item.originalUri.toString())?.originalPath
                if (originalPath == null) {
                    trashItemDao.deleteById(item.originalUri.toString())
                    continue
                }

                val originalFile = File(originalPath)
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
                val trashFile = File(item.trashUri.path ?: continue)
                trashFile.delete()
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

        for (entity in expiredItems) {
            val trashFile = File(Uri.parse(entity.trashUri).path ?: continue)
            trashFile.delete()
        }

        trashItemDao.deleteExpired(currentTime)
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
