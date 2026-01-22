package com.harrisonog.devicemediagallery.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "duplicate_group_media_cross_ref",
    primaryKeys = ["groupHash", "mediaUri"],
    foreignKeys = [
        ForeignKey(
            entity = DuplicateGroupEntity::class,
            parentColumns = ["groupHash"],
            childColumns = ["groupHash"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupHash")]
)
data class DuplicateGroupMediaCrossRef(
    val groupHash: String,
    val mediaUri: String
)
