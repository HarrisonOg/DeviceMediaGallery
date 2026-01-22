package com.harrisonog.devicemediagallery.data.local.dao

import android.net.Uri
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.harrisonog.devicemediagallery.data.local.entities.AlbumMediaCrossRef
import com.harrisonog.devicemediagallery.data.local.entities.VirtualAlbumEntity
import com.harrisonog.devicemediagallery.domain.model.VirtualAlbum
import kotlinx.coroutines.flow.Flow

data class AlbumWithCount(
    val id: Long,
    val name: String,
    val coverImageUri: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val itemCount: Int
) {
    fun toDomainModel(): VirtualAlbum {
        return VirtualAlbum(
            id = id,
            name = name,
            coverImageUri = coverImageUri?.let { Uri.parse(it) },
            itemCount = itemCount,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

@Dao
interface VirtualAlbumDao {

    @Query("SELECT * FROM virtual_albums ORDER BY updatedAt DESC")
    fun getAllAlbums(): Flow<List<VirtualAlbumEntity>>

    @Query("SELECT * FROM virtual_albums WHERE id = :albumId")
    suspend fun getAlbumById(albumId: Long): VirtualAlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: VirtualAlbumEntity): Long

    @Update
    suspend fun update(album: VirtualAlbumEntity)

    @Delete
    suspend fun delete(album: VirtualAlbumEntity)

    @Query("DELETE FROM virtual_albums WHERE id = :albumId")
    suspend fun deleteById(albumId: Long)

    @Query("SELECT COUNT(*) FROM album_media_cross_ref WHERE albumId = :albumId")
    suspend fun getAlbumMediaCount(albumId: Long): Int

    @Query("""
        SELECT va.*, COUNT(amc.mediaUri) as itemCount
        FROM virtual_albums va
        LEFT JOIN album_media_cross_ref amc ON va.id = amc.albumId
        GROUP BY va.id
        ORDER BY va.updatedAt DESC
    """)
    fun getAllAlbumsWithCounts(): Flow<List<AlbumWithCount>>

    @Query("SELECT mediaUri FROM album_media_cross_ref WHERE albumId = :albumId ORDER BY addedAt DESC")
    fun getAlbumMedia(albumId: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaToAlbum(crossRefs: List<AlbumMediaCrossRef>)

    @Query("DELETE FROM album_media_cross_ref WHERE albumId = :albumId AND mediaUri IN (:mediaUris)")
    suspend fun removeMediaFromAlbum(albumId: Long, mediaUris: List<String>)
}
