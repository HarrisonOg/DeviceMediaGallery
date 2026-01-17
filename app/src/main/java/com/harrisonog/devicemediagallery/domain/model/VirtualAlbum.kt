package com.harrisonog.devicemediagallery.domain.model

import android.net.Uri

data class VirtualAlbum(
    val id: Long,
    val name: String,
    val coverImageUri: Uri? = null,
    val itemCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<Tag> = emptyList()
)
