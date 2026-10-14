package com.lumi.stickerize.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiException(message: String) : Exception(message)

/**
 * Memanggil model gambar Gemini (mis. gemini-2.5-flash-image) sekali saja per pembuatan:
 * 3 foto referensi + prompt  ->  1 lembar gambar berisi semua stiker.
 */
class GeminiClient(private val apiKey: String, private val model: String) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .callTimeout(240, TimeUnit.SECONDS)
        .build()

    suspend fun generateSheet(
        prompt: String,
        jpegPhotos: List<ByteArray>,
        aspectRatio: String,
    ): Bitmap = withContext(Dispatchers.IO) {
        val parts = JSONArray()
        parts.put(JSONObject().put("text", prompt))
        jpegPhotos.forEach { bytes ->
            parts.put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", "image/jpeg")
                        .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
                )
            )
        }

        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseModalities", JSONArray().put("TEXT").put("IMAGE"))
                    .put("imageConfig", JSONObject().put("aspectRatio", aspectRatio))
            )

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/${model.trim()}:generateContent")
            .header("x-goog-api-key", apiKey.trim())
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val (code, responseText) = try {
            http.newCall(request).execute().use { resp ->
                Pair(resp.code, resp.body?.string().orEmpty())
            }
        } catch (e: IOException) {
            throw GeminiException("Gagal terhubung ke server AI. Cek koneksi internet kamu.\n(${e.message})")
        }

        if (code !in 200..299) throw GeminiException(describeHttpError(code, responseText))

        val json = try {
            JSONObject(responseText)
        } catch (e: Exception) {
            throw GeminiException("Balasan AI tidak bisa dibaca.")
        }

        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            val reason = json.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
            throw GeminiException(
                "AI menolak permintaan ini" + (if (reason.isNotEmpty()) " ($reason)" else "") +
                    ". Coba foto lain atau ubah kata-katanya."
            )
        }

        val candidate = candidates.getJSONObject(0)
        val outParts = candidate.optJSONObject("content")?.optJSONArray("parts")
        val modelText = StringBuilder()
        if (outParts != null) {
            for (i in 0 until outParts.length()) {
                val p = outParts.getJSONObject(i)
                val inline = p.optJSONObject("inlineData") ?: p.optJSONObject("inline_data")
                if (inline != null) {
                    val data = inline.optString("data")
                    if (data.isNotEmpty()) {
                        val bytes = Base64.decode(data, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp != null) return@withContext bmp
                    }
                }
                val t = p.optString("text")
                if (t.isNotEmpty()) modelText.append(t).append(' ')
            }
        }
        val finish = candidate.optString("finishReason")
        throw GeminiException(
            "AI tidak mengirim gambar" +
                (if (finish.isNotEmpty() && finish != "STOP") " ($finish)" else "") +
                ". " + modelText.toString().trim().take(300).ifEmpty { "Silakan coba lagi." }
        )
    }

    private fun describeHttpError(code: Int, body: String): String {
        val apiMsg = try {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        } catch (e: Exception) {
            ""
        }
        val friendly = when {
            code == 400 && apiMsg.contains("API key", ignoreCase = true) ->
                "API key tidak valid. Periksa lagi di Pengaturan."
            code == 400 -> "Permintaan ditolak oleh AI."
            code == 401 || code == 403 -> "API key tidak punya izin untuk model ini."
            code == 404 -> "Model \"$model\" tidak ditemukan. Ganti nama model di Pengaturan."
            code == 429 -> "Kuota AI habis / terlalu sering. Tunggu sebentar lalu coba lagi."
            code >= 500 -> "Server AI sedang sibuk. Coba lagi sebentar lagi."
            else -> "Terjadi kesalahan (kode $code)."
        }
        return if (apiMsg.isNotEmpty()) "$friendly\n\n$apiMsg" else friendly
    }
}
