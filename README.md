# Lumi Stickerize

Aplikasi Android untuk membuat **stiker WhatsApp otomatis dengan AI**.

## Cara kerja

1. **Pilih 3 foto** sebagai referensi wajah.
2. **Pilih jumlah stiker** (1–10).
3. **Pilih tema**: Cartoon, Chibi, Anime, 3D Lucu, Kawaii, Pixel Art, Komik, Cat Air, Clay, Sketsa.
4. **Tanpa kata / pakai kata** — kalau pakai kata, isi 1 kata untuk tiap stiker (5 stiker = 5 kata).
5. AI (Google Gemini) membuat **1 gambar saja** berisi semua stiker dalam bentuk grid → hemat token.
6. Aplikasi **memotong otomatis** gambar itu menjadi stiker 512×512 (latar putih dihapus, diberi garis tepi putih, WebP < 100 KB).
7. Tekan **Tambahkan ke WhatsApp**.

> Catatan: WhatsApp mewajibkan **minimal 3 stiker** per paket. Jika memilih 1–2 stiker, aplikasi menambahkan stiker bonus tanpa kata.

## Download APK

Setiap push ke `main` otomatis di-build oleh GitHub Actions. Ambil APK terbaru di tab **Releases** repo ini, lalu pasang di HP (izinkan "Instal dari sumber tidak dikenal").

## Pengaturan pertama kali

Buka ikon ⚙️ di aplikasi, lalu isi **Gemini API key** (gratis dari https://aistudio.google.com/apikey). Key hanya disimpan di HP.
Model default: `gemini-2.5-flash-image` — bisa diganti di pengaturan jika Google merilis model gambar yang lebih baru.

## Struktur kode

| File | Isi |
|---|---|
| `ai/PromptBuilder.kt` | Menyusun prompt 1 halaman (grid, tema, kata per sel) |
| `ai/GeminiClient.kt` | Memanggil API Gemini dengan 3 foto + prompt |
| `sticker/StickerCore.java` | Algoritma potong: hapus latar, cari celah antar stiker, buang noise, garis tepi putih |
| `sticker/StickerMaker.kt` | Render 512×512, encode WebP, ikon tray 96×96 |
| `sticker/PackStore.kt` | Simpan paket di penyimpanan internal |
| `sticker/StickerContentProvider.kt` | Kontrak ContentProvider yang dibaca WhatsApp |
| `ui/` | Tampilan Jetpack Compose (wizard 5 langkah) |

## Build sendiri

Buka folder ini di Android Studio, atau jalankan:

```
./gradlew assembleRelease
```

APK ditandatangani dengan `app/lumi-debug.keystore` (cukup untuk pemasangan manual).
Untuk rilis ke Play Store, buat keystore sendiri lalu set env `LUMI_KEYSTORE_PATH`, `LUMI_KEYSTORE_PASSWORD`, `LUMI_KEY_ALIAS`, `LUMI_KEY_PASSWORD`.
