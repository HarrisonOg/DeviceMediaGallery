package com.harrisonog.devicemediagallery.data.repository

import com.harrisonog.devicemediagallery.domain.model.DuplicateGroup
import com.harrisonog.devicemediagallery.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface DuplicateRepository {
    fun getDuplicateGroups(): Flow<List<DuplicateGroup>>
    suspend fun detectDuplicates(
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Result<Int>
    suspend fun deleteDuplicates(items: List<MediaItem>): Result<Unit>
    suspend fun clearDuplicateCache(): Result<Unit>
}
