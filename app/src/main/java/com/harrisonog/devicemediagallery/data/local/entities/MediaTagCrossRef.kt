package com.harrisonog.devicemediagallery.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "media_tag_cross_ref",
    primaryKeys = ["mediaUri", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class MediaTagCrossRef(
    val mediaUri: String,
    val tagId: Long
)
