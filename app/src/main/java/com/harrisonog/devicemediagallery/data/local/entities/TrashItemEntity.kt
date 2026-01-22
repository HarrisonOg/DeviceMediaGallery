package com.harrisonog.devicemediagallery.data.local.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.harrisonog.devicemediagallery.domain.model.TrashItem
import java.util.concurrent.TimeUnit

@Entity(
    tableName = "trash_items",
    indices = [
        Index(value = ["autoDeleteAt"]),
        Index(value = ["originalUri"])
    ]
)
data class TrashItemEntity(
    @PrimaryKey
    val originalUri: String,
    val trashUri: String,
    val originalPath: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val deletedAt: Long = System.currentTimeMillis(),
    val autoDeleteAt: Long = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)
) {
    fun toDomainModel(): TrashItem {
        val now = System.currentTimeMillis()
        val daysRemaining = TimeUnit.MILLISECONDS.toDays(autoDeleteAt - now).toInt()
        return TrashItem(
            originalUri = Uri.parse(originalUri),
            trashUri = Uri.parse(trashUri),
            fileName = fileName,
            mimeType = mimeType,
            size = size,
            deletedAt = deletedAt,
            daysUntilDeletion = maxOf(0, daysRemaining)
        )
    }
}
