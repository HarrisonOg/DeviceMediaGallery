package com.harrisonog.devicemediagallery.domain.model

data class DuplicateGroup(
    val groupHash: String,
    val mediaItems: List<MediaItem>,
    val detectedAt: Long
)
