package com.lumi.stickerize.util

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

/** Deteksi wajah di foto memakai ML Kit (berjalan di HP, tanpa internet). */
object FaceFinder {

    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setMinFaceSize(0.06f)
                .build()
        )
    }

    /** Kotak wajah dalam koordinat bitmap, diurutkan dari kiri ke kanan. Maks 6 wajah terbesar. */
    suspend fun detect(bitmap: Bitmap): List<Rect> = try {
        detector.process(InputImage.fromBitmap(bitmap, 0)).await()
            .map { face ->
                val b = face.boundingBox
                Rect(
                    b.left.coerceIn(0, bitmap.width - 1),
                    b.top.coerceIn(0, bitmap.height - 1),
                    b.right.coerceIn(1, bitmap.width),
                    b.bottom.coerceIn(1, bitmap.height),
                )
            }
            .filter { it.width() >= bitmap.width * 0.04f && it.height() > 0 }
            .sortedByDescending { it.width() * it.height() }
            .take(6)
            .sortedBy { it.left }
    } catch (e: Exception) {
        emptyList()
    }
}
