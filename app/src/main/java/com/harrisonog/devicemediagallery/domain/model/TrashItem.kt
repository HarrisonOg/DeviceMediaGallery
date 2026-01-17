package com.harrisonog.devicemediagallery.domain.model

import android.net.Uri

data class TrashItem(
    val originalUri: Uri,
    val trashUri: Uri,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val deletedAt: Long,
    val daysUntilDeletion: Int
) {
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")

    val isImage: Boolean
        get() = mimeType.startsWith("image/")
}
