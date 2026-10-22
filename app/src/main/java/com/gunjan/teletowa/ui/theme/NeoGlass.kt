package com.gunjan.teletowa.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object NeoGlassStyle {
    val CardShape = RoundedCornerShape(22.dp)
    val PillShape = RoundedCornerShape(50)
    val ChipShape = RoundedCornerShape(14.dp)

    val VioletCyanGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF7B2CBF), Color(0xFF00E5CC))
    )

    val VioletPinkGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF8B5CF6), Color(0xFFFF5C93))
    )

    val NeonGlowGradient = Brush.verticalGradient(
        colors = listOf(Color(0x338B5CF6), Color(0x0500F5D4))
    )

    val GlassCardBorder = Brush.verticalGradient(
        colors = listOf(Color(0x558B5CF6), Color(0x1500E5CC), Color(0x08FFFFFF))
    )

    val GlassInputBorder = Brush.horizontalGradient(
        colors = listOf(Color(0x408B5CF6), Color(0x2000E5CC))
    )
}

@Composable
fun NeoGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = NeoGlassStyle.CardShape,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val bg = if (isDark) Color(0x1F1E263E) else Color(0x99FFFFFF)
    val borderBrush = if (isDark) {
        NeoGlassStyle.GlassCardBorder
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0x306C3CE9), Color(0x2000897B), Color(0x20000000))
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(borderWidth, borderBrush, shape)
    ) {
        content()
    }
}
