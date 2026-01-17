package com.harrisonog.devicemediagallery.domain.model

import androidx.compose.ui.graphics.Color

data class Tag(
    val id: Long,
    val name: String,
    val color: Color,
    val usageCount: Int = 0
)
