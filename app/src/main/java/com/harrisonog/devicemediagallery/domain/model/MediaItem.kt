package com.harrisonog.devicemediagallery.domain.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val path: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val width: Int,
    val height: Int,
    val duration: Long? = null,
    val tags: List<Tag> = emptyList(),
    val isSelected: Boolean = false
) {
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")

    val isImage: Boolean
        get() = mimeType.startsWith("image/")
}
