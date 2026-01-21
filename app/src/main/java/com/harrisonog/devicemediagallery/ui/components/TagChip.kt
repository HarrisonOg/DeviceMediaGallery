package com.harrisonog.devicemediagallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.harrisonog.devicemediagallery.domain.model.Tag

@Composable
fun TagChip(
    tag: Tag,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null
) {
    val textColor = if (tag.color.luminance() > 0.5f) Color.Black else Color.White

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tag.color)
            .padding(
                start = 12.dp,
                end = if (onRemove != null) 4.dp else 12.dp,
                top = 4.dp,
                bottom = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tag.name,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )

        if (onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove tag",
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
