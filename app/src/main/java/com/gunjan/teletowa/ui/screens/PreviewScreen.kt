package com.gunjan.teletowa.ui.screens

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gunjan.teletowa.ui.theme.NeoGlassStyle
import com.gunjan.teletowa.ui.theme.Poppins
import com.gunjan.teletowa.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private val thumbnailCache = object : LruCache<String, Bitmap>(
    (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    vm: MainViewModel,
    onConvert: () -> Unit,
    onBack: () -> Unit
) {
    val files by vm.downloadedFiles.collectAsState()
    val stickerSet by vm.stickerSet.collectAsState()
    val context = LocalContext.current

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@forEach
                val ext = context.contentResolver.getType(uri)?.substringAfter("/") ?: "webp"
                val file = File(context.filesDir, "stickers/raw/gallery_${System.currentTimeMillis()}.$ext")
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { fos -> inputStream.copyTo(fos) }
                inputStream.close()
                vm.addExternalFile(file)
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Review Pack",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onConvert,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(NeoGlassStyle.VioletCyanGradient)
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Convert Pack",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                stickerSet?.title ?: "Sticker Pack",
                fontFamily = Poppins,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF00E5CC), CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${files.size} stickers selected",
                    fontFamily = Poppins,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(files, key = { _, f -> f.absolutePath }) { index, file ->
                    StickerCard(file = file, index = index, onDelete = { vm.removeSticker(index) })
                }
                item {
                    AddStickerCard(onClick = { galleryLauncher.launch("image/*") })
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun StickerCard(file: File, index: Int, onDelete: () -> Unit) {
    val isVideo = file.extension.lowercase() in listOf("webm", "mp4")

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x221E263E))
            .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(18.dp))
    ) {
        if (isVideo) {
            CachedVideoThumbnail(file = file, modifier = Modifier.fillMaxSize().padding(10.dp))
        } else {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context).data(file).crossfade(true).build(),
                contentDescription = "Sticker ${index + 1}",
                modifier = Modifier.fillMaxSize().padding(10.dp),
                contentScale = ContentScale.Fit
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .background(Color(0x880B0D15), CircleShape)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Delete",
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(14.dp)
            )
        }

        if (isVideo) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xAA0B0D15)
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color(0xFF00E5CC),
                    modifier = Modifier
                        .padding(3.dp)
                        .size(12.dp)
                )
            }
        }

        Text(
            "#${String.format("%02d", index + 1)}",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 6.dp),
            fontFamily = Poppins,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun CachedVideoThumbnail(file: File, modifier: Modifier = Modifier) {
    val path = file.absolutePath
    var bitmap by remember(path) { mutableStateOf(thumbnailCache.get(path)) }

    if (bitmap == null) {
        LaunchedEffect(path) {
            val bmp = withContext(Dispatchers.IO) {
                try {
                    val r = MediaMetadataRetriever()
                    r.setDataSource(path)
                    val frame = r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    r.release(); frame
                } catch (e: Exception) { null }
            }
            if (bmp != null) { thumbnailCache.put(path, bmp); bitmap = bmp }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF8B5CF6)
            )
        }
    }
}

@Composable
private fun AddStickerCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x1F1A2135)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0x338B5CF6), Color(0x3300E5CC)))),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0x338B5CF6),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add from gallery",
                        tint = Color(0xFF00E5CC),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Add Custom",
                fontFamily = Poppins,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
    }
}
