package com.gunjan.teletowa.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gunjan.teletowa.ui.navigation.Routes
import com.gunjan.teletowa.ui.theme.NeoGlassCard
import com.gunjan.teletowa.ui.theme.NeoGlassStyle
import com.gunjan.teletowa.ui.theme.Poppins
import com.gunjan.teletowa.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val token by vm.botToken.collectAsState()
    val author by vm.authorName.collectAsState()
    val pattern by vm.packNamingPattern.collectAsState()
    val autoSplit by vm.autoSplit.collectAsState()
    val lossless by vm.losslessConversion.collectAsState()
    val packLimit by vm.packLimit.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val waBusiness by vm.whatsappBusiness.collectAsState()

    var editToken by remember(token) { mutableStateOf(token) }
    var editAuthor by remember(author) { mutableStateOf(author) }
    var editPattern by remember(pattern) { mutableStateOf(pattern) }
    var showClearDialog by remember { mutableStateOf(false) }
    var tokenVisible by remember { mutableStateOf(false) }

    // Auto-save whenever fields change
    LaunchedEffect(editToken) { if (editToken != token && editToken.isNotBlank()) vm.updateSettings(editToken, editAuthor, editPattern, autoSplit, lossless) }
    LaunchedEffect(editAuthor) { if (editAuthor != author) vm.updateSettings(editToken, editAuthor, editPattern, autoSplit, lossless) }
    LaunchedEffect(editPattern) { if (editPattern != pattern) vm.updateSettings(editToken, editAuthor, editPattern, autoSplit, lossless) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Preferences",
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
        bottomBar = {}
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // Ambient Neo Orbs
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .offset(x = (-50).dp, y = 50.dp)
                    .blur(90.dp)
                    .background(Color(0x257B2CBF), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 50.dp, y = (-80).dp)
                    .blur(90.dp)
                    .background(Color(0x2000E5CC), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // Telegram Config Card
                SettingsCard(title = "Telegram Integration", icon = Icons.Outlined.SmartToy) {
                    Text(
                        "Bot API Token",
                        fontFamily = Poppins,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x22101422))
                            .border(1.dp, NeoGlassStyle.GlassInputBorder, RoundedCornerShape(14.dp))
                    ) {
                        TextField(
                            value = editToken,
                            onValueChange = { editToken = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                    Icon(
                                        if (tokenVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (tokenVisible) "Hide" else "Show",
                                        tint = Color(0xFF00E5CC),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Obtain instantly from @BotFather on Telegram",
                        fontFamily = Poppins,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 11.sp
                    )
                }

                // Identity Card
                SettingsCard(title = "Pack Metadata", icon = Icons.Outlined.Person) {
                    SettingsInput(label = "Default Publisher Name", value = editAuthor, onValueChange = { editAuthor = it })
                    Spacer(Modifier.height(14.dp))
                    SettingsInput(
                        label = "Pack Naming Pattern",
                        value = editPattern,
                        onValueChange = { editPattern = it },
                        hint = "Supported tags: {name}, {author}, {index}"
                    )
                }

                // Theme Mode Card
                SettingsCard(title = "Interface Theme", icon = Icons.Outlined.Palette) {
                    Text(
                        "Aesthetic Profile",
                        fontFamily = Poppins,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeChip("Light", "light", themeMode == "light", Icons.Outlined.LightMode) { vm.updateThemeMode("light") }
                        ThemeChip("Dark", "dark", themeMode == "dark", Icons.Outlined.DarkMode) { vm.updateThemeMode("dark") }
                        ThemeChip("System", "system", themeMode == "system", Icons.Outlined.SettingsBrightness) { vm.updateThemeMode("system") }
                    }
                }

                // Conversion Rules Card
                SettingsCard(title = "Conversion Engine", icon = Icons.Outlined.Tune) {
                    SettingsToggleRow(
                        title = "Auto-split large sets",
                        subtitle = "Divides into clean sub-packs automatically",
                        checked = autoSplit,
                        onCheckedChange = { vm.updateSettings(editToken, editAuthor, editPattern, it, lossless) }
                    )
                    Spacer(Modifier.height(14.dp))
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Sticker limit per pack",
                                fontFamily = Poppins,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "$packLimit stickers",
                                fontFamily = Poppins,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF00E5CC),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Slider(
                            value = packLimit.toFloat(),
                            onValueChange = { vm.updatePackLimit(it.toInt()) },
                            valueRange = 3f..50f,
                            steps = 46,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5CC),
                                activeTrackColor = Color(0xFF8B5CF6)
                            )
                        )
                        Text(
                            "WhatsApp limits: up to 30 for animated, 50 for static",
                            fontFamily = Poppins,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    SettingsToggleRow(
                        title = "Lossless visual mode",
                        subtitle = "High fidelity rendering with larger file weights",
                        checked = lossless,
                        onCheckedChange = { vm.updateSettings(editToken, editAuthor, editPattern, autoSplit, it) }
                    )
                }

                // WhatsApp Target Card
                SettingsCard(title = "WhatsApp Routing", icon = Icons.Outlined.Forum) {
                    SettingsToggleRow(
                        title = "WhatsApp Business Support",
                        subtitle = "Enable dedicated export channel for WA Business",
                        checked = waBusiness,
                        onCheckedChange = { vm.updateWhatsAppBusiness(it) }
                    )
                }

                // Danger Action
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x18FF3366))
                        .border(1.dp, Color(0x33FF3366), RoundedCornerShape(18.dp))
                ) {
                    TextButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Wipe Local Cache & Data",
                            fontFamily = Poppins,
                            color = Color(0xFFFF5252),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // App Info Badge
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "TeleToWA Studio v1.2",
                        fontFamily = Poppins,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(90.dp))
            }

            GlassBottomNav(
                currentRoute = Routes.SETTINGS,
                onNavigate = onNavigate,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All App Data", fontFamily = Poppins, fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all downloaded sticker files, converted WebP caches, and local metadata. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.clearHistory(); showClearDialog = false }) {
                    Text("Clear All", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    NeoGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x228B5CF6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color(0xFF00E5CC),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun SettingsInput(
    label: String, value: String, onValueChange: (String) -> Unit, hint: String? = null
) {
    Column {
        Text(
            label,
            fontFamily = Poppins,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x22101422))
                .border(1.dp, NeoGlassStyle.GlassInputBorder, RoundedCornerShape(14.dp))
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true
            )
        }
        if (hint != null) {
            Text(
                hint,
                fontFamily = Poppins,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontFamily = Poppins,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                fontFamily = Poppins,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00E5CC),
                checkedTrackColor = Color(0xFF7B2CBF)
            )
        )
    }
}

@Composable
private fun RowScope.ThemeChip(
    label: String,
    value: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0x338B5CF6) else Color(0x18101422))
            .border(
                1.dp,
                if (selected) Color(0xFF8B5CF6) else Color(0x228B5CF6),
                RoundedCornerShape(16.dp)
            )
    ) {
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (selected) Color(0xFF00E5CC) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    fontFamily = Poppins,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color(0xFF00E5CC) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
