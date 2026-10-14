package com.lumi.stickerize.sticker

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.lumi.stickerize.BuildConfig
import java.io.File

/**
 * ContentProvider dengan kontrak yang diharapkan WhatsApp
 * (sesuai contoh resmi WhatsApp/stickers untuk Android).
 */
class StickerContentProvider : ContentProvider() {

    companion object {
        private const val METADATA = "metadata"
        private const val STICKERS = "stickers"
        private const val STICKERS_ASSET = "stickers_asset"

        private const val CODE_ALL_PACKS = 1
        private const val CODE_ONE_PACK = 2
        private const val CODE_STICKERS = 3
        private const val CODE_ASSET = 4

        private val PACK_COLUMNS = arrayOf(
            "sticker_pack_identifier",
            "sticker_pack_name",
            "sticker_pack_publisher",
            "sticker_pack_icon",
            "android_play_store_link",
            "ios_app_download_link",
            "sticker_pack_publisher_email",
            "sticker_pack_publisher_website",
            "sticker_pack_privacy_policy_website",
            "sticker_pack_license_agreement_website",
            "image_data_version",
            "whatsapp_will_not_cache_stickers",
            "animated_sticker_pack",
        )

        private val STICKER_COLUMNS = arrayOf(
            "sticker_file_name",
            "sticker_emoji",
            "sticker_accessibility_text",
        )
    }

    private val authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY
    private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(authority, METADATA, CODE_ALL_PACKS)
        addURI(authority, "$METADATA/*", CODE_ONE_PACK)
        addURI(authority, "$STICKERS/*", CODE_STICKERS)
        addURI(authority, "$STICKERS_ASSET/*/*", CODE_ASSET)
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val ctx = context ?: return null
        return when (matcher.match(uri)) {
            CODE_ALL_PACKS -> packCursor(PackStore.all(ctx))
            CODE_ONE_PACK -> packCursor(listOfNotNull(PackStore.load(ctx, uri.lastPathSegment.orEmpty())))
            CODE_STICKERS -> {
                val pack = PackStore.load(ctx, uri.lastPathSegment.orEmpty())
                val cursor = MatrixCursor(STICKER_COLUMNS)
                pack?.stickers?.forEach { s ->
                    cursor.addRow(arrayOf<Any>(s.fileName, s.emojis.joinToString(","), s.caption))
                }
                cursor
            }
            else -> throw IllegalArgumentException("URI tidak dikenal: $uri")
        }
    }

    private fun packCursor(packs: List<StickerPack>): Cursor {
        val cursor = MatrixCursor(PACK_COLUMNS)
        packs.forEach { p ->
            cursor.addRow(
                arrayOf<Any>(
                    p.id,
                    p.name,
                    p.publisher,
                    p.trayFile,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    p.version.toString(),
                    0,
                    0,
                )
            )
        }
        return cursor
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        if (matcher.match(uri) != CODE_ASSET) return null
        val ctx = context ?: return null
        val segments = uri.pathSegments
        if (segments.size != 3) return null
        val packId = segments[1]
        val fileName = segments[2]
        if (fileName.contains("/") || fileName.contains("..")) return null
        val file = File(PackStore.packDir(ctx, packId), fileName)
        if (!file.exists()) return null
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    override fun getType(uri: Uri): String? = when (matcher.match(uri)) {
        CODE_ALL_PACKS -> "vnd.android.cursor.dir/vnd.$authority.$METADATA"
        CODE_ONE_PACK -> "vnd.android.cursor.item/vnd.$authority.$METADATA"
        CODE_STICKERS -> "vnd.android.cursor.dir/vnd.$authority.$STICKERS"
        CODE_ASSET -> if (uri.lastPathSegment.orEmpty().endsWith(".png")) "image/png" else "image/webp"
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Tidak didukung")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Tidak didukung")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("Tidak didukung")
}
