package com.lumi.stickerize.sticker;

/**
 * Logika murni (tanpa API Android) untuk memotong 1 lembar gambar AI menjadi stiker-stiker.
 * Semua piksel dalam format ARGB int (sama seperti Bitmap Android).
 */
public final class StickerCore {

    private StickerCore() {}

    /** Hasil potongan satu sel: piksel ARGB dengan latar yang sudah transparan. */
    public static final class Piece {
        public final int[] pixels;
        public final int width;
        public final int height;

        Piece(int[] pixels, int width, int height) {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
        }
    }

    static boolean isBackgroundColor(int c) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        return min >= 226 && (max - min) <= 30;
    }

    /** Tandai latar putih yang tersambung ke tepi gambar (flood fill). true = latar. */
    public static boolean[] backgroundMask(int[] px, int w, int h) {
        boolean[] bg = new boolean[w * h];
        int[] queue = new int[w * h];
        int head = 0, tail = 0;
        for (int x = 0; x < w; x++) {
            tail = seed(px, bg, queue, tail, x);
            tail = seed(px, bg, queue, tail, (h - 1) * w + x);
        }
        for (int y = 0; y < h; y++) {
            tail = seed(px, bg, queue, tail, y * w);
            tail = seed(px, bg, queue, tail, y * w + w - 1);
        }
        while (head < tail) {
            int i = queue[head++];
            int x = i % w, y = i / w;
            if (x > 0) tail = seed(px, bg, queue, tail, i - 1);
            if (x < w - 1) tail = seed(px, bg, queue, tail, i + 1);
            if (y > 0) tail = seed(px, bg, queue, tail, i - w);
            if (y < h - 1) tail = seed(px, bg, queue, tail, i + w);
        }
        return bg;
    }

    private static int seed(int[] px, boolean[] bg, int[] queue, int tail, int i) {
        if (!bg[i] && isBackgroundColor(px[i])) {
            bg[i] = true;
            queue[tail++] = i;
        }
        return tail;
    }

    /**
     * Cari garis potong. AI jarang menaruh stiker tepat di grid, jadi di sekitar posisi ideal
     * kita cari kolom/baris yang paling "kosong" (celah putih di antara stiker).
     * @param vertical true = cari batas kolom (sumbu x), false = batas baris (sumbu y)
     * @return array berisi parts+1 posisi, dari 0 sampai panjang sumbu
     */
    public static int[] findCuts(boolean[] bg, int w, int h, int parts, boolean vertical) {
        int len = vertical ? w : h;
        int other = vertical ? h : w;
        int[] profile = new int[len];
        for (int a = 0; a < len; a++) {
            int count = 0;
            for (int b = 0; b < other; b++) {
                int idx = vertical ? (b * w + a) : (a * w + b);
                if (!bg[idx]) count++;
            }
            profile[a] = count;
        }
        int[] cuts = new int[parts + 1];
        cuts[0] = 0;
        cuts[parts] = len;
        int window = Math.max(2, len / (parts * 5));
        for (int i = 1; i < parts; i++) {
            int expected = (int) Math.round((double) len * i / parts);
            int best = expected;
            long bestScore = Long.MAX_VALUE;
            int from = Math.max(1, expected - window);
            int to = Math.min(len - 2, expected + window);
            for (int a = from; a <= to; a++) {
                // skor: jumlah piksel isi, plus sedikit penalti jika jauh dari posisi ideal
                long score = (long) profile[a] * 1000 + Math.abs(a - expected);
                if (score < bestScore) {
                    bestScore = score;
                    best = a;
                }
            }
            cuts[i] = best;
        }
        return cuts;
    }

    /**
     * Ambil isi satu sel. Komponen kecil yang menempel di tepi sel (potongan stiker tetangga)
     * dan bintik-bintik kecil dibuang. Mengembalikan null jika sel kosong.
     */
    public static Piece extract(int[] px, boolean[] bg, int w, int x0, int y0, int x1, int y1) {
        int cw = x1 - x0, ch = y1 - y0;
        if (cw <= 2 || ch <= 2) return null;
        int n = cw * ch;
        int[] label = new int[n]; // 0 = belum, -1 = latar, >0 = id komponen
        int[] queue = new int[n];
        int[] areas = new int[16];
        boolean[] touches = new boolean[16];
        int[] bounds = new int[16 * 4];
        int comps = 0;

        for (int i = 0; i < n; i++) {
            int lx = i % cw, ly = i / cw;
            int gi = (y0 + ly) * w + (x0 + lx);
            if (bg[gi]) label[i] = -1;
        }

        for (int start = 0; start < n; start++) {
            if (label[start] != 0) continue;
            comps++;
            if (comps >= areas.length) {
                areas = java.util.Arrays.copyOf(areas, areas.length * 2);
                touches = java.util.Arrays.copyOf(touches, touches.length * 2);
                bounds = java.util.Arrays.copyOf(bounds, bounds.length * 2);
            }
            int minX = cw, minY = ch, maxX = -1, maxY = -1, area = 0;
            boolean touch = false;
            int head = 0, tail = 0;
            queue[tail++] = start;
            label[start] = comps;
            while (head < tail) {
                int i = queue[head++];
                int lx = i % cw, ly = i / cw;
                area++;
                if (lx < minX) minX = lx;
                if (lx > maxX) maxX = lx;
                if (ly < minY) minY = ly;
                if (ly > maxY) maxY = ly;
                if (lx == 0 || ly == 0 || lx == cw - 1 || ly == ch - 1) touch = true;
                for (int dy = -1; dy <= 1; dy++) {
                    int ny = ly + dy;
                    if (ny < 0 || ny >= ch) continue;
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = lx + dx;
                        if ((dx == 0 && dy == 0) || nx < 0 || nx >= cw) continue;
                        int ni = ny * cw + nx;
                        if (label[ni] == 0) {
                            label[ni] = comps;
                            queue[tail++] = ni;
                        }
                    }
                }
            }
            areas[comps] = area;
            touches[comps] = touch;
            bounds[comps * 4] = minX;
            bounds[comps * 4 + 1] = minY;
            bounds[comps * 4 + 2] = maxX;
            bounds[comps * 4 + 3] = maxY;
        }
        if (comps == 0) return null;

        int largest = 0;
        for (int c = 1; c <= comps; c++) largest = Math.max(largest, areas[c]);
        int minArea = Math.max(12, n / 4000);
        if (largest < n / 200) return null; // sel praktis kosong

        boolean[] keep = new boolean[comps + 1];
        int l = cw, t = ch, r = -1, b = -1;
        for (int c = 1; c <= comps; c++) {
            boolean k = areas[c] >= minArea;
            if (touches[c] && areas[c] < largest * 0.15) k = false;
            keep[c] = k;
            if (k) {
                l = Math.min(l, bounds[c * 4]);
                t = Math.min(t, bounds[c * 4 + 1]);
                r = Math.max(r, bounds[c * 4 + 2]);
                b = Math.max(b, bounds[c * 4 + 3]);
            }
        }
        if (r < l || b < t) return null;

        int pw = r - l + 1, ph = b - t + 1;
        int[] out = new int[pw * ph];
        for (int y = 0; y < ph; y++) {
            for (int x = 0; x < pw; x++) {
                int li = (t + y) * cw + (l + x);
                int lab = label[li];
                if (lab > 0 && keep[lab]) {
                    out[y * pw + x] = px[(y0 + t + y) * w + (x0 + l + x)] | 0xFF000000;
                } else {
                    out[y * pw + x] = 0;
                }
            }
        }
        return new Piece(out, pw, ph);
    }

    /**
     * Tambahkan garis tepi putih ala stiker WhatsApp di sekeliling isi (in-place).
     * Memakai transformasi jarak chamfer 3-4 agar cepat.
     */
    public static void addOutline(int[] px, int w, int h, int radius, int outlineRgb) {
        final int INF = Integer.MAX_VALUE / 4;
        int[] d = new int[w * h];
        for (int i = 0; i < d.length; i++) d[i] = ((px[i] >>> 24) >= 128) ? 0 : INF;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int v = d[i];
                if (v == 0) continue;
                if (x > 0) v = Math.min(v, d[i - 1] + 3);
                if (y > 0) {
                    v = Math.min(v, d[i - w] + 3);
                    if (x > 0) v = Math.min(v, d[i - w - 1] + 4);
                    if (x < w - 1) v = Math.min(v, d[i - w + 1] + 4);
                }
                d[i] = v;
            }
        }
        for (int y = h - 1; y >= 0; y--) {
            for (int x = w - 1; x >= 0; x--) {
                int i = y * w + x;
                int v = d[i];
                if (v == 0) continue;
                if (x < w - 1) v = Math.min(v, d[i + 1] + 3);
                if (y < h - 1) {
                    v = Math.min(v, d[i + w] + 3);
                    if (x < w - 1) v = Math.min(v, d[i + w + 1] + 4);
                    if (x > 0) v = Math.min(v, d[i + w - 1] + 4);
                }
                d[i] = v;
            }
        }

        double full = radius;      // jarak (piksel) dengan garis penuh
        double soft = 1.5;         // lebar tepi halus (anti-alias)
        int or = (outlineRgb >> 16) & 0xFF, og = (outlineRgb >> 8) & 0xFF, ob = outlineRgb & 0xFF;
        for (int i = 0; i < px.length; i++) {
            double dist = d[i] / 3.0;
            double oa;
            if (dist <= full) oa = 1.0;
            else if (dist >= full + soft) oa = 0.0;
            else oa = 1.0 - (dist - full) / soft;
            if (oa <= 0) continue;

            int c = px[i];
            double sa = ((c >>> 24) & 0xFF) / 255.0;
            if (sa >= 1.0) continue;
            // komposit: konten DI ATAS garis putih
            double outA = sa + oa * (1 - sa);
            if (outA <= 0) continue;
            int sr = (c >> 16) & 0xFF, sg = (c >> 8) & 0xFF, sb = c & 0xFF;
            int rr = (int) Math.round((sr * sa + or * oa * (1 - sa)) / outA);
            int gg = (int) Math.round((sg * sa + og * oa * (1 - sa)) / outA);
            int bb = (int) Math.round((sb * sa + ob * oa * (1 - sa)) / outA);
            int aa = (int) Math.round(outA * 255);
            px[i] = (aa << 24) | (clamp(rr) << 16) | (clamp(gg) << 8) | clamp(bb);
        }
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
