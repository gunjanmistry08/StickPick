package com.gunjan.teletowa.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gunjan.teletowa.data.local.PackStorage
import com.gunjan.teletowa.ui.navigation.Routes
import com.gunjan.teletowa.ui.theme.NeoGlassCard
import com.gunjan.teletowa.ui.theme.NeoGlassStyle
import com.gunjan.teletowa.ui.theme.Poppins
import com.gunjan.teletowa.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onNavigate: (String) -> Unit,
    onFetchComplete: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val stickerSet by vm.stickerSet.collectAsState()
    val author by vm.authorName.collectAsState()
    val savedPacks by vm.savedPacks.collectAsState()
    val context = LocalContext.current
    val packStorage = remember { PackStorage(context) }

    LaunchedEffect(Unit) { vm.loadSavedPacks() }
    LaunchedEffect(stickerSet) { if (stickerSet != null) onFetchComplete() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {}
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Ambient Neon background glow orbs
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .offset(x = (-70).dp, y = (-50).dp)
                    .blur(110.dp)
                    .background(Color(0x357B2CBF), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 70.dp, y = (-20).dp)
                    .blur(110.dp)
                    .background(Color(0x2800E5CC), CircleShape)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Top Creator Studio Header & Badge
                item {
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "StickerForge",
                                    fontFamily = Poppins,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFECEEF6),
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF00E5CC), CircleShape)
                                )
                            }
                            Text(
                                if (author.isNotBlank()) "Studio • $author" else "Studio • Creator Mode",
                                fontFamily = Poppins,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        // Engine Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1F8B5CF6))
                                .border(1.dp, Color(0x388B5CF6), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF00E5CC), CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "RUST CORE READY",
                                    fontFamily = Poppins,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5CC),
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }
                }

                // Greeting & Tagline Banner
                item {
                    NeoGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0x228B5CF6), Color(0x0C00E5CC), Color(0x00000000))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Text(
                                "Forge WhatsApp Packs",
                                fontFamily = Poppins,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Turn Telegram packs into WhatsApp stickers in seconds with auto-resizing & transparent framing.",
                                fontFamily = Poppins,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                // Quick Action Chips Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionChip(
                            icon = Icons.Outlined.ContentPaste,
                            label = "Paste Link",
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString() ?: ""
                                        if (text.isNotBlank()) input = text
                                    }
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionChip(
                            icon = Icons.Outlined.FolderOpen,
                            label = "Open Vault",
                            onClick = { onNavigate(Routes.MY_PACKS) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionChip(
                            icon = Icons.Outlined.Tune,
                            label = "Settings",
                            onClick = { onNavigate(Routes.SETTINGS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Main Importer / Console Panel
                item {
                    NeoGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "IMPORT TELEGRAM PACK",
                                    fontFamily = Poppins,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5CC),
                                    letterSpacing = 1.sp
                                )
                                if (input.isNotEmpty()) {
                                    Text(
                                        "READY TO FORGE",
                                        fontFamily = Poppins,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8B5CF6)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            TextField(
                                value = input,
                                onValueChange = { input = it },
                                placeholder = {
                                    Text(
                                        "Paste pack link or name...",
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, NeoGlassStyle.GlassInputBorder, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0x33101422),
                                    unfocusedContainerColor = Color(0x22101422),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Link,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5CC),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (input.isNotEmpty()) {
                                        IconButton(onClick = { input = "" }) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )

                            Spacer(Modifier.height(16.dp))

                            // Forge Button
                            Button(
                                onClick = {
                                    if (input.isNotBlank()) {
                                        vm.resetWorkflow()
                                        vm.fetchStickerSet(input)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (input.isNotBlank() && !isLoading) NeoGlassStyle.VioletCyanGradient
                                        else Brush.horizontalGradient(listOf(Color(0xFF232A3E), Color(0xFF232A3E)))
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                enabled = input.isNotBlank() && !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AutoFixHigh,
                                            contentDescription = null,
                                            tint = if (input.isNotBlank()) Color.White else Color.White.copy(alpha = 0.4f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Forge Stickers",
                                            fontFamily = Poppins,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (input.isNotBlank()) Color.White else Color.White.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Error Message Box
                item {
                    AnimatedVisibility(visible = error != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x22FF3366))
                                .border(1.dp, Color(0x44FF3366), RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    error ?: "",
                                    fontFamily = Poppins,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF8585),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Recent Packs Carousel
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "YOUR STICKER VAULT",
                            fontFamily = Poppins,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            "View All",
                            fontFamily = Poppins,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5CC),
                            modifier = Modifier.clickable { onNavigate(Routes.MY_PACKS) }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    if (savedPacks.isEmpty()) {
                        NeoGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No packs forged yet",
                                    fontFamily = Poppins,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    "Paste a Telegram sticker link above to begin",
                                    fontFamily = Poppins,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(savedPacks.take(5), key = { it.identifier }) { pack ->
                                val baseId = run {
                                    val last = pack.identifier.lastIndexOf('_')
                                    if (last > 0) pack.identifier.substring(0, last) else pack.identifier
                                }
                                val baseDir = packStorage.getConvertedDir(baseId)
                                val trayFile = File(baseDir, pack.trayImageFile)

                                Box(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0x221E263E))
                                        .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(20.dp))
                                        .clickable { onNavigate(Routes.MY_PACKS) }
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(76.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0x33101422)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (trayFile.exists()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context).data(trayFile).crossfade(true).build(),
                                                    contentDescription = pack.name,
                                                    modifier = Modifier.fillMaxSize().padding(6.dp),
                                                    contentScale = ContentScale.Fit
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Style,
                                                    contentDescription = null,
                                                    tint = Color(0xFF00E5CC),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            pack.name,
                                            fontFamily = Poppins,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${pack.stickers.size} stickers",
                                            fontFamily = Poppins,
                                            fontSize = 11.sp,
                                            color = Color(0xFF00E5CC),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(90.dp)) }
            }

            GlassBottomNav(
                currentRoute = Routes.HOME,
                onNavigate = onNavigate,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1F1E263E))
            .border(1.dp, Color(0x288B5CF6), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color(0xFF00E5CC),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                fontFamily = Poppins,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun GlassBottomNav(currentRoute: String, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xEE101422))
                .border(1.dp, NeoGlassStyle.GlassCardBorder, RoundedCornerShape(32.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassNavItem(
                    Icons.Filled.Home, Icons.Outlined.Home, "Studio",
                    currentRoute == Routes.HOME
                ) { onNavigate(Routes.HOME) }
                GlassNavItem(
                    Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "Vault",
                    currentRoute == Routes.MY_PACKS
                ) { onNavigate(Routes.MY_PACKS) }
                GlassNavItem(
                    Icons.Filled.Settings, Icons.Outlined.Settings, "Config",
                    currentRoute == Routes.SETTINGS
                ) { onNavigate(Routes.SETTINGS) }
            }
        }
    }
}

@Composable
private fun GlassNavItem(
    filledIcon: ImageVector,
    outlinedIcon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (selected) Color(0x288B5CF6) else Color.Transparent
            )
            .border(
                1.dp,
                if (selected) Color(0x408B5CF6) else Color.Transparent,
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            onClick = onClick,
            color = Color.Transparent
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (selected) filledIcon else outlinedIcon,
                    contentDescription = label,
                    tint = if (selected) Color(0xFF00E5CC)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                AnimatedVisibility(
                    visible = selected,
                    enter = expandHorizontally(tween(200)) + fadeIn(tween(200)),
                    exit = shrinkHorizontally(tween(150)) + fadeOut(tween(150))
                ) {
                    Row {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            label,
                            fontFamily = Poppins,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5CC)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernBottomNav(currentRoute: String, onNavigate: (String) -> Unit) {
    GlassBottomNav(currentRoute = currentRoute, onNavigate = onNavigate)
}
