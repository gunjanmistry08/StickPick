# AGENTS.md — TeleToWA Guidelines & Architecture

Welcome to **TeleToWA**! This document provides AI coding agents and developers with essential context, architectural structure, development conventions, and operational workflows for this repository.

---

## 1. Project Overview

TeleToWA is a modern Android application built using Jetpack Compose that allows users to:
1. Fetch Telegram sticker packs using Telegram Bot API.
2. Download static (`.webp`) and animated/video (`.webm`, `.mp4`) stickers.
3. Convert video/animated stickers to WhatsApp-compatible animated/static WebP using FFmpeg.
4. Auto-split packs exceeding WhatsApp constraints (maximum 30 stickers for animated packs, maximum 50 stickers for static packs, min 3 stickers).
5. Export packs directly to WhatsApp / WhatsApp Business via a custom ContentProvider implementing the official WhatsApp Sticker API.

---

## 2. Tech Stack & Key Libraries

- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose + Material 3 + Jetpack Navigation Compose
- **Target / Min SDK**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`
- **Architecture**: MVVM (Model-View-ViewModel) with StateFlow / Kotlin Coroutines
- **Networking**: Retrofit 2 + OkHttp 3 + Gson
- **Media Processing**: `com.antonkarpenko:ffmpeg-kit-video:2.1.0` (FFmpeg wrapper)
- **Image Loading**: Coil (`coil-compose`, `coil-gif`)
- **Storage & State**: Android DataStore Preferences + Local App Storage (`PackStorage`)
- **Integration**: Custom Android `ContentProvider` (`StickerContentProvider`) conforming to WhatsApp Sticker API specifications.

---

## 3. Repository Structure & Key Packages

```
TeleToWA/
├── app/
│   ├── build.gradle.kts                   # App-level dependencies & NK/ABI configurations
│   └── src/main/
│       ├── AndroidManifest.xml            # Permissions, Provider declarations, queries
│       ├── java/com/avishkar/stickpick/
│       │   ├── MainActivity.kt            # Main entry point & theme container
│       │   ├── conversion/
│       │   │   └── ConversionEngine.kt    # FFmpeg processing, resizing to 512x512, WebP conversion
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── PackStorage.kt         # Disk persistence for raw/converted packs & metadata
│       │   │   │   └── PreferencesManager.kt  # User preferences (bot token, publisher name, theme)
│       │   │   ├── model/
│       │   │   │   └── Models.kt              # StickerPack, Sticker, Telegram API response models
│       │   │   ├── remote/
│       │   │   │   ├── RetrofitClient.kt      # Retrofit instance builder
│       │   │   │   └── TelegramApi.kt         # Telegram Bot API endpoints (getStickerSet, getFile)
│       │   │   └── repository/
│       │   │       └── StickerRepository.kt   # High-level repo coordinating API & downloads
│       │   ├── ui/
│       │   │   ├── navigation/
│       │   │   │   └── AppNavGraph.kt         # Navigation routes & screen composables
│       │   │   ├── screens/
│       │   │   │   ├── HomeScreen.kt          # Input Telegram pack URL / manage packs
│       │   │   │   ├── DownloadingScreen.kt   # Download progress indicator
│       │   │   │   ├── PreviewScreen.kt       # Sticker preview, selection, reordering, adding custom images
│       │   │   │   ├── ConvertingScreen.kt    # Conversion progress (FFmpeg)
│       │   │   │   ├── MyPacksScreen.kt       # View converted sticker packs
│       │   │   │   ├── SettingsScreen.kt      # Bot token, author name, app preferences
│       │   │   │   └── OnboardingScreen.kt    # First-run setup wizard
│       │   │   └── theme/                 # Material 3 colors, typography, theme definition
│       │   ├── viewmodel/
│       │   │   └── MainViewModel.kt       # Core state management across UI screens
│       │   └── whatsapp/
│       │       ├── StickerContentProvider.kt # WhatsApp ContentProvider implementation
│       │       └── WhatsAppIntentHelper.kt   # Intents to trigger "Add to WhatsApp"
│       └── res/                           # Drawables, fonts (Poppins), values
├── gradle/
│   └── libs.versions.toml                 # Version catalog
└── README.md                              # User setup and quickstart guide
```

---

## 4. Key Architectural & Domain Rules

### 4.1 WhatsApp Sticker API Requirements
- **Sticker Dimensions**: Exactly `512 x 512` pixels.
- **File Format**: `.webp` (transparent background where applicable, no black borders).
- **Pack Sizes**:
  - Minimum: 3 stickers per pack.
  - Maximum: 30 stickers for animated packs; up to 50 for static packs.
- **File Size Limits**:
  - Animated stickers: must stay strictly within WhatsApp's animated WebP size limits (typically ≤ 500 KB per sticker).
  - Static stickers: ≤ 100 KB per sticker.
  - Tray icon: `96 x 96` PNG or WebP, ≤ 50 KB.
- **Provider Authority**: Must match `AUTHORITY = "com.avishkar.stickpick.stickercontentprovider"` in `StickerContentProvider.kt` and `AndroidManifest.xml`.

### 4.2 FFmpeg & ABI Filters
- FFmpeg native binaries (`libffmpegkit.so`) add significant APK size.
- `app/build.gradle.kts` restricts ABIs to `arm64-v8a` and `armeabi-v7a` in release to prevent bloated APKs.
- When testing on an x86/x86_64 emulator, keep in mind FFmpeg emulation can be slow or may require adding x86 ABI filters back to `abiFilters` if debugging locally on standard emulators.

### 4.3 Data Flow & Concurrency
- Network requests and FFmpeg tasks must **never** run on `Dispatchers.Main`. Always use `Dispatchers.IO` / background coroutines.
- State is managed via `MainViewModel` and exposed as `StateFlow` / Compose states.

---

## 5. Development & Build Commands

When executing commands, use Gradle wrapper from the project root:

```bash
# Check dependencies and project build health
./gradlew check

# Build debug APK
./gradlew assembleDebug

# Build release APK (skipping lint vital if needed)
./gradlew assembleRelease -x lintVitalAnalyzeRelease -x lintVitalReportRelease -x lintVitalRelease

# Run unit tests
./gradlew testDebugUnitTest
```

---

## 6. Coding Standards & Guidelines for AI Agents

1. **Idiomatic Kotlin**: Use idiomatic Kotlin constructs (null-safety, data classes, `sealed interface`/`sealed class` for UI states, coroutines, `Flow`).
2. **Jetpack Compose Guidelines**:
   - Keep Composables stateless where feasible; hoist state into caller or ViewModel.
   - Use Material 3 design components and semantic theme tokens (`MaterialTheme.colorScheme.*`).
   - Use `remember` and `derivedStateOf` appropriately to avoid unnecessary recompositions.
3. **Preserve Comments & Metadata**: Do not delete existing comments or structural docstrings unless deliberately updating or refactoring that specific feature.
4. **Android Permissions & Compatibility**:
   - Check compatibility when using new Android 14+ / 15+ APIs against `minSdk = 26`.
   - Respect scoped storage rules; read/write within app-specific storage directories (`context.filesDir` / `context.cacheDir`).
5. **WhatsApp ContentProvider Integrity**:
   - Column names in `StickerContentProvider.kt` must strictly adhere to the WhatsApp Sticker API specification. Do not rename or alter column keys (`sticker_pack_identifier`, `sticker_pack_name`, etc.).
