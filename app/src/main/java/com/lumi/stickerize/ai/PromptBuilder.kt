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
        people: Int = 1,
    ): String {
        val sb = StringBuilder()
        sb.appendLine(
            "Create ONE single image: a sticker sheet arranged as an invisible grid of " +
                "${grid.cols} columns × ${grid.rows} rows (${grid.cells} equal square cells)."
        )
        if (people >= 2) {
            sb.appendLine(
                "REFERENCE IMAGES: the FIRST reference image shows two people together — PERSON A is " +
                    "the one on the LEFT, PERSON B is the one on the RIGHT. The SECOND reference image " +
                    "shows only PERSON A. The THIRD reference image shows only PERSON B. Any further " +
                    "reference images show them again in the same outfits. Pixelated faces belong to " +
                    "other people — ignore them."
            )
            sb.appendLine(
                "CHARACTERS: draw exactly two DIFFERENT characters, PERSON A and PERSON B. Each keeps " +
                    "their OWN face, gender, hairstyle or hijab, skin tone, accessories and outfit exactly " +
                    "as in their own reference image. NEVER draw the same person twice, never clone or " +
                    "mirror one person, never swap their clothes. Every sticker contains exactly one " +
                    "PERSON A (left) and exactly one PERSON B (right), side by side."
            )
        } else {
            sb.appendLine(
                "REFERENCE IMAGES: the FIRST reference image shows the person to draw. Pixelated faces " +
                    "belong to other people — ignore them. The other reference images show the same " +
                    "person again; if other people appear there, ignore them too."
            )
            sb.appendLine(
                "CHARACTER: exactly ONE character in every sticker — this person. Keep them clearly " +
                    "recognisable: same face shape, hairstyle or hijab, hair colour, skin tone, glasses " +
                    "or accessories, and the same outfit as in the photos. Never draw a second person."
            )
        }
        sb.appendLine("Art style: ${style.prompt}.")
        sb.appendLine()
        sb.appendLine("Strict layout rules:")
        sb.appendLine("- Background must be pure flat white (#FFFFFF) everywhere. No scenery, no gradients, no shadows on the background.")
        sb.appendLine("- Do NOT draw grid lines, borders, frames, boxes, cards, panels or separators. Stickers float directly on the white background, never inside a rectangle.")
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
                val who = if (people >= 2) "PERSON A and PERSON B together" else "The character"
                sb.appendLine("$n. $who, with a pose and facial expression that matches the meaning of the caption. Caption: \"$caption\"")
            } else {
                val who = if (people >= 2) "PERSON A and PERSON B together, both" else "The character"
                sb.appendLine("$n. $who ${poses[i % poses.size]}. No text.")
            }
        }
        if (people >= 2) {
            sb.appendLine()
            sb.appendLine("Final check: in EVERY cell PERSON A and PERSON B must clearly be two different people (different faces and outfits, as in their own reference images). No duplicated person anywhere.")
        }
        return sb.toString()
    }
}
