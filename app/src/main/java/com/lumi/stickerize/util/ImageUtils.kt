package com.lumi.stickerize.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

object ImageUtils {

    /** Muat foto dari galeri, perkecil (hemat token & memori) dan putar sesuai EXIF. */
    fun loadScaled(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDim || bounds.outHeight / (sample * 2) >= maxDim) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null

        val rotation = try {
            resolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }

        val longest = maxOf(bmp.width, bmp.height)
        val scale = if (longest > maxDim) maxDim.toFloat() / longest else 1f
        if (rotation != 0f || scale < 1f) {
            val m = Matrix().apply {
                if (scale < 1f) postScale(scale, scale)
                if (rotation != 0f) postRotate(rotation)
            }
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (rotated != bmp) bmp.recycle()
            bmp = rotated
        }
        return bmp
    }

    /**
     * Siapkan foto referensi untuk AI: wajah yang TIDAK dipilih disamarkan (mozaik),
     * lalu foto dipotong di sekitar wajah yang dipilih (sampai badan, agar outfit terlihat).
     */
    fun buildReference(source: Bitmap, faces: List<Rect>, selected: Set<Int>): Bitmap {
        val bmp = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bmp)

        faces.forEachIndexed { i, face ->
            if (i in selected) return@forEachIndexed
            val pad = (face.width() * 0.2f).toInt()
            val r = Rect(
                (face.left - pad).coerceAtLeast(0),
                (face.top - pad).coerceAtLeast(0),
                (face.right + pad).coerceAtMost(bmp.width),
                (face.bottom + pad).coerceAtMost(bmp.height),
            )
            if (r.width() < 2 || r.height() < 2) return@forEachIndexed
            val region = Bitmap.createBitmap(bmp, r.left, r.top, r.width(), r.height())
            val tiny = Bitmap.createScaledBitmap(region, 6, 6, true)
            val mosaic = Bitmap.createScaledBitmap(tiny, r.width(), r.height(), false)
            canvas.drawBitmap(mosaic, r.left.toFloat(), r.top.toFloat(), null)
            region.recycle(); tiny.recycle(); mosaic.recycle()
        }

        val chosen = faces.filterIndexed { i, _ -> i in selected }
        if (chosen.isEmpty()) return bmp
        val union = Rect(chosen[0])
        chosen.forEach { union.union(it) }
        val fw = chosen.map { it.width() }.average().toFloat()
        val fh = chosen.map { it.height() }.average().toFloat()
        val crop = Rect(
            (union.left - fw * 1.4f).toInt().coerceAtLeast(0),
            (union.top - fh * 0.8f).toInt().coerceAtLeast(0),
            (union.right + fw * 1.4f).toInt().coerceAtMost(bmp.width),
            (union.bottom + fh * 3.2f).toInt().coerceAtMost(bmp.height),
        )
        if (crop.width() < 32 || crop.height() < 32) return bmp
        val out = Bitmap.createBitmap(bmp, crop.left, crop.top, crop.width(), crop.height())
        if (out != bmp) bmp.recycle()
        return out
    }

    fun toJpeg(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
        return bos.toByteArray()
    }
}
