package com.lumi.stickerize.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumi.stickerize.ai.OpenRouterClient
import com.lumi.stickerize.ai.PromptBuilder
import com.lumi.stickerize.data.AppSettings
import com.lumi.stickerize.model.GridSpec
import com.lumi.stickerize.model.StickerStyle
import com.lumi.stickerize.sticker.PackStore
import com.lumi.stickerize.sticker.StickerMaker
import com.lumi.stickerize.util.FaceFinder
import com.lumi.stickerize.util.ImageUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Step(val title: String, val subtitle: String) {
    PHOTOS("Pilih 3 foto", "Foto wajah yang jelas akan jadi referensi AI"),
    FACES("Pilih wajah", "Ketuk kotak wajah yang mau dijadikan stiker"),
    COUNT("Jumlah stiker", "Mau dibuatkan berapa stiker?"),
    STYLE("Pilih tema", "Gaya gambar stikermu"),
    TEXT("Tulisan", "Pakai kata di stiker atau tidak?"),
    GENERATE("Siap dibuat", "Periksa lagi sebelum AI bekerja"),
    RESULT("Stiker kamu", "Tambahkan langsung ke WhatsApp"),
}

/** Satu foto referensi beserta hasil deteksi wajahnya. */
class PhotoInfo(val bitmap: Bitmap, val image: ImageBitmap, val faces: List<Rect>)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val settings = AppSettings(app)

    var step by mutableStateOf(Step.PHOTOS)
        private set

    val photoUris = mutableStateListOf<Uri?>(null, null, null)
    val photos = mutableStateListOf<PhotoInfo?>(null, null, null)
    /** Indeks wajah yang dipilih di tiap foto (maks 2). */
    val selections = mutableStateListOf<Set<Int>>(emptySet(), emptySet(), emptySet())

    var count by mutableIntStateOf(5)
    var style by mutableStateOf<StickerStyle?>(null)
    var useText by mutableStateOf(false)
    val words = mutableStateListOf<String>().apply { repeat(MAX_STICKERS) { add("") } }

    var generating by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var notice by mutableStateOf<String?>(null)

    var sheet by mutableStateOf<ImageBitmap?>(null)
        private set
    var stickers by mutableStateOf<List<ImageBitmap>>(emptyList())
        private set
    var packId by mutableStateOf<String?>(null)
        private set
    var packName by mutableStateOf("")

    private var job: Job? = null

    val photosReady: Boolean get() = photoUris.all { it != null } && photos.all { it != null }

    /** Setiap foto punya minimal 1 wajah terpilih. */
    val facesReady: Boolean
        get() = photos.indices.all { i ->
            val p = photos[i]
            p != null && p.faces.isNotEmpty() && selections[i].isNotEmpty()
        }

    /** Jumlah orang di stiker = pilihan terbanyak di antara 3 foto (1 atau 2). */
    val peopleCount: Int get() = selections.maxOfOrNull { it.size }?.coerceIn(1, MAX_FACES) ?: 1
    val activeWords: List<String> get() = words.take(count)
    val wordsReady: Boolean get() = !useText || activeWords.all { it.isNotBlank() }


    fun canGoNext(): Boolean = when (step) {
        Step.PHOTOS -> photosReady
        Step.FACES -> facesReady
        Step.COUNT -> count in MIN_STICKERS..MAX_STICKERS
        Step.STYLE -> style != null
        Step.TEXT -> wordsReady
        else -> false
    }

    fun next() {
        if (!canGoNext()) return
        step = Step.entries[step.ordinal + 1]
    }

    fun back() {
        if (generating) return
        if (step.ordinal > 0 && step != Step.RESULT) step = Step.entries[step.ordinal - 1]
        else if (step == Step.RESULT) step = Step.GENERATE
    }

    fun goTo(target: Step) {
        if (!generating) step = target
    }

    fun setPhoto(index: Int, uri: Uri) {
        photoUris[index] = uri
        photos[index] = null
        selections[index] = emptySet()
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                runCatching { ImageUtils.loadScaled(getApplication(), uri, 1024) }.getOrNull()
            }
            if (photoUris[index] != uri) return@launch
            if (bmp == null) {
                photoUris[index] = null
                error = "Foto tidak bisa dibuka. Coba pilih foto lain."
                return@launch
            }
            val faces = FaceFinder.detect(bmp)
            if (photoUris[index] != uri) return@launch
            photos[index] = PhotoInfo(bmp, bmp.asImageBitmap(), faces)
            // 1 wajah = otomatis terpilih; lebih dari 1 = user yang memilih
            selections[index] = if (faces.size == 1) setOf(0) else emptySet()
        }
    }

    /** Ketuk kotak wajah: pilih / batal pilih. Maksimal 2 wajah per foto. */
    fun toggleFace(photo: Int, face: Int) {
        val cur = selections[photo]
        selections[photo] = when {
            face in cur -> cur - face
            cur.size >= MAX_FACES -> {
                error = "Maksimal hanya 2 wajah yang bisa dijadikan stiker."
                cur
            }
            else -> cur + face
        }
    }

    /** Pilih semua wajah di foto (hanya kalau jumlahnya ≤ 2). */
    fun selectAllFaces(photo: Int) {
        val p = photos[photo] ?: return
        if (p.faces.size > MAX_FACES) {
            error = "Foto ini punya ${p.faces.size} wajah. Maksimal hanya 2 wajah yang bisa dijadikan stiker, jadi pilih 2 saja."
            return
        }
        selections[photo] = p.faces.indices.toSet()
    }

    fun setPhotos(uris: List<Uri>) {
        // 3 foto sekaligus = ganti semua; kurang dari 3 = isi slot yang masih kosong dulu
        val targets = if (uris.size >= 3) listOf(0, 1, 2)
        else (0..2).filter { photoUris[it] == null }.ifEmpty { listOf(0, 1, 2) }
        uris.zip(targets).forEach { (uri, index) -> setPhoto(index, uri) }
    }

    fun clearPhoto(index: Int) {
        photoUris[index] = null
        photos[index] = null
        selections[index] = emptySet()
    }

    fun generate() {
        if (generating) return
        val apiKey = settings.apiKey
        if (apiKey.isBlank()) {
            error = "NEED_API_KEY"
            return
        }
        val chosenStyle = style ?: return
        val infos = photos.toList()
        if (infos.any { it == null } || !facesReady) return
        val chosen = selections.toList()
        val people = peopleCount

        generating = true
        error = null
        job = viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val total = count
                val grid = GridSpec.forCount(total)
                val captions = if (useText) activeWords.map { it.trim() } else emptyList()

                // wajah yang tidak dipilih disamarkan, lalu foto dipotong di sekitar wajah terpilih
                val refs = withContext(Dispatchers.Default) {
                    infos.mapIndexed { i, info ->
                        val ref = ImageUtils.buildReference(info!!.bitmap, info.faces, chosen[i])
                        ImageUtils.toJpeg(ref).also { ref.recycle() }
                    }
                }

                val prompt = PromptBuilder.build(chosenStyle, grid, total, captions, people)
                val sheetBmp: Bitmap = OpenRouterClient(apiKey, settings.model)
                    .generateSheet(prompt, refs, grid.aspectRatio)

                val pieces = withContext(Dispatchers.Default) {
                    StickerMaker.slice(sheetBmp, grid, total)
                }
                if (pieces.size < GridSpec.WHATSAPP_MIN) {
                    sheet = sheetBmp.asImageBitmap()
                    throw IllegalStateException(
                        "AI hanya menghasilkan ${pieces.size} stiker yang bisa dipotong " +
                            "(WhatsApp butuh minimal 3). Silakan coba buat ulang."
                    )
                }

                val id = "lumi_" + System.currentTimeMillis()
                val name = "Lumi ${chosenStyle.label} " +
                    SimpleDateFormat("d MMM HH.mm", Locale("id", "ID")).format(Date())
                withContext(Dispatchers.IO) {
                    PackStore.save(app, id, name, settings.publisher, pieces, captions)
                }

                sheet = sheetBmp.asImageBitmap()
                stickers = pieces.map { it.asImageBitmap() }
                packId = id
                packName = name
                notice = if (pieces.size < total) {
                    "Ada ${total - pieces.size} stiker yang tidak tergambar oleh AI, jadi hanya ${pieces.size} stiker yang dibuat."
                } else null
                step = Step.RESULT
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Terjadi kesalahan. Coba lagi."
            } finally {
                generating = false
            }
        }
    }

    fun cancel() {
        job?.cancel()
        generating = false
    }

    /** Simpan nama paket terbaru sebelum dikirim ke WhatsApp. */
    fun commitPackName(): Pair<String, String>? {
        val id = packId ?: return null
        val name = packName.trim().ifEmpty { "Lumi Stickerize" }
        PackStore.rename(getApplication(), id, name)
        return id to name
    }

    fun restart() {
        if (generating) return
        for (i in 0 until 3) clearPhoto(i)
        count = 5
        style = null
        useText = false
        for (i in words.indices) words[i] = ""
        sheet = null
        stickers = emptyList()
        packId = null
        packName = ""
        notice = null
        error = null
        step = Step.PHOTOS
    }

    companion object {
        /** WhatsApp mewajibkan minimal 3 stiker per paket. */
        const val MIN_STICKERS = GridSpec.WHATSAPP_MIN
        const val MAX_STICKERS = 10
        const val MAX_FACES = 2
    }
}
