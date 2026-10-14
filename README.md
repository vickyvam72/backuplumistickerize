# Lumi Stickerize

Aplikasi Android untuk membuat **stiker WhatsApp otomatis dengan AI**.

## Cara kerja

1. **Pilih 3 foto** sebagai referensi wajah.
2. **Pilih jumlah stiker** (3–10, minimal 3 sesuai aturan WhatsApp).
3. **Pilih tema**: Cartoon, Chibi, Anime, 3D Lucu, Kawaii, Pixel Art, Komik, Cat Air, Clay, Sketsa.
4. **Tanpa kata / pakai kata** — kalau pakai kata, isi 1 kata untuk tiap stiker (5 stiker = 5 kata).
5. AI (lewat OpenRouter) membuat **1 gambar saja** berisi semua stiker dalam bentuk grid → hemat token.
6. Aplikasi **memotong otomatis** gambar itu menjadi stiker 512×512 (latar putih dihapus, diberi garis tepi putih, WebP < 100 KB).
7. Tekan **Tambahkan ke WhatsApp**.

## Download APK

Setiap push ke `main` otomatis di-build oleh GitHub Actions. Ambil APK terbaru di tab **Releases** repo ini, lalu pasang di HP (izinkan "Instal dari sumber tidak dikenal").

## Pengaturan pertama kali

Buka ikon ⚙️ di aplikasi, lalu isi **OpenRouter API key** (buat di https://openrouter.ai/keys, isi kredit di https://openrouter.ai/credits). Key hanya disimpan di HP.

Pilihan model (bisa diganti di pengaturan):

| Model | ID | Catatan |
|---|---|---|
| Nano Banana 2 Lite (default) | `google/gemini-3.1-flash-lite-image` | Murah & cepat, wajah mirip, teks rapi |
| Seedream 5.0 Flash | `bytedance-seed/seedream-5-0-flash` | Paling hemat (±$0,018/gambar) |
| Nano Banana 2 | `google/gemini-3.1-flash-image` | Kualitas terbaik, ±2x harga Lite |

Aplikasi memakai endpoint `POST https://openrouter.ai/api/v1/images` dengan 3 foto sebagai `input_references`. Satu pembuatan = 1 gambar, berapa pun jumlah stikernya.

## Struktur kode

| File | Isi |
|---|---|
| `ai/PromptBuilder.kt` | Menyusun prompt 1 halaman (grid, tema, kata per sel) |
| `ai/OpenRouterClient.kt` | Memanggil Image API OpenRouter dengan 3 foto + prompt |
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
