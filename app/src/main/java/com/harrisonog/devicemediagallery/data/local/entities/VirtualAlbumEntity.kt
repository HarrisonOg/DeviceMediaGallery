package com.harrisonog.devicemediagallery.data.local.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.harrisonog.devicemediagallery.domain.model.VirtualAlbum

@Entity(
    tableName = "virtual_albums",
    indices = [Index(value = ["updatedAt"])]
)
data class VirtualAlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val coverImageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(itemCount: Int): VirtualAlbum {
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
