package com.harrisonog.devicemediagallery.data.repository

import com.harrisonog.devicemediagallery.domain.model.MediaFolder
import com.harrisonog.devicemediagallery.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getMediaItems(folderPath: String? = null): Flow<List<MediaItem>>
    fun getFolders(): Flow<List<MediaFolder>>
    suspend fun getMediaItemByUri(uri: String): MediaItem?
}
