package com.gunjan.teletowa.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gunjan.teletowa.ui.theme.NeoGlassCard
import com.gunjan.teletowa.ui.theme.NeoGlassStyle
import com.gunjan.teletowa.ui.theme.Poppins
import com.gunjan.teletowa.viewmodel.MainViewModel

@Composable
fun DownloadingScreen(
    vm: MainViewModel,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val progress by vm.downloadProgress.collectAsState()

    LaunchedEffect(Unit) { vm.downloadAllStickers() }
    LaunchedEffect(progress.isComplete) {
        if (progress.isComplete && progress.error == null) onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Glowing Background Orbs
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.Center)
                .blur(110.dp)
                .background(Color(0x337B2CBF), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Circular Progress with Neon Ring
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(Color(0x1F14192A))
                        .border(1.dp, Color(0x338B5CF6), CircleShape)
                )

                val animatedProgress by animateFloatAsState(
                    targetValue = progress.percentage,
                    animationSpec = tween(500),
                    label = "progress"
                )

                Canvas(modifier = Modifier.size(150.dp)) {
                    val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = Color(0x228B5CF6),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = stroke,
                        topLeft = Offset(stroke.width / 2, stroke.width / 2),
                        size = Size(size.width - stroke.width, size.height - stroke.width)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFF8B5CF6), Color(0xFF00E5CC), Color(0xFF8B5CF6))
                        ),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = stroke,
                        topLeft = Offset(stroke.width / 2, stroke.width / 2),
                        size = Size(size.width - stroke.width, size.height - stroke.width)
                    )
                }

                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    tint = Color(0xFF00E5CC),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(44.dp))

            Text(
                "Ingesting Stickers...",
                fontFamily = Poppins,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Extracting vector/video layers from Telegram network",
                fontFamily = Poppins,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Pack Info Glass Card
            NeoGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "ACTIVE PACK",
                            fontFamily = Poppins,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00E5CC),
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            progress.packName.ifBlank { "Initializing..." },
                            fontFamily = Poppins,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        "${(progress.percentage * 100).toInt()}%",
                        fontFamily = Poppins,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5CC)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Stats Row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    modifier = Modifier.weight(7f),
                    icon = { Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                    label = "Items Downloaded",
                    value = "${progress.downloadedStickers}/${progress.totalStickers}"
                )
                StatCard(
                    modifier = Modifier.weight(5f),
                    icon = { Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF00E5CC)) },
                    label = "Remaining",
                    value = "${progress.totalStickers - progress.downloadedStickers}"
                )
            }

            if (progress.error != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    progress.error!!,
                    color = Color(0xFFFF5252),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(28.dp))

            TextButton(onClick = onCancel) {
                Text(
                    "Cancel Process",
                    fontFamily = Poppins,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    NeoGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            icon()
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                fontFamily = Poppins,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Text(
                value,
                fontFamily = Poppins,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
