package com.lumi.stickerize.ai

import android.graphics.Bitmap

class AiException(message: String) : Exception(message)

/** Penyedia AI gambar (OpenRouter berbayar atau Cloudflare gratis). */
interface ImageGenerator {
    /** Jumlah foto referensi maksimal yang diterima model. */
    val maxReferences: Int

    suspend fun generateSheet(prompt: String, jpegPhotos: List<ByteArray>, aspectRatio: String): Bitmap
}
