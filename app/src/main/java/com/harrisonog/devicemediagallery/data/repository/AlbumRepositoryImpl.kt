package com.harrisonog.devicemediagallery.data.repository

import com.harrisonog.devicemediagallery.data.local.dao.VirtualAlbumDao
import com.harrisonog.devicemediagallery.data.local.entities.AlbumMediaCrossRef
import com.harrisonog.devicemediagallery.data.local.entities.VirtualAlbumEntity
import com.harrisonog.devicemediagallery.domain.model.VirtualAlbum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepositoryImpl @Inject constructor(
    private val virtualAlbumDao: VirtualAlbumDao
) : AlbumRepository {

    override fun getVirtualAlbums(): Flow<List<VirtualAlbum>> {
        return virtualAlbumDao.getAllAlbumsWithCounts()
            .map { albumsWithCounts ->
                albumsWithCounts.map { it.toDomainModel() }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getAlbumById(id: Long): VirtualAlbum? {
        val entity = virtualAlbumDao.getAlbumById(id) ?: return null
        val count = virtualAlbumDao.getAlbumMediaCount(id)
        return entity.toDomainModel(count)
    }

    override suspend fun createAlbum(name: String): Long {
        val entity = VirtualAlbumEntity(name = name)
        return virtualAlbumDao.insert(entity)
    }

    override suspend fun updateAlbum(album: VirtualAlbum) {
        val entity = VirtualAlbumEntity(
            id = album.id,
            name = album.name,
            coverImageUri = album.coverImageUri?.toString(),
            createdAt = album.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        virtualAlbumDao.update(entity)
    }

    override suspend fun deleteAlbum(id: Long) {
        virtualAlbumDao.deleteById(id)
    }

    override suspend fun addMediaToAlbum(albumId: Long, mediaUris: List<String>) {
        val crossRefs = mediaUris.map { uri ->
            AlbumMediaCrossRef(albumId = albumId, mediaUri = uri)
        }
        virtualAlbumDao.insertMediaToAlbum(crossRefs)

        // Update album's updatedAt timestamp
        virtualAlbumDao.getAlbumById(albumId)?.let { album ->
            virtualAlbumDao.update(album.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun removeMediaFromAlbum(albumId: Long, mediaUris: List<String>) {
        virtualAlbumDao.removeMediaFromAlbum(albumId, mediaUris)

        // Update album's updatedAt timestamp
        virtualAlbumDao.getAlbumById(albumId)?.let { album ->
            virtualAlbumDao.update(album.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override fun getAlbumMediaUris(albumId: Long): Flow<List<String>> {
        return virtualAlbumDao.getAlbumMedia(albumId)
            .flowOn(Dispatchers.IO)
    }
}
