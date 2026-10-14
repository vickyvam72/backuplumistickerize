package com.lumi.stickerize.data

import android.content.Context

/** Pilihan model gambar OpenRouter yang sudah diuji cocok untuk stiker. */
data class AiModel(val id: String, val label: String, val note: String)

/** Pengaturan sederhana yang disimpan di HP (key hanya dikirim ke OpenRouter). */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("lumi_settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("openrouter_key", "").orEmpty()
        set(v) = prefs.edit().putString("openrouter_key", v.trim()).apply()

    var model: String
        get() = prefs.getString("openrouter_model", DEFAULT_MODEL).orEmpty().ifBlank { DEFAULT_MODEL }
        set(v) = prefs.edit().putString("openrouter_model", v.trim()).apply()

    var publisher: String
        get() = prefs.getString("publisher", "Lumi Stickerize").orEmpty().ifBlank { "Lumi Stickerize" }
        set(v) = prefs.edit().putString("publisher", v.trim()).apply()

    companion object {
        const val DEFAULT_MODEL = "google/gemini-3.1-flash-lite-image"
        const val API_KEY_URL = "https://openrouter.ai/keys"

        val MODELS = listOf(
            AiModel(
                DEFAULT_MODEL,
                "Nano Banana 2 Lite",
                "Rekomendasi • murah, cepat, wajah mirip, teks rapi"
            ),
            AiModel(
                "bytedance-seed/seedream-5-0-flash",
                "Seedream 5.0 Flash",
                "Paling hemat • ±\$0,018 per pembuatan"
            ),
            AiModel(
                "google/gemini-3.1-flash-image",
                "Nano Banana 2",
                "Kualitas paling bagus • ±2x harga Lite"
            ),
        )
    }
}
