package com.lumi.stickerize.sticker

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class StickerEntry(val fileName: String, val emojis: List<String>, val caption: String)

data class StickerPack(
    val id: String,
    val name: String,
    val publisher: String,
    val trayFile: String,
    val version: Int,
    val stickers: List<StickerEntry>,
)

/** Menyimpan paket stiker di penyimpanan internal aplikasi agar bisa dibaca WhatsApp. */
object PackStore {

    const val TRAY_FILE = "tray.png"

    private val defaultEmojis = listOf("😀", "😂", "👍", "😍", "😮", "😭", "😠", "😴", "🤔", "🎉", "😊", "✨")

    private fun root(context: Context) = File(context.filesDir, "packs").apply { mkdirs() }

    fun packDir(context: Context, id: String): File = File(root(context), sanitize(id))

    private fun sanitize(id: String) = id.replace(Regex("[^A-Za-z0-9_.-]"), "_")

    fun save(
        context: Context,
        id: String,
        name: String,
        publisher: String,
        stickers: List<Bitmap>,
        captions: List<String>,
    ): StickerPack {
        val dir = packDir(context, id)
        dir.mkdirs()
        val oldVersion = load(context, id)?.version ?: 0

        val entries = stickers.mapIndexed { i, bmp ->
            val file = "sticker_${i + 1}.webp"
            File(dir, file).writeBytes(StickerMaker.encodeWebp(bmp))
            StickerEntry(file, listOf(defaultEmojis[i % defaultEmojis.size]), captions.getOrNull(i).orEmpty())
        }
        File(dir, TRAY_FILE).writeBytes(StickerMaker.trayPng(stickers.first()))

        val pack = StickerPack(
            id = id,
            name = name.take(128),
            publisher = publisher.take(128),
            trayFile = TRAY_FILE,
            version = oldVersion + 1,
            stickers = entries,
        )
        File(dir, "pack.json").writeText(toJson(pack).toString())
        return pack
    }

    /** Ganti satu stiker (mis. setelah dihapus manual). Versi paket dinaikkan agar WhatsApp memperbarui. */
    fun updateSticker(context: Context, id: String, index: Int, bitmap: Bitmap): StickerPack? {
        val pack = load(context, id) ?: return null
        val entry = pack.stickers.getOrNull(index) ?: return null
        val dir = packDir(context, id)
        File(dir, entry.fileName).writeBytes(StickerMaker.encodeWebp(bitmap))
        if (index == 0) File(dir, TRAY_FILE).writeBytes(StickerMaker.trayPng(bitmap))
        val updated = pack.copy(version = pack.version + 1)
        File(dir, "pack.json").writeText(toJson(updated).toString())
        return updated
    }

    fun rename(context: Context, id: String, newName: String): StickerPack? {
        val pack = load(context, id) ?: return null
        val updated = pack.copy(name = newName.take(128), version = pack.version + 1)
        File(packDir(context, id), "pack.json").writeText(toJson(updated).toString())
        return updated
    }

    fun load(context: Context, id: String): StickerPack? {
        val f = File(packDir(context, id), "pack.json")
        if (!f.exists()) return null
        return try {
            fromJson(JSONObject(f.readText()))
        } catch (e: Exception) {
            null
        }
    }

    fun all(context: Context): List<StickerPack> =
        root(context).listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { load(context, it.name) }
            ?.sortedByDescending { it.id }
            ?: emptyList()

    private fun toJson(p: StickerPack): JSONObject = JSONObject()
        .put("id", p.id)
        .put("name", p.name)
        .put("publisher", p.publisher)
        .put("tray", p.trayFile)
        .put("version", p.version)
        .put("stickers", JSONArray().apply {
            p.stickers.forEach { s ->
                put(
                    JSONObject()
                        .put("file", s.fileName)
                        .put("emojis", JSONArray(s.emojis))
                        .put("caption", s.caption)
                )
            }
        })

    private fun fromJson(o: JSONObject): StickerPack {
        val arr = o.getJSONArray("stickers")
        val list = (0 until arr.length()).map { i ->
            val s = arr.getJSONObject(i)
            val em = s.optJSONArray("emojis")
            StickerEntry(
                fileName = s.getString("file"),
                emojis = if (em == null) listOf("😀") else (0 until em.length()).map { em.getString(it) },
                caption = s.optString("caption"),
            )
        }
        return StickerPack(
            id = o.getString("id"),
            name = o.getString("name"),
            publisher = o.optString("publisher", "Lumi Stickerize"),
            trayFile = o.optString("tray", TRAY_FILE),
            version = o.optInt("version", 1),
            stickers = list,
        )
    }
}
