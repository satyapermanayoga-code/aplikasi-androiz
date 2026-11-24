package android.graphics;

import java.util.ArrayDeque;
import java.util.Locale;

/** Tiruan Canvas Android untuk uji di komputer: mencatat perintah gambar sebagai SVG. */
public class Canvas {

    private final StringBuilder isi = new StringBuilder();
    private final StringBuilder definisi = new StringBuilder();
    private final ArrayDeque<Integer> simpanan = new ArrayDeque<>();
    private int grupTerbuka;
    private int nomor;

    public int save() {
        simpanan.push(grupTerbuka);
        grupTerbuka = 0;
        return simpanan.size();
    }

    public void restore() {
        tutupGrup();
        grupTerbuka = simpanan.pop();
    }

    private void tutupGrup() {
        for (int i = 0; i < grupTerbuka; i++) {
            isi.append("</g>");
        }
        grupTerbuka = 0;
    }

    private void buka(String ubah) {
        isi.append("<g transform=\"").append(ubah).append("\">");
        grupTerbuka++;
    }

    public void scale(float sx, float sy) {
        buka("scale(" + sx + " " + sy + ")");
    }

    public void translate(float dx, float dy) {
        buka("translate(" + dx + " " + dy + ")");
    }

    public void rotate(float derajat, float px, float py) {
        buka("rotate(" + derajat + " " + px + " " + py + ")");
    }

    private static String hex(int warna) {
        return String.format("#%06X", warna & 0xFFFFFF);
    }

    private String isian(Paint p) {
        if (p.shader instanceof LinearGradient) {
            LinearGradient g = (LinearGradient) p.shader;
            String id = "g" + (nomor++);
            definisi.append(String.format(Locale.ROOT,
                    "<linearGradient id=\"%s\" gradientUnits=\"userSpaceOnUse\" x1=\"%s\" y1=\"%s\" x2=\"%s\" y2=\"%s\">"
                            + "<stop offset=\"0\" stop-color=\"%s\"/><stop offset=\"1\" stop-color=\"%s\"/></linearGradient>",
                    id, g.x0, g.y0, g.x1, g.y1, hex(g.warna0), hex(g.warna1)));
            return "fill=\"url(#" + id + ")\"";
        }
        return String.format(Locale.ROOT, "fill=\"%s\" fill-opacity=\"%.3f\"", hex(p.color),
                ((p.color >>> 24) & 0xFF) / 255f);
    }

    public void drawRect(float kiri, float atas, float kanan, float bawah, Paint p) {
        isi.append(String.format(Locale.ROOT, "<rect x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\" %s/>",
                kiri, atas, kanan - kiri, bawah - atas, isian(p)));
    }

    public void drawRoundRect(RectF k, float rx, float ry, Paint p) {
        isi.append(String.format(Locale.ROOT,
                "<rect x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\" rx=\"%s\" ry=\"%s\" %s/>",
                k.left, k.top, k.right - k.left, k.bottom - k.top, rx, ry, isian(p)));
    }

    public void drawCircle(float cx, float cy, float r, Paint p) {
        isi.append(String.format(Locale.ROOT, "<circle cx=\"%s\" cy=\"%s\" r=\"%s\" %s/>", cx, cy, r, isian(p)));
    }

    public void drawOval(RectF k, Paint p) {
        isi.append(String.format(Locale.ROOT, "<ellipse cx=\"%s\" cy=\"%s\" rx=\"%s\" ry=\"%s\" %s/>",
                (k.left + k.right) / 2f, (k.top + k.bottom) / 2f, (k.right - k.left) / 2f,
                (k.bottom - k.top) / 2f, isian(p)));
    }

    public void drawBitmap(Bitmap gambar, Rect asal, RectF tujuan, Paint p) {
        isi.append(String.format(Locale.ROOT,
                "<image href=\"%s.png\" x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\" preserveAspectRatio=\"none\"/>",
                gambar.nama, tujuan.left, tujuan.top, tujuan.right - tujuan.left, tujuan.bottom - tujuan.top));
    }

    public void drawText(String teks, float x, float y, Paint p) {
        String jangkar = p.rata == Paint.Align.CENTER ? "middle" : p.rata == Paint.Align.RIGHT ? "end" : "start";
        String aman = teks.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        isi.append(String.format(Locale.ROOT,
                "<text x=\"%s\" y=\"%s\" font-family=\"Roboto, DejaVu Sans, sans-serif\" font-weight=\"700\" "
                        + "font-size=\"%s\" text-anchor=\"%s\" %s>%s</text>",
                x, y, p.ukuranTeks, jangkar, isian(p), aman));
    }

    /** Hasil akhir sebagai dokumen SVG berukuran w x h piksel. */
    public String keSvg(int w, int h) {
        StringBuilder s = new StringBuilder();
        s.append(String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">", w, h, w, h));
        s.append("<defs>").append(definisi).append("</defs>").append(isi);
        for (int i = 0; i < grupTerbuka; i++) {
            s.append("</g>");
        }
        return s.append("</svg>").toString();
    }
}
