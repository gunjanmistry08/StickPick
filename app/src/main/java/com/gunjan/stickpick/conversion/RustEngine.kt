package com.gunjan.stickpick.conversion

import java.io.File

object RustEngine {
    init {
        System.loadLibrary("stickpick_core")
    }

    external fun ping(): String

    /**
     * Converts raw image bytes (WebP, PNG, JPEG) into WhatsApp-compliant static WebP bytes:
     * - Canvas: Exactly 512x512 with transparent padding
     * - Scaled to preserve original aspect ratio
     * - File size <= 100 KB
     *
     * Returns null if conversion failed.
     */
    external fun convertStaticImage(inputBytes: ByteArray): ByteArray?

    /**
     * Generates a 96x96 tray icon conforming to WhatsApp specification (<= 50 KB).
     *
     * Returns null if conversion failed.
     */
    external fun createTrayIcon(inputBytes: ByteArray): ByteArray?

    /**
     * Converts a Telegram .tgs sticker (gzipped Lottie JSON) to an animated WebP:
     * - Canvas: Exactly 512x512 with transparent padding
     * - Infinite loop (loop_count = 0)
     * - Duration <= 3.0 seconds
     * - File size <= 500 KB
     *
     * Returns null if conversion failed.
     */
    external fun convertTgs(inputBytes: ByteArray): ByteArray?

    /**
     * Converts a Telegram .webm (VP9) video sticker to an animated WebP:
     * - Canvas: Exactly 512x512 with transparent padding
     * - Infinite loop (loop_count = 0)
     * - Duration <= 3.0 seconds
     * - File size <= 500 KB
     *
     * Returns null if conversion failed.
     */
    external fun convertWebm(inputBytes: ByteArray): ByteArray?

    /**
     * Convenience helper to convert a .webm video file to an animated WebP file.
     */
    fun convertWebmFile(inputFile: File, outputFile: File): Boolean {
        return try {
            val bytes = inputFile.readBytes()
            val webpBytes = convertWebm(bytes) ?: return false
            outputFile.writeBytes(webpBytes)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Convenience helper to convert a .tgs file to an animated WebP file.
     */
    fun convertTgsFile(inputFile: File, outputFile: File): Boolean {
        return try {
            val bytes = inputFile.readBytes()
            val webpBytes = convertTgs(bytes) ?: return false
            outputFile.writeBytes(webpBytes)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Convenience helper to convert an input file and write the result to outputFile.
     */
    fun convertStaticFile(inputFile: File, outputFile: File): Boolean {
        return try {
            val bytes = inputFile.readBytes()
            val webpBytes = convertStaticImage(bytes) ?: return false
            outputFile.writeBytes(webpBytes)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Convenience helper to generate a tray icon file.
     */
    fun createTrayIconFile(inputFile: File, outputFile: File): Boolean {
        return try {
            val bytes = inputFile.readBytes()
            val webpBytes = createTrayIcon(bytes) ?: return false
            outputFile.writeBytes(webpBytes)
            true
        } catch (e: Exception) {
            false
        }
    }
}
