package com.gunjan.teletowa.conversion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import com.gunjan.teletowa.data.local.PackStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

data class ConversionResult(val file: File, val isAnimated: Boolean)

class ConversionEngine(context: Context) {
    private val storage = PackStorage(context)

    companion object {
        private const val TAG = "TeleToWA"
    }

    /**
     * Converts a sticker file into a WhatsApp-compliant 512x512 WebP sticker.
     * Uses Rust native engine (teletowa_core) for high speed, low memory, and minimal binary size.
     */
    suspend fun convertSticker(
        inputFile: File,
        packId: String,
        outputName: String,
        lossless: Boolean = false
    ): Result<ConversionResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val outDir = storage.getConvertedDir(packId)
            val outputFile = File(outDir, "$outputName.webp")
            outputFile.delete()

            when (val ext = inputFile.extension.lowercase()) {
                "tgs" -> {
                    Log.i(TAG, "⚡ Converting .tgs vector sticker via Rust engine (lossless=$lossless): ${inputFile.name}")
                    if (RustEngine.convertTgsFile(inputFile, outputFile, lossless) && isAnimatedWebp(outputFile)) {
                        val duration = System.currentTimeMillis() - startTime
                        Log.i(TAG, "✅ Converted .tgs in ${duration}ms (size: ${outputFile.length() / 1024} KB) -> ${outputFile.name}")
                        return@withContext Result.success(ConversionResult(outputFile, true))
                    }
                    outputFile.delete()
                    Log.e(TAG, "❌ Failed to convert .tgs sticker: ${inputFile.name}")
                    return@withContext Result.failure(Exception("Cannot convert TGS: ${inputFile.name}"))
                }

                "webm", "mp4" -> {
                    Log.i(TAG, "⚡ Converting video sticker ($ext) via Rust engine (lossless=$lossless): ${inputFile.name}")
                    // 1. Primary: Animated WebP via Rust VP9 engine (for .webm)
                    if (ext == "webm" && RustEngine.convertWebmFile(inputFile, outputFile, lossless) && isAnimatedWebp(outputFile)) {
                        val duration = System.currentTimeMillis() - startTime
                        Log.i(TAG, "✅ Converted .webm in ${duration}ms (size: ${outputFile.length() / 1024} KB) -> ${outputFile.name}")
                        return@withContext Result.success(ConversionResult(outputFile, true))
                    }

                    // 2. Fallback: Extract first frame to static WebP via Android MediaMetadataRetriever
                    Log.w(TAG, "⚠️ Video animation conversion skipped/fallback for ${inputFile.name}, extracting static frame...")
                    outputFile.delete()
                    if (convertVideoFrameToStatic(inputFile, outputFile)) {
                        val duration = System.currentTimeMillis() - startTime
                        Log.i(TAG, "✅ Extracted static frame in ${duration}ms (size: ${outputFile.length() / 1024} KB)")
                        return@withContext Result.success(ConversionResult(outputFile, false))
                    }

                    outputFile.delete()
                    Log.e(TAG, "❌ Cannot convert video: ${inputFile.name}")
                    return@withContext Result.failure(Exception("Cannot convert video: ${inputFile.name}"))
                }

                else -> {
                    Log.i(TAG, "⚡ Converting static sticker ($ext) via Rust engine (lossless=$lossless): ${inputFile.name}")
                    // Primary: Fast Rust static conversion (512x512 transparent pad, quality tuned)
                    if (RustEngine.convertStaticFile(inputFile, outputFile, lossless) && outputFile.exists() && outputFile.length() > 100) {
                        val duration = System.currentTimeMillis() - startTime
                        Log.i(TAG, "✅ Converted static image in ${duration}ms (size: ${outputFile.length() / 1024} KB) -> ${outputFile.name}")
                        return@withContext Result.success(ConversionResult(outputFile, false))
                    }

                    // Fallback: Android Bitmap decoder
                    Log.w(TAG, "⚠️ Rust static converter failed for ${inputFile.name}, trying Android Bitmap fallback...")
                    convertImageFallback(inputFile, outputFile, lossless)
                    if (outputFile.exists() && outputFile.length() > 100) {
                        val duration = System.currentTimeMillis() - startTime
                        Log.i(TAG, "✅ Converted static image via fallback in ${duration}ms (size: ${outputFile.length() / 1024} KB)")
                        return@withContext Result.success(ConversionResult(outputFile, false))
                    }

                    Log.e(TAG, "❌ Cannot convert image: ${inputFile.name}")
                    return@withContext Result.failure(Exception("Cannot convert: ${inputFile.name}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Exception during conversion of ${inputFile.name}: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun convertVideoFrameToStatic(input: File, output: File): Boolean {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(input.absolutePath)
            val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            if (frame == null) return false

            val scaled = scaleTo512Transparent(frame)
            writeWebp(scaled, output)
            if (scaled !== frame) scaled.recycle()
            frame.recycle()
            output.exists() && output.length() > 100
        } catch (e: Exception) {
            false
        }
    }

    private fun isAnimatedWebp(file: File): Boolean {
        if (!file.exists() || file.length() < 30) return false
        return try {
            val raf = RandomAccessFile(file, "r")
            val header = ByteArray(30)
            raf.read(header)
            raf.close()
            val content = String(header, Charsets.ISO_8859_1)
            if (!content.startsWith("RIFF") || !content.contains("WEBP")) return false
            if (content.contains("VP8X")) {
                val flagsByte = header[20].toInt() and 0xFF
                return (flagsByte and 0x02) != 0
            }
            val raf2 = RandomAccessFile(file, "r")
            val scanBuf = ByteArray(minOf(file.length(), 4096).toInt())
            raf2.read(scanBuf)
            raf2.close()
            String(scanBuf, Charsets.ISO_8859_1).contains("ANIM")
        } catch (e: Exception) {
            false
        }
    }

    private fun convertImageFallback(input: File, output: File, lossless: Boolean = false) {
        val bitmap = BitmapFactory.decodeFile(input.absolutePath) ?: return
        val scaled = scaleTo512Transparent(bitmap)
        val quality = if (lossless) 95 else 80
        writeWebp(scaled, output, quality)
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
    }

    private fun scaleTo512Transparent(bitmap: Bitmap): Bitmap {
        val maxSize = 512
        val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
        val newW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val newH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        val result = Bitmap.createBitmap(maxSize, maxSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        canvas.drawBitmap(scaled, (maxSize - newW) / 2f, (maxSize - newH) / 2f, null)
        if (scaled !== bitmap) scaled.recycle()
        return result
    }

    @Suppress("DEPRECATION")
    private fun writeWebp(bitmap: Bitmap, output: File, quality: Int = 80) {
        FileOutputStream(output).use { fos ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, fos)
            } else {
                bitmap.compress(Bitmap.CompressFormat.WEBP, quality, fos)
            }
        }
    }

    suspend fun createTrayImage(inputFile: File, packId: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outDir = storage.getConvertedDir(packId)
            val trayFile = File(outDir, "tray_${packId}.png")

            // 1. Primary: Rust tray generator (<= 50 KB compliant)
            if (RustEngine.createTrayIconFile(inputFile, trayFile) && trayFile.exists() && trayFile.length() > 50) {
                Log.i(TAG, "✅ Generated tray icon via Rust engine (${trayFile.length()} bytes)")
                return@withContext Result.success(trayFile)
            }

            // 2. Fallback: Android framework
            val bitmap = when (inputFile.extension.lowercase()) {
                "webm", "mp4" -> {
                    val r = MediaMetadataRetriever()
                    r.setDataSource(inputFile.absolutePath)
                    val f = r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    r.release(); f
                }
                else -> BitmapFactory.decodeFile(inputFile.absolutePath)
            } ?: throw Exception("Cannot decode tray source")

            val scaled = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
            FileOutputStream(trayFile).use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            Log.i(TAG, "✅ Generated tray icon via Android fallback (${trayFile.length()} bytes)")
            Result.success(trayFile)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create tray image: ${e.message}", e)
            Result.failure(e)
        }
    }
}
