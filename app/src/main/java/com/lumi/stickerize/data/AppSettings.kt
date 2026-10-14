package com.lumi.stickerize.data

import android.content.Context

/** Pengaturan sederhana yang disimpan di HP (tidak pernah dikirim ke mana-mana selain ke Google AI). */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("lumi_settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("api_key", "").orEmpty()
        set(v) = prefs.edit().putString("api_key", v.trim()).apply()

    var model: String
        get() = prefs.getString("model", DEFAULT_MODEL).orEmpty().ifBlank { DEFAULT_MODEL }
        set(v) = prefs.edit().putString("model", v.trim()).apply()

    var publisher: String
        get() = prefs.getString("publisher", "Lumi Stickerize").orEmpty().ifBlank { "Lumi Stickerize" }
        set(v) = prefs.edit().putString("publisher", v.trim()).apply()

    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash-image"
        const val API_KEY_URL = "https://aistudio.google.com/apikey"
    }
}
