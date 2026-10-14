package com.lumi.stickerize.data

import android.content.Context
import com.lumi.stickerize.ai.CloudflareClient
import com.lumi.stickerize.ai.ImageGenerator
import com.lumi.stickerize.ai.OpenRouterClient

enum class Provider { OPENROUTER, CLOUDFLARE }

/** Pilihan model gambar yang sudah dicek cocok untuk stiker. */
data class AiModel(
    val id: String,
    val label: String,
    val note: String,
    val provider: Provider,
    val free: Boolean = false,
)

/** Pengaturan sederhana yang disimpan di HP (key hanya dikirim ke penyedia AI-nya). */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("lumi_settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("openrouter_key", "").orEmpty()
        set(v) = prefs.edit().putString("openrouter_key", v.trim()).apply()

    var cfAccountId: String
        get() = prefs.getString("cf_account", "").orEmpty()
        set(v) = prefs.edit().putString("cf_account", v.trim()).apply()

    var cfToken: String
        get() = prefs.getString("cf_token", "").orEmpty()
        set(v) = prefs.edit().putString("cf_token", v.trim()).apply()

    var model: String
        get() = prefs.getString("openrouter_model", DEFAULT_MODEL).orEmpty().ifBlank { DEFAULT_MODEL }
        set(v) = prefs.edit().putString("openrouter_model", v.trim()).apply()

    var publisher: String
        get() = prefs.getString("publisher", "Lumi Stickerize").orEmpty().ifBlank { "Lumi Stickerize" }
        set(v) = prefs.edit().putString("publisher", v.trim()).apply()

    val provider: Provider get() = providerOf(model)

    /** Kunci untuk penyedia yang dipakai sudah diisi? */
    val hasCredentials: Boolean
        get() = when (provider) {
            Provider.OPENROUTER -> apiKey.isNotBlank()
            Provider.CLOUDFLARE -> cfAccountId.isNotBlank() && cfToken.isNotBlank()
        }

    fun generator(): ImageGenerator = when (provider) {
        Provider.OPENROUTER -> OpenRouterClient(apiKey, model)
        Provider.CLOUDFLARE -> CloudflareClient(cfAccountId, cfToken, model)
    }

    companion object {
        const val DEFAULT_MODEL = "google/gemini-3.1-flash-lite-image"
        const val API_KEY_URL = "https://openrouter.ai/keys"
        const val CF_TOKEN_URL = "https://dash.cloudflare.com/profile/api-tokens"

        /** Model Cloudflare diawali "@cf/", selain itu dianggap model OpenRouter. */
        fun providerOf(model: String): Provider =
            if (model.trim().startsWith("@cf/")) Provider.CLOUDFLARE else Provider.OPENROUTER

        val MODELS = listOf(
            AiModel(
                DEFAULT_MODEL,
                "Nano Banana 2 Lite",
                "Rekomendasi • ±\$0,034 per pembuatan, wajah paling mirip, teks rapi",
                Provider.OPENROUTER
            ),
            AiModel(
                "bytedance-seed/seedream-5-0-flash",
                "Seedream 5.0 Flash",
                "Hemat • ±\$0,018 per pembuatan",
                Provider.OPENROUTER
            ),
            AiModel(
                "google/gemini-3.1-flash-image",
                "Nano Banana 2",
                "Kualitas terbaik • ±2x harga Lite",
                Provider.OPENROUTER
            ),
            AiModel(
                "@cf/black-forest-labs/flux-2-klein-9b",
                "FLUX.2 Klein 9B",
                "GRATIS ±6x per hari (Cloudflare) • kemiripan wajah lebih rendah",
                Provider.CLOUDFLARE,
                free = true
            ),
            AiModel(
                "@cf/black-forest-labs/flux-2-klein-4b",
                "FLUX.2 Klein 4B",
                "GRATIS ±80x per hari (Cloudflare) • cepat, kualitas standar",
                Provider.CLOUDFLARE,
                free = true
            ),
        )
    }
}
