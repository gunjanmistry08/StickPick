package com.gunjan.teletowa.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gunjan.teletowa.R
import com.gunjan.teletowa.data.local.PackStorage
import com.gunjan.teletowa.data.model.StickerPack
import com.gunjan.teletowa.ui.navigation.Routes
import com.gunjan.teletowa.ui.theme.NeoGlassCard
import com.gunjan.teletowa.ui.theme.NeoGlassStyle
import com.gunjan.teletowa.ui.theme.Poppins
import com.gunjan.teletowa.viewmodel.MainViewModel
import com.gunjan.teletowa.whatsapp.WhatsAppIntentHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val packThumbCache = object : LruCache<String, Bitmap>(
    (Runtime.getRuntime().maxMemory() / 1024 / 16).toInt()
) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPacksScreen(vm: MainViewModel, onNavigate: (String) -> Unit) {
    val packs by vm.savedPacks.collectAsState()
    val waBusiness by vm.whatsappBusiness.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity
    val packStorage = remember { PackStorage(context) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.loadSavedPacks() }
    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    Scaffold(
        topBar = {},
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {},
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // Neo Glow Ambient Background
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .offset(x = (-40).dp, y = (-20).dp)
                    .blur(90.dp)
                    .background(Color(0x307B2CBF), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 60.dp, y = 80.dp)
                    .blur(90.dp)
                    .background(Color(0x2200E5CC), CircleShape)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(Modifier.height(20.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.logo),
                            contentDescription = "TeleToWA",
                            modifier = Modifier.height(34.dp),
                            contentScale = ContentScale.FillHeight
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "My Vault",
                        fontFamily = Poppins,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Manage, preview, and sync converted sticker collections",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // Quick stats banner
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Create Pack Card
                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .height(130.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(NeoGlassStyle.VioletCyanGradient)
                                .clickable { onNavigate(Routes.HOME) }
                                .padding(18.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0x33FFFFFF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.AddCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        "New Pack",
                                        fontFamily = Poppins,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        "Convert Telegram URL",
                                        fontFamily = Poppins,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }

                        // Total Packs Stats Box
                        NeoGlassCard(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(130.dp),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "${packs.size}",
                                    fontFamily = Poppins,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 34.sp,
                                    color = Color(0xFF00E5CC)
                                )
                                Text(
                                    "PACKS",
                                    fontFamily = Poppins,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }
                    }
                }

                if (packs.isEmpty()) {
                    item {
                        Spacer(Modifier.height(40.dp))
                        NeoGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color(0x1F8B5CF6), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Style,
                                        contentDescription = null,
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No Packs Stored Yet",
                                    fontFamily = Poppins,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Paste a Telegram sticker pack link on Home to convert your first collection.",
                                    fontFamily = Poppins,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(packs, key = { it.identifier }) { pack ->
                    PackCard(
                        pack = pack,
                        vm = vm,
                        packStorage = packStorage,
                        showBusiness = waBusiness,
                        onAddToWhatsApp = {
                            WhatsAppIntentHelper.addStickerPackToWhatsApp(activity, pack.identifier, pack.name)
                        },
                        onAddToBusiness = {
                            WhatsAppIntentHelper.addStickerPackToWhatsAppBusiness(activity, pack.identifier, pack.name)
                        }
                    )
                }
                item { Spacer(Modifier.height(90.dp)) }
            }

            GlassBottomNav(
                currentRoute = Routes.MY_PACKS,
                onNavigate = onNavigate,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PackCard(
    pack: StickerPack,
    vm: MainViewModel,
    packStorage: PackStorage,
    showBusiness: Boolean,
    onAddToWhatsApp: () -> Unit,
    onAddToBusiness: () -> Unit
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var editName by remember(pack.name) { mutableStateOf(pack.name) }

    val trayFile = remember(pack.identifier, pack.trayImageFile) {
        val baseId = run {
            val last = pack.identifier.lastIndexOf('_')
            if (last > 0) pack.identifier.substring(0, last) else pack.identifier
        }
        val baseDir = packStorage.getConvertedDir(baseId)
        val f = File(baseDir, pack.trayImageFile)
        if (f.exists()) f
        else {
            val exactDir = packStorage.getConvertedDir(pack.identifier)
            val ef = File(exactDir, pack.trayImageFile)
            if (ef.exists()) ef else null
        }
    }

    NeoGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showDetailDialog = true }
            ) {
                // Tray image preview with glass frame
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x33101422))
                        .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (trayFile != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(trayFile).crossfade(true).build(),
                            contentDescription = "Pack tray",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            Icons.Default.Style,
                            contentDescription = null,
                            tint = Color(0xFF00E5CC),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        pack.name,
                        fontFamily = Poppins,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${pack.stickers.size} Stickers • ${pack.publisher}",
                        fontFamily = Poppins,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Text(
                        "Tap to inspect items",
                        fontFamily = Poppins,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00E5CC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { vm.deletePack(pack.identifier) }) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (showBusiness) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAddToWhatsApp,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "WhatsApp",
                            fontFamily = Poppins,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = onAddToBusiness,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008069))
                    ) {
                        Icon(
                            Icons.Outlined.Business,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Business",
                            fontFamily = Poppins,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Button(
                    onClick = onAddToWhatsApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Export to WhatsApp",
                        fontFamily = Poppins,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Rename Pack", fontFamily = Poppins, fontWeight = FontWeight.Bold) },
            text = {
                TextField(
                    value = editName,
                    onValueChange = { editName = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updatePackName(pack.identifier, editName)
                    showEditDialog = false
                }) {
                    Text("Save", color = Color(0xFF00E5CC), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDetailDialog) {
        PackDetailDialog(pack = pack, packStorage = packStorage, onDismiss = { showDetailDialog = false })
    }
}

@Composable
private fun PackDetailDialog(pack: StickerPack, packStorage: PackStorage, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val baseId = remember(pack.identifier) {
        val last = pack.identifier.lastIndexOf('_')
        if (last > 0) pack.identifier.substring(0, last) else pack.identifier
    }
    val convertedDir = remember(baseId) { packStorage.getConvertedDir(baseId) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.78f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeoGlassStyle.GlassCardBorder)
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            pack.name,
                            fontFamily = Poppins,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${pack.stickers.size} stickers total",
                            fontFamily = Poppins,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(pack.stickers) { index, sticker ->
                        val convertedFile = File(convertedDir, sticker.imageFileName)
                        val rawFile = if (sticker.rawFilePath.isNotEmpty()) File(sticker.rawFilePath) else null

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x221E263E))
                                .border(1.dp, Color(0x268B5CF6), RoundedCornerShape(14.dp))
                                .padding(6.dp)
                        ) {
                            when {
                                convertedFile.exists() -> {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(convertedFile).crossfade(true).build(),
                                        contentDescription = "Sticker ${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                rawFile != null && rawFile.exists() && rawFile.extension == "webm" -> {
                                    CachedThumb(rawFile, Modifier.fillMaxSize())
                                }
                                else -> {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.BrokenImage,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            if (rawFile?.extension?.lowercase() == "webm") {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(2.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xAA0B0D15)
                                ) {
                                    Icon(
                                        Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5CC),
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CachedThumb(file: File, modifier: Modifier = Modifier) {
    val path = file.absolutePath
    var bitmap by remember(path) { mutableStateOf(packThumbCache.get(path)) }
    if (bitmap == null) {
        LaunchedEffect(path) {
            val bmp = withContext(Dispatchers.IO) {
                try {
                    val r = MediaMetadataRetriever(); r.setDataSource(path)
                    val frame = r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    r.release(); frame
                } catch (e: Exception) { null }
            }
            if (bmp != null) { packThumbCache.put(path, bmp); bitmap = bmp }
        }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF8B5CF6))
        }
    }
}
