package com.gunjan.teletowa.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gunjan.teletowa.ui.theme.NeoGlassCard
import com.gunjan.teletowa.ui.theme.NeoGlassStyle
import com.gunjan.teletowa.ui.theme.Poppins
import com.gunjan.teletowa.viewmodel.MainViewModel

@Composable
fun ConvertingScreen(vm: MainViewModel, onComplete: () -> Unit) {
    val progress by vm.conversionProgress.collectAsState()

    LaunchedEffect(Unit) { vm.convertAllStickers() }
    LaunchedEffect(progress.isComplete) {
        if (progress.isComplete && progress.error == null) onComplete()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.percentage,
        animationSpec = tween(500),
        label = "conversion"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background Orbs
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.Center)
                .blur(110.dp)
                .background(Color(0x307B2CBF), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Visual Cluster
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0x188B5CF6))
                        .border(1.dp, Color(0x338B5CF6), CircleShape)
                )

                // Central icon
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0x33101422))
                        .border(1.dp, NeoGlassStyle.GlassCardBorder, RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = Color(0xFF00E5CC),
                        modifier = Modifier.size(46.dp)
                    )
                }

                // Floating Sync Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-12).dp, y = 12.dp)
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeoGlassStyle.VioletCyanGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            Text(
                "Converting to WhatsApp WebP...",
                fontFamily = Poppins,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "FFmpeg rendering 512x512 transparent frames & optimal payloads",
                fontFamily = Poppins,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Progress Glass Card
            NeoGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(22.dp)) {
                    // Linear Progress
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF00E5CC),
                        trackColor = Color(0x338B5CF6),
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x288B5CF6))
                                .border(1.dp, Color(0x408B5CF6), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5CC),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${(animatedProgress * 100).toInt()}% Rendered",
                                    fontFamily = Poppins,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5CC)
                                )
                            }
                        }
                        Text(
                            "${progress.convertedStickers} of ${progress.totalStickers} items",
                            fontFamily = Poppins,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Status Steps
            ConversionStep(
                title = "Format & Transparent Matting",
                subtitle = "Ensuring zero black borders on WhatsApp dark/light chat",
                isActive = progress.percentage < 0.5f && !progress.isComplete,
                isComplete = progress.percentage >= 0.5f || progress.isComplete
            )
            Spacer(Modifier.height(12.dp))
            ConversionStep(
                title = "Payload Compression & Chunking",
                subtitle = "Enforcing WhatsApp 500KB animated limits & auto-splitting",
                isActive = progress.percentage >= 0.5f && !progress.isComplete,
                isComplete = progress.isComplete
            )

            if (progress.error != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    progress.error!!,
                    color = Color(0xFFFF5252),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(30.dp))

            // Info Toast
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x2514192A))
                    .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF00E5CC),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Please keep TeleToWA active in foreground",
                        fontFamily = Poppins,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversionStep(title: String, subtitle: String, isActive: Boolean, isComplete: Boolean) {
    val alpha = when {
        isActive -> 1f
        isComplete -> 1f
        else -> 0.5f
    }
    NeoGlassCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isComplete) Color(0xFF00E5CC)
                        else if (isActive) Color(0xFF8B5CF6)
                        else Color(0x338B5CF6)
                    )
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontFamily = Poppins,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                Text(
                    subtitle,
                    fontFamily = Poppins,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isComplete) Color(0x2600E5CC)
                        else if (isActive) Color(0x268B5CF6)
                        else Color(0x15FFFFFF)
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    when {
                        isComplete -> "DONE"
                        isActive -> "ACTIVE"
                        else -> "WAITING"
                    },
                    fontFamily = Poppins,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isComplete -> Color(0xFF00E5CC)
                        isActive -> Color(0xFF8B5CF6)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
            }
        }
    }
}
