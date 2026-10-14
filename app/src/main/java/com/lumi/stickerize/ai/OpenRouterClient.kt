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

/**
 * Memanggil Image API OpenRouter (POST /api/v1/images) sekali saja per pembuatan:
 * 3 foto referensi + prompt  ->  1 lembar gambar berisi semua stiker.
 */
class OpenRouterClient(private val apiKey: String, private val model: String) : ImageGenerator {

    override val maxReferences: Int = 10

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(240, TimeUnit.SECONDS)
        .callTimeout(300, TimeUnit.SECONDS)
        .build()

    override suspend fun generateSheet(
        prompt: String,
        jpegPhotos: List<ByteArray>,
        aspectRatio: String,
    ): Bitmap = withContext(Dispatchers.IO) {
        val refs = JSONArray()
        jpegPhotos.forEach { bytes ->
            val dataUrl = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            refs.put(
                JSONObject()
                    .put("type", "image_url")
                    .put("image_url", JSONObject().put("url", dataUrl))
            )
        }

        val body = JSONObject()
            .put("model", model.trim())
            .put("prompt", prompt)
            .put("n", 1)
            .put("resolution", "1K")
            .put("aspect_ratio", aspectRatio)
            .put("input_references", refs)

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/images")
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("HTTP-Referer", "https://github.com/vickyvam72/backuplumistickerize")
            .header("X-Title", "Lumi Stickerize")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val (code, text) = try {
            http.newCall(request).execute().use { resp ->
                Pair(resp.code, resp.body?.string().orEmpty())
            }
        } catch (e: IOException) {
            throw AiException("Gagal terhubung ke server AI. Cek koneksi internet kamu.\n(${e.message})")
        }

        if (code !in 200..299) throw AiException(describeHttpError(code, text))

        val json = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw AiException("Balasan AI tidak bisa dibaca.")
        }

        json.optJSONObject("error")?.let { err ->
            throw AiException("AI gagal membuat gambar: " + err.optString("message"))
        }

        val data = json.optJSONArray("data")
        if (data != null) {
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val b64 = item.optString("b64_json")
                if (b64.isNotEmpty()) {
                    val bytes = Base64.decode(b64.substringAfter("base64,"), Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { return@withContext it }
                }
            }
        }
        throw AiException("AI tidak mengirim gambar. Silakan coba lagi.")
    }

    private fun describeHttpError(code: Int, body: String): String {
        val apiMsg = try {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        } catch (e: Exception) {
            ""
        }
        val friendly = when (code) {
            400 -> "Permintaan ditolak oleh AI (mungkin model ini tidak mendukung foto referensi)."
            401 -> "API key OpenRouter tidak valid. Periksa lagi di Pengaturan."
            402 -> "Saldo/kredit OpenRouter kamu habis. Isi ulang di openrouter.ai/credits."
            403 -> "Permintaan diblokir oleh filter keamanan AI. Coba foto atau kata lain."
            404 -> "Model \"$model\" tidak ditemukan di OpenRouter. Ganti model di Pengaturan."
            408 -> "AI terlalu lama merespons. Coba lagi."
            429 -> "Terlalu banyak permintaan. Tunggu sebentar lalu coba lagi."
            502, 503 -> "AI gagal membuat gambar kali ini (tidak dikenai biaya). Coba lagi."
            else -> "Terjadi kesalahan (kode $code)."
        }
        return if (apiMsg.isNotEmpty() && apiMsg != friendly) "$friendly\n\n$apiMsg" else friendly
    }
}
