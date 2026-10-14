package com.lumi.stickerize.ai

import com.lumi.stickerize.model.GridSpec
import com.lumi.stickerize.model.StickerStyle

object PromptBuilder {

    private val poses = listOf(
        "waving hello with a big happy smile",
        "laughing out loud with eyes squeezed shut",
        "giving an enthusiastic thumbs up",
        "making a heart shape with both hands, in love",
        "shocked with wide eyes and open mouth",
        "crying dramatically with big tears",
        "angry and pouting with puffed cheeks",
        "sleepy and yawning",
        "thinking with a hand on the chin",
        "jumping and celebrating with both arms raised",
    )

    /**
     * @param stickerCount jumlah stiker yang benar-benar dipakai (sudah min. 3)
     * @param captions kata untuk tiap stiker; kosong = tanpa kata
     */
    fun build(
        style: StickerStyle,
        grid: GridSpec,
        stickerCount: Int,
        captions: List<String>,
    ): String {
        val sb = StringBuilder()
        sb.appendLine(
            "Create ONE single image: a sticker sheet arranged as an invisible grid of " +
                "${grid.cols} columns × ${grid.rows} rows (${grid.cells} equal square cells)."
        )
        sb.appendLine(
            "The character is the person in the attached reference photos. Keep them clearly " +
                "recognisable: same face shape, hairstyle, hair colour, skin tone, glasses or " +
                "hijab/accessories if any, and a similar outfit. Use the same character in every cell."
        )
        sb.appendLine("Art style: ${style.prompt}.")
        sb.appendLine()
        sb.appendLine("Strict layout rules:")
        sb.appendLine("- Background must be pure flat white (#FFFFFF) everywhere. No scenery, no gradients, no shadows on the background.")
        sb.appendLine("- Do NOT draw grid lines, borders, frames, boxes or separators.")
        sb.appendLine("- Each sticker sits centred inside its own cell with clear white space (at least 10% padding) on every side. Nothing may touch or cross into a neighbouring cell.")
        sb.appendLine("- Show the character as a half-body or full-body pose with a clean, bold dark outline around the whole figure.")
        sb.appendLine("- No watermark, signature, logo or any text other than the captions listed below.")
        if (captions.isNotEmpty()) {
            sb.appendLine("- Captions: bold, rounded, very legible cartoon lettering with a dark outline, placed inside the same cell just below or beside the character. Spell every caption EXACTLY as written (keep the original language, do not translate).")
        }
        sb.appendLine()
        sb.appendLine("Cells, in reading order (left to right, top to bottom):")
        for (i in 0 until grid.cells) {
            val n = i + 1
            if (i >= stickerCount) {
                sb.appendLine("$n. Leave this cell completely empty (pure white).")
                continue
            }
            val caption = captions.getOrNull(i)?.trim().orEmpty()
            if (caption.isNotEmpty()) {
                sb.appendLine("$n. A pose and facial expression that matches the meaning of the caption. Caption: \"$caption\"")
            } else {
                sb.appendLine("$n. The character ${poses[i % poses.size]}. No text.")
            }
        }
        return sb.toString()
    }
}
