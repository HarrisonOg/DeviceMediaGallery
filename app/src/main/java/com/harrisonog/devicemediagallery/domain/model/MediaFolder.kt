package com.harrisonog.devicemediagallery.domain.model

data class MediaFolder(
    val path: String,
    val name: String,
    val mediaCount: Int,
    val coverItem: MediaItem? = null,
    val tags: List<Tag> = emptyList(),
    val isVirtual: Boolean = false,
    val albumId: Long? = null
)
