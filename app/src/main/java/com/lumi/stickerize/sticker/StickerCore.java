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

    /**
     * Hapus garis bingkai tipis (kotak/kartu yang kadang digambar AI di sekeliling stiker).
     * Garis bingkai dikenali sebagai deretan piksel gelap yang panjang, tapi di kedua sisinya
     * (beberapa piksel di atas & bawah, atau kiri & kanan) masih putih. Garis seperti itu
     * diputihkan (in-place) supaya latar di dalam kotak bisa terhapus oleh flood fill.
     * @param minRun panjang minimum garis (mis. 45% lebar satu sel)
     * @return jumlah piksel garis yang diputihkan
     */
    public static int removeFrameLines(int[] px, int w, int h, int minRun) {
        final int gap = 6;          // jarak cek sisi kiri/kanan garis
        final int maxThick = 10;    // garis lebih tebal dari ini bukan bingkai
        boolean[] line = new boolean[w * h];
        int removed = 0;

        // garis horizontal
        for (int y = 0; y < h; y++) {
            int x = 0;
            while (x < w) {
                if (isBackgroundColor(px[y * w + x])) { x++; continue; }
                int start = x;
                while (x < w && !isBackgroundColor(px[y * w + x])) x++;
                int len = x - start;
                if (len >= minRun && isThinHorizontal(px, w, h, y, start, x, gap, maxThick)) {
                    for (int i = start; i < x; i++) line[y * w + i] = true;
                }
            }
        }
        // garis vertikal
        for (int x = 0; x < w; x++) {
            int y = 0;
            while (y < h) {
                if (isBackgroundColor(px[y * w + x])) { y++; continue; }
                int start = y;
                while (y < h && !isBackgroundColor(px[y * w + x])) y++;
                int len = y - start;
                if (len >= minRun && isThinVertical(px, w, h, x, start, y, gap, maxThick)) {
                    for (int i = start; i < y; i++) line[i * w + x] = true;
                }
            }
        }
        // Sisa lengkungan sudut bingkai: potongan tinta kecil yang menempel pada garis bingkai.
        final int maxCorner = Math.max(200, minRun * minRun / 60);
        boolean[] visited = new boolean[w * h];
        int[] queue = new int[w * h];
        for (int i = 0; i < line.length; i++) {
            if (!line[i]) continue;
            int x = i % w, y = i / w;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    int ni = ny * w + nx;
                    if (line[ni] || visited[ni] || isBackgroundColor(px[ni])) continue;
                    // kumpulkan komponen tinta (tanpa garis), berhenti kalau terlalu besar
                    int head = 0, tail = 0;
                    queue[tail++] = ni;
                    visited[ni] = true;
                    boolean tooBig = false;
                    while (head < tail) {
                        int c = queue[head++];
                        if (tail > maxCorner) { tooBig = true; break; }
                        int cx = c % w, cy = c / w;
                        for (int ey = -1; ey <= 1; ey++) {
                            for (int ex = -1; ex <= 1; ex++) {
                                int mx = cx + ex, my = cy + ey;
                                if (mx < 0 || my < 0 || mx >= w || my >= h) continue;
                                int mi = my * w + mx;
                                if (line[mi] || visited[mi] || isBackgroundColor(px[mi])) continue;
                                visited[mi] = true;
                                queue[tail++] = mi;
                            }
                        }
                    }
                    if (!tooBig) {
                        for (int q = 0; q < tail; q++) {
                            px[queue[q]] = 0xFFFFFFFF;
                            removed++;
                        }
                    }
                }
            }
        }

        for (int i = 0; i < line.length; i++) {
            if (line[i]) {
                px[i] = 0xFFFFFFFF;
                removed++;
            }
        }
        return removed;
    }

    private static boolean isThinHorizontal(int[] px, int w, int h, int y, int x0, int x1,
                                            int gap, int maxThick) {
        int whiteAbove = 0, whiteBelow = 0, n = x1 - x0;
        for (int x = x0; x < x1; x++) {
            if (whiteWithin(px, w, h, x, y, 0, -1, gap, maxThick)) whiteAbove++;
            if (whiteWithin(px, w, h, x, y, 0, 1, gap, maxThick)) whiteBelow++;
        }
        return whiteAbove >= n * 0.8 && whiteBelow >= n * 0.8;
    }

    private static boolean isThinVertical(int[] px, int w, int h, int x, int y0, int y1,
                                          int gap, int maxThick) {
        int whiteLeft = 0, whiteRight = 0, n = y1 - y0;
        for (int y = y0; y < y1; y++) {
            if (whiteWithin(px, w, h, x, y, -1, 0, gap, maxThick)) whiteLeft++;
            if (whiteWithin(px, w, h, x, y, 1, 0, gap, maxThick)) whiteRight++;
        }
        return whiteLeft >= n * 0.8 && whiteRight >= n * 0.8;
    }

    /** Apakah ada piksel putih dalam jarak [gap .. maxThick] ke arah (dx,dy)? Tepi gambar dihitung putih. */
    private static boolean whiteWithin(int[] px, int w, int h, int x, int y, int dx, int dy,
                                       int gap, int maxThick) {
        for (int k = 1; k <= maxThick; k++) {
            int nx = x + dx * k, ny = y + dy * k;
            if (nx < 0 || ny < 0 || nx >= w || ny >= h) return true;
            if (isBackgroundColor(px[ny * w + nx])) {
                // putih harus bertahan beberapa piksel (bukan sekadar celah antar huruf)
                int run = 0;
                for (int j = k; j < k + gap; j++) {
                    int mx = x + dx * j, my = y + dy * j;
                    if (mx < 0 || my < 0 || mx >= w || my >= h) { run = gap; break; }
                    if (!isBackgroundColor(px[my * w + mx])) break;
                    run++;
                }
                return run >= gap;
            }
        }
        return false;
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
        boolean[] lineLike = new boolean[comps + 1];
        int cellMin = Math.min(cw, ch);
        for (int c = 1; c <= comps; c++) {
            boolean k = areas[c] >= minArea;
            if (touches[c] && areas[c] < largest * 0.15) k = false;
            keep[c] = k;
            // Potongan garis: tipis, memanjang, dan hampir memenuhi kotak pembatasnya
            int bwc = bounds[c * 4 + 2] - bounds[c * 4] + 1;
            int bhc = bounds[c * 4 + 3] - bounds[c * 4 + 1] + 1;
            int len = Math.max(bwc, bhc);
            double thick = (double) areas[c] / len;
            double fill = (double) areas[c] / ((double) bwc * bhc);
            lineLike[c] = len >= thick * 3.5 && thick <= cellMin * 0.035 && fill >= 0.45
                && areas[c] < largest * 0.2;
        }
        // Buang potongan garis yang terpisah dari isi stiker (bukan bagian huruf / gambar).
        int gapLimit = Math.max(4, (int) (cellMin * 0.025));
        for (int c = 1; c <= comps; c++) {
            if (!keep[c] || !lineLike[c]) continue;
            boolean near = false;
            for (int o = 1; o <= comps && !near; o++) {
                if (o == c || !keep[o] || lineLike[o]) continue;
                int dx = Math.max(0, Math.max(bounds[o * 4] - bounds[c * 4 + 2], bounds[c * 4] - bounds[o * 4 + 2]));
                int dy = Math.max(0, Math.max(bounds[o * 4 + 1] - bounds[c * 4 + 3], bounds[c * 4 + 1] - bounds[o * 4 + 3]));
                if (Math.max(dx, dy) <= gapLimit) near = true;
            }
            if (!near) keep[c] = false;
        }

        int l = cw, t = ch, r = -1, b = -1;
        for (int c = 1; c <= comps; c++) {
            boolean k = keep[c];
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
