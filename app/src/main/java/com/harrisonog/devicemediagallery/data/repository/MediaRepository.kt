package com.harrisonog.devicemediagallery.data.repository

import com.harrisonog.devicemediagallery.domain.model.MediaFolder
import com.harrisonog.devicemediagallery.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    /**
     * Get media items with pagination support.
     * @param folderPath Optional folder path to filter by.
     * @param limit Maximum number of items to return. Default is 100.
     * @param offset Number of items to skip. Default is 0.
     */
    fun getMediaItems(
        folderPath: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<List<MediaItem>>

    fun getFolders(): Flow<List<MediaFolder>>
    suspend fun getMediaItemByUri(uri: String): MediaItem?

    /**
     * Get media items by a list of URIs. More efficient than loading all media
     * and filtering when you only need specific items.
     * @param uris List of media URIs to fetch.
     */
    suspend fun getMediaItemsByUris(uris: List<String>): List<MediaItem>
}
