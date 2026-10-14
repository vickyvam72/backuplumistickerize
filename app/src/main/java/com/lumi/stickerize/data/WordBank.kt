package com.lumi.stickerize.data

import android.content.Context

/** 1.600+ pilihan kata stiker dari assets/kata_stiker.txt, untuk tombol acak. */
object WordBank {

    @Volatile
    private var words: List<String>? = null

    fun all(context: Context): List<String> = words ?: synchronized(this) {
        words ?: context.assets.open("kata_stiker.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.length <= 25 }
                .distinct()
                .toList()
        }.also { words = it }
    }

    /** Ambil satu kata acak yang belum dipakai di stiker lain. */
    fun random(context: Context, exclude: Collection<String> = emptyList()): String {
        val list = all(context)
        val used = exclude.map { it.trim().lowercase() }.toSet()
        val pool = list.filter { it.lowercase() !in used }
        return (pool.ifEmpty { list }).random()
    }
}
