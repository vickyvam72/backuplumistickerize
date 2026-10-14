package com.lumi.stickerize.whatsapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.lumi.stickerize.BuildConfig

object WhatsAppHelper {

    private const val CONSUMER = "com.whatsapp"
    private const val BUSINESS = "com.whatsapp.w4b"
    private const val ACTION = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"

    private fun installed(context: Context, pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun isAnyInstalled(context: Context) = installed(context, CONSUMER) || installed(context, BUSINESS)

    /** Intent resmi untuk menampilkan dialog "Tambahkan ke WhatsApp". */
    fun addPackIntent(context: Context, packId: String, packName: String): Intent {
        val base = Intent(ACTION).apply {
            putExtra("sticker_pack_id", packId)
            putExtra("sticker_pack_authority", BuildConfig.CONTENT_PROVIDER_AUTHORITY)
            putExtra("sticker_pack_name", packName)
        }
        val hasConsumer = installed(context, CONSUMER)
        val hasBusiness = installed(context, BUSINESS)
        return when {
            hasConsumer && hasBusiness -> Intent.createChooser(base, "Tambahkan ke…")
            hasConsumer -> base.setPackage(CONSUMER)
            hasBusiness -> base.setPackage(BUSINESS)
            else -> base
        }
    }
}
