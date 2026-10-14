package com.lumi.stickerize.sticker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import com.lumi.stickerize.model.GridSpec
import java.io.ByteArrayOutputStream

/** Mengubah lembar gambar dari AI menjadi stiker siap WhatsApp (512x512, WebP < 100 KB). */
object StickerMaker {

    const val SIZE = 512
    private const val OUTLINE = 10
    private const val MARGIN = 6
    private const val MAX_STICKER_BYTES = 100 * 1024
    private const val TRAY_SIZE = 96

    /**
     * Potong lembar menjadi [take] stiker pertama (urutan baca). Sel kosong dilewati.
     */
    fun slice(sheet: Bitmap, grid: GridSpec, take: Int): List<Bitmap> {
        val src = if (sheet.config == Bitmap.Config.ARGB_8888) sheet
        else sheet.copy(Bitmap.Config.ARGB_8888, false)
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)

        // buang bingkai/kotak tipis yang kadang digambar AI di sekeliling stiker
        StickerCore.removeFrameLines(px, w, h, (w / grid.cols * 0.45).toInt())
        val bg = StickerCore.backgroundMask(px, w, h)
        val xCuts = StickerCore.findCuts(bg, w, h, grid.cols, true)
        val yCuts = StickerCore.findCuts(bg, w, h, grid.rows, false)

        val result = ArrayList<Bitmap>()
        var index = 0
        loop@ for (r in 0 until grid.rows) {
            for (c in 0 until grid.cols) {
                if (index >= take) break@loop
                index++
                val piece = StickerCore.extract(
                    px, bg, w, xCuts[c], yCuts[r], xCuts[c + 1], yCuts[r + 1]
                ) ?: continue
                result.add(render(piece))
            }
        }
        return result
    }

    private fun render(piece: StickerCore.Piece): Bitmap {
        val content = Bitmap.createBitmap(piece.pixels, piece.width, piece.height, Bitmap.Config.ARGB_8888)
        val canvasBmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val inner = SIZE - 2 * (OUTLINE + MARGIN)
        val scale = minOf(inner.toFloat() / piece.width, inner.toFloat() / piece.height)
        val dw = (piece.width * scale).toInt().coerceAtLeast(1)
        val dh = (piece.height * scale).toInt().coerceAtLeast(1)
        val left = (SIZE - dw) / 2
        val top = (SIZE - dh) / 2
        Canvas(canvasBmp).drawBitmap(
            content, null, Rect(left, top, left + dw, top + dh),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        )
        content.recycle()

        val px = IntArray(SIZE * SIZE)
        canvasBmp.getPixels(px, 0, SIZE, 0, 0, SIZE, SIZE)
        StickerCore.addOutline(px, SIZE, SIZE, OUTLINE, 0xFFFFFF)
        canvasBmp.setPixels(px, 0, SIZE, 0, 0, SIZE, SIZE)
        return canvasBmp
    }

    /** WebP statis, kualitas diturunkan bertahap sampai di bawah 100 KB. */
    fun encodeWebp(bitmap: Bitmap): ByteArray {
        var quality = 92
        while (true) {
            val bos = ByteArrayOutputStream()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, bos)
            } else {
                @Suppress("DEPRECATION")
                bitmap.compress(Bitmap.CompressFormat.WEBP, quality, bos)
            }
            if (bos.size() <= MAX_STICKER_BYTES || quality <= 20) return bos.toByteArray()
            quality -= 8
        }
    }

    /** Ikon tray paket: PNG 96x96. */
    fun trayPng(firstSticker: Bitmap): ByteArray {
        val tray = Bitmap.createScaledBitmap(firstSticker, TRAY_SIZE, TRAY_SIZE, true)
        val bos = ByteArrayOutputStream()
        tray.compress(Bitmap.CompressFormat.PNG, 100, bos)
        return bos.toByteArray()
    }
}
