package com.lumi.stickerize.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumi.stickerize.R
import com.lumi.stickerize.data.AppSettings
import com.lumi.stickerize.data.Provider
import com.lumi.stickerize.model.GridSpec
import com.lumi.stickerize.model.StickerStyle
import com.lumi.stickerize.ui.theme.LumiBlue
import com.lumi.stickerize.ui.theme.LumiGradient
import com.lumi.stickerize.ui.theme.LumiTile
import com.lumi.stickerize.ui.theme.WhatsAppGreen
import com.lumi.stickerize.whatsapp.WhatsAppHelper
import kotlinx.coroutines.delay

private const val NEED_API_KEY = "NEED_API_KEY"

@Composable
fun LumiApp(vm: MainViewModel) {
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(vm.error) {
        if (vm.error == NEED_API_KEY) {
            vm.error = null
            showSettings = true
        }
    }

    BackHandler(enabled = vm.step != Step.PHOTOS && !vm.generating) { vm.back() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Header(step = vm.step, onSettings = { showSettings = true })

        Box(Modifier.weight(1f)) {
            AnimatedContent(targetState = vm.step, label = "step") { step ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    when (step) {
                        Step.PHOTOS -> PhotosStep(vm)
                        Step.FACES -> FacesStep(vm)
                        Step.COUNT -> CountStep(vm)
                        Step.STYLE -> StyleStep(vm)
                        Step.TEXT -> TextStep(vm)
                        Step.GENERATE -> GenerateStep(vm)
                        Step.RESULT -> ResultStep(vm)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        BottomBar(vm)
    }

    if (showSettings) {
        SettingsDialog(settings = vm.settings, onClose = { showSettings = false })
    }

    val err = vm.error
    if (err != null && err != NEED_API_KEY) {
        AlertDialog(
            onDismissRequest = { vm.error = null },
            confirmButton = { TextButton(onClick = { vm.error = null }) { Text("Oke") } },
            title = { Text("Ups!") },
            text = { Text(err) },
        )
    }
}

/* ---------------------------------------------------------------- header & navigasi */

@Composable
private fun Header(step: Step, onSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(LumiGradient)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Lumi Stickerize",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Pengaturan", tint = Color.White)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (step == Step.RESULT) "Selesai" else "Langkah ${step.ordinal + 1} dari ${Step.entries.size - 1}",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp
        )
        Text(step.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Text(step.subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { (step.ordinal + 1) / Step.entries.size.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp)
                .clip(CircleShape),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.3f),
        )
    }
}

@Composable
private fun BottomBar(vm: MainViewModel) {
    if (vm.step == Step.RESULT) return
    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (vm.step != Step.PHOTOS) {
                OutlinedButton(
                    onClick = { vm.back() },
                    enabled = !vm.generating,
                    modifier = Modifier.height(52.dp)
                ) { Text("Kembali") }
            }
            if (vm.step == Step.GENERATE) {
                if (vm.generating) {
                    OutlinedButton(
                        onClick = { vm.cancel() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) { Text("Batalkan") }
                } else {
                    Button(
                        onClick = { vm.generate() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) { Text("✨  Buat Stiker", fontWeight = FontWeight.Bold) }
                }
            } else {
                Button(
                    onClick = { vm.next() },
                    enabled = vm.canGoNext(),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) { Text("Lanjut", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/* ---------------------------------------------------------------- langkah 1: foto */

@Composable
private fun PhotosStep(vm: MainViewModel) {
    var pendingIndex by remember { mutableIntStateOf(0) }
    val pickOne = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setPhoto(pendingIndex, uri)
    }
    val pickMany = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris ->
        if (uris.isNotEmpty()) vm.setPhotos(uris)
    }
    val imagesOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    WarningCard(
        "Penting: outfit harus sama",
        "Pastikan ketiga foto memakai pakaian/outfit yang SAMA (baju, hijab, aksesoris). " +
            "Kalau outfit berbeda-beda, AI bisa bingung dan hasil stiker jadi tidak konsisten."
    )
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in 0 until 3) {
            PhotoSlot(
                index = i,
                thumb = vm.photos[i]?.image,
                loading = vm.photoUris[i] != null && vm.photos[i] == null,
                onPick = {
                    pendingIndex = i
                    pickOne.launch(imagesOnly)
                },
                onClear = { vm.clearPhoto(i) },
                modifier = Modifier.weight(1f)
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    OutlinedButton(
        onClick = { pickMany.launch(imagesOnly) },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Pilih 3 foto sekaligus")
    }
    Spacer(Modifier.height(20.dp))
    TipCard(
        "Tips foto terbaik",
        listOf(
            "Outfit sama di ketiga foto",
            "Wajah terlihat jelas & terang",
            "Foto 1 dipakai untuk memilih wajah: pastikan semua orang yang mau dijadikan stiker ada di foto 1 (maks. 2 orang)",
            "Campur sudut: depan, samping, setengah badan",
        )
    )
}

@Composable
private fun PhotoSlot(
    index: Int,
    thumb: ImageBitmap?,
    loading: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(18.dp))
            .background(LumiTile)
            .border(
                BorderStroke(
                    2.dp,
                    if (thumb != null) LumiBlue else MaterialTheme.colorScheme.outline
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center
    ) {
        when {
            thumb != null -> {
                Image(
                    bitmap = thumb,
                    contentDescription = "Foto ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onClear),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Hapus",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            loading -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = LumiBlue)
                Spacer(Modifier.height(4.dp))
                Text("Foto ${index + 1}", color = LumiBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/* ---------------------------------------------------------------- langkah 2: wajah */

private val faceColors = listOf(Color(0xFF3E84E8), Color(0xFFE8673E))

@Composable
private fun FacesStep(vm: MainViewModel) {
    InfoCard("Wajah di foto pertama sudah dideteksi otomatis. Ketuk kotak di wajah untuk memilih siapa yang dijadikan stiker. Maksimal hanya 2 wajah.")
    Spacer(Modifier.height(14.dp))
    val info = vm.photos[0] ?: return
    val sel = vm.selectedFaces
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Foto 1", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(
            if (info.faces.isEmpty()) "Tidak ada wajah" else "${info.faces.size} wajah • ${sel.size} dipilih",
            fontSize = 13.sp,
            color = if (info.faces.isEmpty()) Color(0xFFD14343) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(6.dp))
    FacePhoto(info = info, selected = sel, onToggle = { vm.toggleFace(it) })
    Spacer(Modifier.height(8.dp))
    when {
        info.faces.isEmpty() -> Text(
            "Wajah tidak terdeteksi. Kembali dan ganti foto 1 dengan foto yang wajahnya terlihat jelas.",
            fontSize = 13.sp,
            color = Color(0xFFD14343)
        )
        info.faces.size == 2 && sel.size < 2 -> OutlinedButton(
            onClick = { vm.selectAllFaces() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Pilih keduanya (stiker berdua)") }
        info.faces.size > 2 -> Text(
            "Ada ${info.faces.size} wajah. Pilih maksimal 2.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        sel.isEmpty() -> Text("Ketuk wajah yang mau dijadikan stiker.", fontSize = 13.sp, color = LumiBlue)
    }
    if (sel.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        InfoCard(
            if (sel.size >= 2) "Stiker akan berisi 2 orang: orang bertanda biru (kiri) dan oranye (kanan). Foto 2 & 3 dipakai sebagai referensi tambahan."
            else "Stiker akan berisi 1 orang. Foto 2 & 3 dipakai sebagai referensi tambahan untuk orang yang sama."
        )
    }
}

@Composable
private fun FacePhoto(info: PhotoInfo, selected: Set<Int>, onToggle: (Int) -> Unit) {
    val bw = info.bitmap.width
    val bh = info.bitmap.height
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(bw.toFloat() / bh.toFloat())
            .clip(RoundedCornerShape(16.dp))
            .background(LumiTile)
    ) {
        val scale = maxWidth / bw.toFloat()
        Image(
            bitmap = info.image,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        info.faces.forEachIndexed { idx, face ->
            val isSel = idx in selected
            val order = selected.sorted().indexOf(idx)
            val color = if (isSel) faceColors[order.coerceIn(0, 1)] else Color.White
            Box(
                Modifier
                    .offset(x = scale * face.left.toFloat(), y = scale * face.top.toFloat())
                    .size(width = scale * face.width().toFloat(), height = scale * face.height().toFloat())
                    .border(if (isSel) 3.dp else 2.dp, color, RoundedCornerShape(8.dp))
                    .background(if (isSel) Color.Transparent else Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .clickable { onToggle(idx) }
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-6).dp, y = (-6).dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSel) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("${idx + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LumiBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun WarningCard(title: String, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF4E0))
            .border(1.dp, Color(0xFFF2B54A), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text("⚠️", fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF7A4B00))
            Spacer(Modifier.height(2.dp))
            Text(text, fontSize = 13.sp, color = Color(0xFF7A4B00))
        }
    }
}

/* ---------------------------------------------------------------- langkah 3: jumlah */

@Composable
private fun CountStep(vm: MainViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RoundButton("−", enabled = vm.count > MainViewModel.MIN_STICKERS) { vm.count-- }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${vm.count}", fontSize = 64.sp, fontWeight = FontWeight.Black, color = LumiBlue)
                Text("stiker", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RoundButton("+", enabled = vm.count < MainViewModel.MAX_STICKERS) { vm.count++ }
        }
    }
    Spacer(Modifier.height(16.dp))
    (MainViewModel.MIN_STICKERS..MainViewModel.MAX_STICKERS).chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { n ->
                val selected = vm.count == n
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) LumiBlue else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (selected) LumiBlue else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .clickable { vm.count = n },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$n",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
    Spacer(Modifier.height(6.dp))
    InfoCard("Bisa 3 sampai 10 stiker. Semuanya dibuat dalam 1 gambar AI, jadi biayanya sama saja.")
}

@Composable
private fun RoundButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = Modifier.size(56.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = LumiBlue
        )
    ) { Text(label, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
}

/* ---------------------------------------------------------------- langkah 3: tema */

@Composable
private fun StyleStep(vm: MainViewModel) {
    StickerStyle.entries.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { s ->
                val selected = vm.style == s
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .border(
                            if (selected) 2.dp else 1.dp,
                            if (selected) LumiBlue else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { vm.style = s }
                        .padding(vertical = 18.dp, horizontal = 12.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(s.emoji, fontSize = 34.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(s.label, fontWeight = FontWeight.Bold)
                    }
                    if (selected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = LumiBlue,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                        )
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
    }
}

/* ---------------------------------------------------------------- langkah 4: kata */

private val wordIdeas = listOf(
    "Makasih!", "Siap!", "Wkwkwk", "OTW", "Semangat!",
    "Maaf ya", "Mantap", "Hehe", "Gas!", "Oke deh",
)

@Composable
private fun TextStep(vm: MainViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard("🙂", "Tanpa kata", "Hanya ekspresi", !vm.useText, Modifier.weight(1f)) { vm.useText = false }
        ChoiceCard("💬", "Pakai kata", "${vm.count} kata", vm.useText, Modifier.weight(1f)) { vm.useText = true }
    }
    if (vm.useText) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Tulis ${vm.count} kata (1 per stiker)",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = { vm.randomAllWords() }) { Text("🎲 Acak semua") }
        }
        Spacer(Modifier.height(4.dp))
        for (i in 0 until vm.count) {
            OutlinedTextField(
                value = vm.words[i],
                onValueChange = { vm.words[i] = it.take(25) },
                label = { Text("Stiker ${i + 1}") },
                placeholder = { Text(wordIdeas[i % wordIdeas.size]) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { vm.randomWord(i) }) {
                        Text("🎲", fontSize = 20.sp)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = if (i == vm.count - 1) ImeAction.Done else ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Tekan 🎲 untuk kata acak dari 1.600+ pilihan. Kata pendek (1–3 kata) paling rapi hasilnya.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChoiceCard(
    emoji: String,
    title: String,
    caption: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) LumiBlue else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 32.sp)
        Spacer(Modifier.height(6.dp))
        Text(title, fontWeight = FontWeight.Bold)
        Text(caption, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/* ---------------------------------------------------------------- langkah 5: buat */

private val loadingLines = listOf(
    "Mempelajari wajah dari 3 foto…",
    "Menggambar ekspresi lucu…",
    "Menyusun semua stiker dalam 1 halaman…",
    "Hampir selesai, sabar ya…",
)

@Composable
private fun GenerateStep(vm: MainViewModel) {
    if (vm.generating) {
        var line by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(3500)
                line = (line + 1) % loadingLines.size
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(Modifier.size(64.dp), strokeWidth = 5.dp)
            Spacer(Modifier.height(24.dp))
            Text(loadingLines[line], fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Biasanya 15–60 detik. Jangan tutup aplikasi.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.photos.map { it?.image }.forEach { t ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LumiTile)
                    ) {
                        if (t != null) {
                            Image(t, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            SummaryRow(
                "Wajah",
                if (vm.peopleCount >= 2) "2 orang (stiker berdua)" else "1 orang",
                Step.FACES,
                vm
            )
            HorizontalDivider()
            SummaryRow("Jumlah", "${vm.count} stiker", Step.COUNT, vm)
            HorizontalDivider()
            SummaryRow("Tema", vm.style?.let { "${it.emoji} ${it.label}" } ?: "-", Step.STYLE, vm)
            HorizontalDivider()
            SummaryRow(
                "Tulisan",
                if (vm.useText) vm.activeWords.joinToString(" • ") else "Tanpa kata",
                Step.TEXT,
                vm
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    InfoCard("AI membuat semua stiker dalam 1 gambar saja (hemat token), lalu aplikasi memotongnya otomatis.")
}

@Composable
private fun SummaryRow(label: String, value: String, target: Step, vm: MainViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { vm.goTo(target) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(value, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("Ubah", color = LumiBlue, fontSize = 13.sp)
    }
}

/* ---------------------------------------------------------------- hasil */

@Composable
private fun ResultStep(vm: MainViewModel) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Int?>(null) }
    editing?.let { idx ->
        val original = vm.stickerBitmaps.getOrNull(idx)
        if (original != null) {
            EraserDialog(
                index = idx,
                original = original,
                onDismiss = { editing = null },
                onSave = { edited ->
                    vm.updateSticker(idx, edited)
                    editing = null
                }
            )
        }
    }
    val waLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            vm.notice = "Paket berhasil ditambahkan ke WhatsApp 🎉"
        } else {
            val reason = res.data?.getStringExtra("validation_error")
            if (reason != null) vm.error = "WhatsApp menolak paket ini:\n$reason"
        }
    }

    vm.notice?.let {
        InfoCard(it)
        Spacer(Modifier.height(12.dp))
    }

    Text(
        "✏️ Ketuk stiker untuk merapikan / menghapus bagian yang tidak rapi",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    vm.stickers.withIndex().toList().chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (index, bmp) ->
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(LumiTile)
                        .clickable { editing = index }
                        .padding(6.dp)
                ) {
                    Image(bmp, contentDescription = "Stiker ${index + 1}", modifier = Modifier.fillMaxSize())
                    Text(
                        "✏️",
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                            .padding(4.dp)
                    )
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(10.dp))
    }

    TextButton(onClick = { showSheet = !showSheet }) {
        Text(if (showSheet) "Sembunyikan gambar asli AI" else "Lihat gambar asli dari AI")
    }
    if (showSheet) {
        vm.sheet?.let {
            Image(
                it,
                contentDescription = "Gambar asli",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = vm.packName,
        onValueChange = { vm.packName = it.take(60) },
        label = { Text("Nama paket stiker") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = {
            if (!WhatsAppHelper.isAnyInstalled(context)) {
                vm.error = "WhatsApp belum terpasang di HP ini."
                return@Button
            }
            val (id, name) = vm.commitPackName() ?: return@Button
            try {
                waLauncher.launch(WhatsAppHelper.addPackIntent(context, id, name))
            } catch (e: ActivityNotFoundException) {
                vm.error = "Versi WhatsApp kamu belum mendukung stiker dari aplikasi lain. Coba perbarui WhatsApp."
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text("Tambahkan ke WhatsApp", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = {
                vm.goTo(Step.GENERATE)
                vm.generate()
            },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Buat ulang")
        }
        OutlinedButton(
            onClick = { vm.restart() },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) { Text("Mulai baru") }
    }
}

/* ---------------------------------------------------------------- komponen kecil */

@Composable
private fun InfoCard(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(14.dp)
    ) {
        Text("💡", fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun TipCard(title: String, items: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        items.forEach {
            Row(Modifier.padding(vertical = 3.dp)) {
                Text("•  ", color = LumiBlue, fontWeight = FontWeight.Bold)
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsDialog(settings: AppSettings, onClose: () -> Unit) {
    var key by remember { mutableStateOf(settings.apiKey) }
    var cfAccount by remember { mutableStateOf(settings.cfAccountId) }
    var cfToken by remember { mutableStateOf(settings.cfToken) }
    var model by remember { mutableStateOf(settings.model) }
    var publisher by remember { mutableStateOf(settings.publisher) }
    var reveal by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val provider = AppSettings.providerOf(model)
    val hidden = if (reveal) VisualTransformation.None else PasswordVisualTransformation()

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Pengaturan AI") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Pilih model AI", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                AppSettings.MODELS.forEach { m ->
                    val selected = model == m.id
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                if (selected) 2.dp else 0.dp,
                                if (selected) LumiBlue else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { model = m.id }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(m.label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (m.free) {
                                Text(
                                    "GRATIS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(WhatsAppGreen)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(m.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("ID model (bisa diisi manual)") },
                    supportingText = { Text("Diawali @cf/ = Cloudflare, lainnya = OpenRouter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                if (provider == Provider.OPENROUTER) {
                    Text("OpenRouter (berbayar)", fontWeight = FontWeight.Bold)
                    Text(
                        "Masukkan API key OpenRouter (diawali sk-or-). Isi kredit di openrouter.ai/credits.",
                        fontSize = 13.sp
                    )
                    TextButton(onClick = { uri.openUri(AppSettings.API_KEY_URL) }) {
                        Text("Buat API key di openrouter.ai →")
                    }
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("OpenRouter API key") },
                        placeholder = { Text("sk-or-v1-…") },
                        singleLine = true,
                        visualTransformation = hidden,
                        trailingIcon = {
                            TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Tutup" else "Lihat") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Cloudflare Workers AI (gratis)", fontWeight = FontWeight.Bold)
                    Text(
                        "1) Daftar gratis di dash.cloudflare.com\n" +
                            "2) Salin Account ID (ada di halaman Workers AI / URL dashboard)\n" +
                            "3) Buat API Token dengan template \"Workers AI\"\n" +
                            "Kuota gratis reset tiap hari jam 07.00 WIB.",
                        fontSize = 13.sp
                    )
                    TextButton(onClick = { uri.openUri(AppSettings.CF_TOKEN_URL) }) {
                        Text("Buat API Token Cloudflare →")
                    }
                    OutlinedTextField(
                        value = cfAccount,
                        onValueChange = { cfAccount = it },
                        label = { Text("Account ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = cfToken,
                        onValueChange = { cfToken = it },
                        label = { Text("API Token") },
                        singleLine = true,
                        visualTransformation = hidden,
                        trailingIcon = {
                            TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Tutup" else "Lihat") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = publisher,
                    onValueChange = { publisher = it.take(60) },
                    label = { Text("Nama pembuat (tampil di WhatsApp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                settings.apiKey = key
                settings.cfAccountId = cfAccount
                settings.cfToken = cfToken
                settings.model = model
                settings.publisher = publisher
                onClose()
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Batal") } },
    )
}
