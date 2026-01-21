package com.harrisonog.devicemediagallery.data.repository

import com.harrisonog.devicemediagallery.domain.model.VirtualAlbum
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    fun getVirtualAlbums(): Flow<List<VirtualAlbum>>
    suspend fun getAlbumById(id: Long): VirtualAlbum?
    suspend fun createAlbum(name: String): Long
    suspend fun updateAlbum(album: VirtualAlbum)
    suspend fun deleteAlbum(id: Long)
    suspend fun addMediaToAlbum(albumId: Long, mediaUris: List<String>)
    suspend fun removeMediaFromAlbum(albumId: Long, mediaUris: List<String>)
    fun getAlbumMediaUris(albumId: Long): Flow<List<String>>
}
