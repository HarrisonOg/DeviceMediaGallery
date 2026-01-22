package com.harrisonog.devicemediagallery.data.repository

import com.harrisonog.devicemediagallery.domain.model.MediaItem
import com.harrisonog.devicemediagallery.domain.model.TrashItem
import kotlinx.coroutines.flow.Flow

/**
 * Callback for reporting progress during file operations.
 * @param bytesWritten The number of bytes written so far.
 * @param totalBytes The total number of bytes to write.
 */
typealias ProgressCallback = (bytesWritten: Long, totalBytes: Long) -> Unit

interface TrashRepository {
    fun getTrashItems(): Flow<List<TrashItem>>
    suspend fun getTrashCount(): Int
    suspend fun moveToTrash(
        mediaItems: List<MediaItem>,
        onProgress: ProgressCallback? = null
    ): Result<Unit>
    suspend fun restoreFromTrash(items: List<TrashItem>): Result<Unit>
    suspend fun permanentlyDelete(items: List<TrashItem>): Result<Unit>
    suspend fun emptyTrash(): Result<Unit>
    suspend fun deleteExpiredItems()
}
