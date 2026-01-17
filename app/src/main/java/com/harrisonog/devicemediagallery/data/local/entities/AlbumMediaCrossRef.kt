package com.harrisonog.devicemediagallery.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "album_media_cross_ref",
    primaryKeys = ["albumId", "mediaUri"],
    foreignKeys = [
        ForeignKey(
            entity = VirtualAlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("albumId")]
)
data class AlbumMediaCrossRef(
    val albumId: Long,
    val mediaUri: String,
    val addedAt: Long = System.currentTimeMillis()
)
