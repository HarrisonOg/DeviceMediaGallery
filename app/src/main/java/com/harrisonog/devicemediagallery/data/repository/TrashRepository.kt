package com.harrisonog.devicemediagallery.data.repository

import com.harrisonog.devicemediagallery.domain.model.MediaItem
import com.harrisonog.devicemediagallery.domain.model.TrashItem
import kotlinx.coroutines.flow.Flow

interface TrashRepository {
    fun getTrashItems(): Flow<List<TrashItem>>
    suspend fun getTrashCount(): Int
    suspend fun moveToTrash(mediaItems: List<MediaItem>): Result<Unit>
    suspend fun restoreFromTrash(items: List<TrashItem>): Result<Unit>
    suspend fun permanentlyDelete(items: List<TrashItem>): Result<Unit>
    suspend fun emptyTrash(): Result<Unit>
    suspend fun deleteExpiredItems()
}
