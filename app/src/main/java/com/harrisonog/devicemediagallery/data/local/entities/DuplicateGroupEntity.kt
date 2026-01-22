package com.harrisonog.devicemediagallery.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "duplicate_groups")
data class DuplicateGroupEntity(
    @PrimaryKey
    val groupHash: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val groupSize: Int
)
