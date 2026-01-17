package com.harrisonog.devicemediagallery.data.local.entities

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.harrisonog.devicemediagallery.domain.model.Tag

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(usageCount: Int = 0): Tag {
        return Tag(
            id = id,
            name = name,
            color = Color(color),
            usageCount = usageCount
        )
    }
}
