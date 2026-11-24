#!/usr/bin/env python3
"""Membuat semua gambar game dari kode, tanpa memakai foto asli:
avatar dalam beberapa pose, rintangan, bunga, dan ikon aplikasi.

Butuh rsvg-convert dan Pillow. Jalankan dari akar repo:

    python3 alat/buat_aset.py [folder_pratinjau]

Hasilnya masuk ke app/src/main/res/drawable-nodpi/. Jika folder pratinjau
diberikan, satu lembar contoh (pratinjau_aset.png) ikut dibuat di sana.
"""
import os
import subprocess
import sys
import tempfile

from PIL import Image, ImageFilter

SKALA = 3  # piksel per satuan gambar
AKAR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(AKAR, 'app', 'src', 'main', 'res', 'drawable-nodpi')

# Palet. Warna rambut, kulit, sepatu, dan kayu diambil dari foto acuan.
RAMBUT = '#2F221C'
RAMBUT_KILAU = '#5B4236'
KULIT = '#F2CFB6'
KULIT_GELAP = '#DFB79D'
GAUN_GARIS = '#B9B4CE'
GAUN_BAYANG = '#E6E4EF'
SEPATU = '#7D8189'
SEPATU_GELAP = '#62666E'
SOL = '#F6F6F4'
PIPI = '#F6A6B2'
MULUT = '#B4524F'
GARIS_LUAR = (58, 42, 52)  # garis tepi gelap di sekeliling tiap gambar


# ---------------------------------------------------------------- avatar

def rambut_belakang(s, a):
    """Rambut panjang bergelombang di belakang badan. s = ayun ke kiri, a = terangkat."""
    return (
        f'<path d="M33,22 C24,30 {21 - s * 0.3:.1f},46 {22 - s * 0.6:.1f},58 '
        f'C{23 - s * 0.8:.1f},66 {19 - s:.1f},{71 - a:.1f} {15 - s:.1f},{79 - a * 1.3:.1f} '
        f'Q{23 - s:.1f},{81 - a * 1.2:.1f} {28 - s * 0.9:.1f},{74 - a:.1f} '
        f'Q{29 - s * 0.9:.1f},{84 - a * 1.3:.1f} {36 - s * 0.8:.1f},{85 - a * 1.3:.1f} '
        f'Q{40 - s * 0.6:.1f},{78 - a:.1f} {42 - s * 0.5:.1f},{71 - a * 0.6:.1f} '
        f'Q{47 - s * 0.4:.1f},{81 - a:.1f} {54 - s * 0.2:.1f},{75 - a * 0.6:.1f} '
        f'C59,64 60,50 58,36 Z" fill="{RAMBUT}"/>'
        f'<path d="M28,40 C25,50 {25 - s * 0.5:.1f},58 {26 - s * 0.8:.1f},{66 - a * 0.5:.1f}" fill="none" '
        f'stroke="{RAMBUT_KILAU}" stroke-width="1.6" stroke-linecap="round" opacity="0.8"/>'
    )


def kaki(hx, hy, sudut, tekuk, gelap=False):
    """Satu kaki dari pinggul (hx, hy). sudut negatif = ke depan, tekuk = lutut."""
    kulit = KULIT_GELAP if gelap else KULIT
    sepatu = SEPATU_GELAP if gelap else SEPATU
    y0 = hy + 18
    return (
        f'<g transform="rotate({sudut} {hx} {hy})">'
        f'<rect x="{hx - 2.7}" y="{hy - 1}" width="5.4" height="11" rx="2.7" fill="{kulit}"/>'
        f'<g transform="rotate({tekuk} {hx} {hy + 9})">'
        f'<rect x="{hx - 2.5}" y="{hy + 8}" width="5" height="10" rx="2.5" fill="{kulit}"/>'
        f'<rect x="{hx - 2.9}" y="{y0 - 4.2}" width="5.8" height="4.6" rx="1.5" fill="#FFFFFF"/>'
        f'<path d="M{hx - 4.2},{y0 - 1} h7.5 q6.5,0.5 7.2,4.6 v2.2 h-14.7 z" fill="{sepatu}"/>'
        f'<rect x="{hx - 4.6}" y="{y0 + 4.6}" width="15.6" height="3" rx="1.5" fill="{SOL}"/>'
        f'<path d="M{hx + 6.2},{y0 + 0.6} q3.6,0.8 4.2,3.9 h-4.2 z" fill="#ECECEC"/>'
        '</g></g>'
    )


def buket(hx, hy, putar):
    """Buket kecil yang digenggam di titik (hx, hy)."""
    s = f'<g transform="rotate({putar} {hx} {hy})">'
    s += (f'<path d="M{hx - 1.7},{hy + 3.4} L{hx + 1.7},{hy + 3.4} L{hx + 7},{hy - 8} L{hx - 7},{hy - 8} Z" '
          f'fill="#FBF7EF" stroke="#D5CDBB" stroke-width="0.5" stroke-linejoin="round"/>')
    for dx, dy, rx, ry, rot, c in ((-8.5, -10.5, 4.2, 2.2, -35, '#7DB872'), (8.5, -10.5, 4.2, 2.2, 35, '#7DB872'),
                                   (-5.5, -19, 3.8, 2, -60, '#93C785'), (5.5, -19.5, 3.8, 2, 60, '#93C785'),
                                   (0, -21.5, 3.6, 2, 90, '#86BF7A')):
        s += (f'<ellipse cx="{hx + dx}" cy="{hy + dy}" rx="{rx}" ry="{ry}" fill="{c}" '
              f'transform="rotate({rot} {hx + dx} {hy + dy})"/>')
    bunga = ((0, -13.5, 3.5, '#FFFFFF'), (-5, -11.2, 3.1, '#F7B5C8'), (5, -11.2, 3.1, '#FFFFFF'),
             (-3.2, -17, 3.1, '#FFFFFF'), (3.4, -17.2, 3.1, '#F7B5C8'), (0, -9.2, 2.6, '#F7B5C8'))
    for dx, dy, r, c in bunga:
        s += f'<circle cx="{hx + dx}" cy="{hy + dy}" r="{r}" fill="{c}" stroke="#D9C9D6" stroke-width="0.4"/>'
        s += f'<circle cx="{hx + dx}" cy="{hy + dy}" r="0.9" fill="#F3C94F"/>'
    for dx, dy in ((-7.6, -15), (7.6, -15.4), (-1, -20.4), (6.6, -7.6), (-6.8, -7.4)):
        s += f'<circle cx="{hx + dx}" cy="{hy + dy}" r="1.3" fill="#FFFFFF" stroke="#D9C9D6" stroke-width="0.3"/>'
    # pita
    s += (f'<path d="M{hx},{hy - 0.2} l-4.6,-2.6 v5.2 z M{hx},{hy - 0.2} l4.6,-2.6 v5.2 z" fill="#FFFFFF" '
          f'stroke="{GAUN_GARIS}" stroke-width="0.5" stroke-linejoin="round"/>'
          f'<circle cx="{hx}" cy="{hy - 0.2}" r="1.3" fill="#FFFFFF" stroke="{GAUN_GARIS}" stroke-width="0.5"/>')
    return s + '</g>'


def lengan(sx, sy, a1, a2, gelap=False, buket_miring=None):
    """Lengan dari bahu (sx, sy): a1 = sudut lengan atas, a2 = tekukan siku."""
    kulit = KULIT_GELAP if gelap else KULIT
    s = f'<g transform="rotate({a1} {sx} {sy})">'
    s += f'<rect x="{sx - 2.2}" y="{sy - 1}" width="4.4" height="12.5" rx="2.2" fill="{kulit}"/>'
    s += f'<g transform="rotate({a2} {sx} {sy + 10})">'
    s += f'<rect x="{sx - 2}" y="{sy + 9}" width="4" height="10" rx="2" fill="{kulit}"/>'
    if buket_miring is not None:
        # buket diputar balik supaya tetap tegak apa pun posisi lengannya
        s += buket(sx, sy + 19.5, buket_miring - (a1 + a2))
    s += f'<circle cx="{sx}" cy="{sy + 19.5}" r="2.8" fill="{kulit}"/>'
    s += '</g>'
    # lengan baju tipis menggembung dengan manset berenda
    s += (f'<ellipse cx="{sx}" cy="{sy + 4.6}" rx="4.7" ry="6.9" fill="#FFFFFF" fill-opacity="0.88" '
          f'stroke="{GAUN_GARIS}" stroke-width="0.6"/>')
    for dx in (-3, 0, 3):
        s += (f'<circle cx="{sx + dx}" cy="{sy + 11.2}" r="1.9" fill="#FFFFFF" '
              f'stroke="{GAUN_GARIS}" stroke-width="0.5"/>')
    return s + '</g>'


def rok(ayun, kembang):
    """Rok berlapis dengan tepi berenda. ayun = geser tepi bawah, kembang = melebar."""
    yb = 96 - kembang * 0.6
    xl = 26 - kembang + ayun
    xr = 71 + kembang + ayun
    n = 8
    dx = (xr - xl) / n
    d = f'M40.5,64 L57.5,64 C63,73 {xr - 2:.1f},{yb - 12:.1f} {xr:.1f},{yb:.1f} '
    d += ''.join(f'q{-dx / 2:.2f},3.4 {-dx:.2f},0 ' for _ in range(n))
    d += f'C{xl + 2:.1f},{yb - 12:.1f} 35,73 40.5,64 Z'
    s = f'<path d="{d}" fill="{GAUN_BAYANG}" stroke="{GAUN_GARIS}" stroke-width="0.7" stroke-linejoin="round"/>'
    for t in (0.2, 0.4, 0.62, 0.82):
        x = xl + (xr - xl) * t
        s += (f'<path d="M{x:.1f},{yb - 1:.1f} Q{49 + (x - 49) * 0.75:.1f},{yb - 9:.1f} {49 + (x - 49) * 0.55:.1f},{yb - 15:.1f}" '
              f'fill="none" stroke="{GAUN_GARIS}" stroke-width="0.6" stroke-linecap="round" opacity="0.8"/>')
    # lapis atas: renda menyerong dari kanan atas ke kiri bawah
    m = 6
    xa, ya = xr - 4, yb - 15
    xb, yb2 = xl + 1.5, yb - 4
    ddx, ddy = (xb - xa) / m, (yb2 - ya) / m
    d2 = f'M40.5,64 L57.5,64 C62,71 {xa - 1:.1f},{ya - 8:.1f} {xa:.1f},{ya:.1f} '
    d2 += ''.join(f'q{ddx / 2:.2f},{ddy / 2 + 3.4:.2f} {ddx:.2f},{ddy:.2f} ' for _ in range(m))
    d2 += f'C{xl + 3:.1f},{yb - 14:.1f} 35,73 40.5,64 Z'
    s += f'<path d="{d2}" fill="#FFFFFF" stroke="{GAUN_GARIS}" stroke-width="0.7" stroke-linejoin="round"/>'
    s += (f'<path d="M47,66 Q46,74 {xl + 9:.1f},{yb - 9:.1f}" fill="none" stroke="{GAUN_GARIS}" '
          'stroke-width="0.6" stroke-linecap="round" opacity="0.7"/>')
    return s


def badan():
    s = (f'<path d="M40,50.5 Q40,48 42.5,48 L55.5,48 Q58,48 58,50.5 L57.8,65 L40.2,65 Z" fill="#FFFFFF" '
         f'stroke="{GAUN_GARIS}" stroke-width="0.7" stroke-linejoin="round"/>')
    s += f'<path d="M44,47.4 h10 v4 q-5,1.8 -10,0 z" fill="{KULIT}"/>'
    for i in range(5):  # renda di garis leher
        s += (f'<circle cx="{44.6 + i * 2.2:.1f}" cy="52.2" r="1.25" fill="#FFFFFF" '
              f'stroke="{GAUN_GARIS}" stroke-width="0.4"/>')
    s += f'<path d="M40.4,63.6 H57.6" stroke="{GAUN_GARIS}" stroke-width="0.8" stroke-linecap="round"/>'
    return s


def wajah(jenis):
    s = f'<ellipse cx="41.8" cy="36.8" rx="2.8" ry="1.8" fill="{PIPI}" opacity="0.7"/>'
    s += f'<ellipse cx="60.6" cy="36.8" rx="2.8" ry="1.8" fill="{PIPI}" opacity="0.7"/>'
    for x in (45.8, 57.2):
        y = 31.8
        if jenis == 'senyum':
            s += f'<ellipse cx="{x}" cy="{y}" rx="1.9" ry="2.7" fill="{RAMBUT}"/>'
            s += f'<circle cx="{x + 0.6}" cy="{y - 1}" r="0.75" fill="#FFFFFF"/>'
        elif jenis == 'riang':
            s += (f'<path d="M{x - 2.3},{y + 0.8} Q{x},{y - 2.8} {x + 2.3},{y + 0.8}" fill="none" '
                  f'stroke="{RAMBUT}" stroke-width="1.3" stroke-linecap="round"/>')
        else:  # aduh
            arah = 1 if x < 50 else -1
            s += (f'<path d="M{x - 2 * arah},{y - 2} L{x + 1.6 * arah},{y} L{x - 2 * arah},{y + 2}" fill="none" '
                  f'stroke="{RAMBUT}" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>')
    if jenis == 'senyum':
        s += (f'<path d="M49.3,37.3 Q51.5,39.9 53.7,37.3" fill="none" stroke="{MULUT}" '
              'stroke-width="1.1" stroke-linecap="round"/>')
    elif jenis == 'riang':
        s += (f'<path d="M48.7,36.8 Q51.5,41.6 54.3,36.8 Z" fill="#C4575A" stroke="{MULUT}" '
              'stroke-width="0.7" stroke-linejoin="round"/>')
    else:
        s += f'<ellipse cx="51.5" cy="38.6" rx="1.6" ry="2" fill="{MULUT}"/>'
    return s


def kepala(jenis_wajah):
    s = f'<rect x="46.3" y="41" width="5.4" height="8.5" fill="{KULIT_GELAP}"/>'
    s += f'<ellipse cx="49" cy="30" rx="15.6" ry="15.2" fill="{KULIT}"/>'
    s += wajah(jenis_wajah)
    # rambut depan: tudung dan poni
    s += (f'<path d="M33,36 C30.5,18 40,12.5 49,12.5 C59,12.5 67.5,19 65.3,34 L63.6,34 '
          'C63.4,30 62,27.2 60.3,25.2 Q58.6,28.2 55.2,27.6 Q54,24.6 51.5,23.6 Q50.2,27.6 46.2,28 '
          f'Q44.6,25.2 43.2,24.2 Q41.6,28.4 37.6,29.6 Q36,31.6 35.6,36 Z" fill="{RAMBUT}"/>')
    s += f'<path d="M34.2,30 C31.5,38 33.5,44 31.5,51 C35.6,48 37,41 36.6,33 Z" fill="{RAMBUT}"/>'
    s += f'<path d="M64.6,30 C66.4,36 64.8,40 65.6,45 C62.8,42 62.6,36 63.2,31 Z" fill="{RAMBUT}"/>'
    s += (f'<path d="M38,19.5 Q46,14.6 56,16.6" fill="none" stroke="{RAMBUT_KILAU}" '
          'stroke-width="1.8" stroke-linecap="round" opacity="0.85"/>')
    # dua cepol kecil dengan pita putih
    for bx, by in ((38, 14.4), (59.6, 13.8)):
        s += f'<circle cx="{bx}" cy="{by}" r="3.5" fill="{RAMBUT}"/>'
        s += (f'<path d="M{bx},{by - 2.8} l-4.4,-2.7 v5.4 z M{bx},{by - 2.8} l4.4,-2.7 v5.4 z" fill="#FFFFFF" '
              f'stroke="{GAUN_GARIS}" stroke-width="0.5" stroke-linejoin="round"/>')
        s += f'<circle cx="{bx}" cy="{by - 2.8}" r="1.3" fill="#FFFFFF" stroke="{GAUN_GARIS}" stroke-width="0.5"/>'
    return s


POSE = {
    'lari1': dict(dy=0.8, kaki_dekat=(-30, 12), kaki_jauh=(32, 30), lengan_dekat=(-58, -48),
                  lengan_jauh=(38, -25), ayun=5, angkat=2, rok_ayun=-1.5, rok_kembang=1,
                  wajah='senyum', buket=18),
    'lari2': dict(dy=-0.8, kaki_dekat=(30, 30), kaki_jauh=(-30, 12), lengan_dekat=(-52, -52),
                  lengan_jauh=(-18, -30), ayun=7, angkat=5, rok_ayun=1.5, rok_kembang=2,
                  wajah='senyum', buket=10),
    'lompat': dict(dy=0, kaki_dekat=(-50, 62), kaki_jauh=(38, 58), lengan_dekat=(-118, -32),
                   lengan_jauh=(72, 18), ayun=7, angkat=14, rok_ayun=0, rok_kembang=6,
                   wajah='riang', buket=8),
    'jatuh': dict(dy=0, kaki_dekat=(-38, 0), kaki_jauh=(30, 8), lengan_dekat=(-95, -20),
                  lengan_jauh=(100, 10), ayun=2, angkat=8, rok_ayun=0, rok_kembang=3,
                  wajah='aduh', buket=40),
}


def avatar(nama_pose):
    p = POSE[nama_pose]
    s = f'<g transform="translate(0 {p["dy"]})">'
    s += rambut_belakang(p['ayun'], p['angkat'])
    s += lengan(42.5, 52, p['lengan_jauh'][0], p['lengan_jauh'][1], gelap=True)
    s += kaki(45.5, 92, p['kaki_jauh'][0], p['kaki_jauh'][1], gelap=True)
    s += kaki(51.5, 92, p['kaki_dekat'][0], p['kaki_dekat'][1])
    s += rok(p['rok_ayun'], p['rok_kembang'])
    s += badan()
    s += kepala(p['wajah'])
    s += lengan(55.5, 52, p['lengan_dekat'][0], p['lengan_dekat'][1], buket_miring=p['buket'])
    return s + '</g>'


# ------------------------------------------------------------- rintangan

def pot(ox=0.0, oy=0.0, skala=1.0):
    s = f'<g transform="translate({ox} {oy}) scale({skala})">'
    for cx, cy, r, c in ((11, 13, 7.5, '#6DB36B'), (22, 12, 8, '#7CC27A'), (17, 8, 7, '#8ACB86'),
                         (8, 19, 5, '#5FA65E'), (26, 19, 5.4, '#69B168')):
        s += f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{c}"/>'
    for cx, cy in ((12, 10), (22, 8), (17, 15), (26, 15)):
        s += f'<circle cx="{cx}" cy="{cy}" r="2.4" fill="#F7A9C0"/><circle cx="{cx}" cy="{cy}" r="0.9" fill="#FFE08A"/>'
    s += '<path d="M6,25 H28 L25,43 Q24.8,44 23.8,44 H10.2 Q9.2,44 9,43 Z" fill="#C8714A"/>'
    s += '<path d="M9.6,27 H14 L14.8,43.6 H11 Z" fill="#DE9068" opacity="0.75"/>'
    s += '<rect x="3.5" y="20.5" width="27" height="6.4" rx="2.2" fill="#DB8A62"/>'
    return s + '</g>'


def peti(ox=0.0, oy=0.0):
    s = f'<g transform="translate({ox} {oy})">'
    s += '<rect x="1" y="1" width="44" height="44" rx="3" fill="#B98450"/>'
    for y in (15.5, 23, 30.5):
        s += f'<path d="M6,{y} H40" stroke="#A47242" stroke-width="1" />'
    s += '<path d="M8.5,37.5 L37.5,8.5" stroke="#A9773F" stroke-width="6.5" stroke-linecap="butt"/>'
    for x, y, w, h in ((1, 1, 44, 6.5), (1, 38.5, 44, 6.5), (1, 1, 6.5, 44), (38.5, 1, 6.5, 44)):
        s += f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="2" fill="#96653A"/>'
    s += '<rect x="3" y="2.2" width="40" height="1.4" rx="0.7" fill="#C99A63" opacity="0.9"/>'
    for x, y in ((4.3, 4.3), (41.7, 4.3), (4.3, 41.7), (41.7, 41.7)):
        s += f'<circle cx="{x}" cy="{y}" r="1.1" fill="#6F4826"/>'
    return s + '</g>'


def bunga():
    import math
    s = ''
    for i in range(5):
        a = math.radians(-90 + i * 72)
        cx, cy = 14 + 6.4 * math.cos(a), 14 + 6.4 * math.sin(a)
        s += f'<circle cx="{cx:.2f}" cy="{cy:.2f}" r="5.6" fill="#F78FB3"/>'
        s += f'<circle cx="{cx:.2f}" cy="{cy:.2f}" r="2.6" fill="#FBB7CE"/>'
    s += '<circle cx="14" cy="14" r="4.3" fill="#FFD35C"/><circle cx="13" cy="13" r="1.4" fill="#FFE9A6"/>'
    return s


# ------------------------------------------------------------------ ikon

def ikon():
    """Latar depan ikon adaptif 108x108: potret avatar dengan buket."""
    s = '<g transform="translate(52 55) scale(1.04) translate(-49 -36)">'
    s += rambut_belakang(1, 0)
    s += badan()
    s += kepala('senyum')
    s += '</g>'
    s += '<g transform="translate(71 79) scale(0.9)">' + buket(0, 0, 14) + '</g>'
    return s


# ---------------------------------------------------------------- render

def svg(w, h, isi):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w * SKALA}" height="{h * SKALA}" '
            f'viewBox="0 0 {w} {h}">{isi}</svg>')


def render(teks_svg, nama, garis=2):
    """Render SVG ke PNG transparan lalu tambahkan garis tepi gelap."""
    with tempfile.TemporaryDirectory() as tmp:
        f_svg, f_png = os.path.join(tmp, 'a.svg'), os.path.join(tmp, 'a.png')
        with open(f_svg, 'w') as f:
            f.write(teks_svg)
        subprocess.run(['rsvg-convert', f_svg, '-o', f_png], check=True)
        im = Image.open(f_png).convert('RGBA')
    if garis:
        alfa = im.split()[3]
        lebar = alfa.filter(ImageFilter.MaxFilter(2 * garis + 1)).filter(ImageFilter.GaussianBlur(0.7))
        tepi = Image.new('RGBA', im.size, GARIS_LUAR + (0,))
        tepi.putalpha(lebar)
        im = Image.alpha_composite(tepi, im)
    im.save(os.path.join(OUT, nama + '.png'), optimize=True)
    return im


def main():
    os.makedirs(OUT, exist_ok=True)
    hasil = {}
    for nama in POSE:
        hasil['avatar_' + nama] = render(svg(96, 120, avatar(nama)), 'avatar_' + nama)
    hasil['rintangan_pot'] = render(svg(34, 44, pot()), 'rintangan_pot')
    hasil['rintangan_peti'] = render(svg(46, 46, peti()), 'rintangan_peti')
    hasil['rintangan_tinggi'] = render(svg(46, 76, peti(0, 30) + pot(11.4, 0.6, 0.68)), 'rintangan_tinggi')
    hasil['bunga'] = render(svg(28, 28, bunga()), 'bunga')
    # ikon: 108 satuan pada 4 piksel per satuan = 432 piksel
    global SKALA
    SKALA = 4
    hasil['ic_launcher_foreground'] = render(svg(108, 108, ikon()), 'ic_launcher_foreground', garis=3)
    SKALA = 3
    for nama, im in hasil.items():
        print(f'{nama}.png {im.size[0]}x{im.size[1]} {os.path.getsize(os.path.join(OUT, nama + ".png"))} byte')

    if len(sys.argv) > 1:  # lembar pratinjau di atas warna langit dan tanah
        lembar = Image.new('RGBA', (1500, 860), (191, 230, 255, 255))
        tanah = Image.new('RGBA', (1500, 60), (124, 192, 106, 255))
        x = 10
        for nama in POSE:
            lembar.alpha_composite(hasil['avatar_' + nama], (x, 20))
            x += 296
        lembar.alpha_composite(tanah, (0, 374))
        x = 1200
        for nama in ('rintangan_pot', 'rintangan_peti', 'rintangan_tinggi', 'bunga'):
            im = hasil[nama]
            lembar.alpha_composite(im, (x if nama != 'bunga' else 1210, 374 - im.size[1] if nama != 'bunga' else 40))
            if nama != 'bunga':
                x += im.size[0] + 12 if x + im.size[0] + 160 < 1500 else 0
        lembar.alpha_composite(tanah, (0, 800))
        # ikon di atas latar ikon, dengan masker lingkaran
        ik = Image.new('RGBA', (432, 432), (169, 220, 247, 255))
        ik.alpha_composite(hasil['ic_launcher_foreground'])
        masker = Image.new('L', (432, 432), 0)
        from PIL import ImageDraw
        ImageDraw.Draw(masker).ellipse((72, 72, 360, 360), fill=255)
        ik.putalpha(masker)
        lembar.alpha_composite(ik.crop((72, 72, 360, 360)), (20, 450))
        # avatar ukuran sebenarnya di layar kecil (1/3) untuk cek keterbacaan
        x = 360
        for nama in POSE:
            kecil = hasil['avatar_' + nama].resize((96, 120), Image.LANCZOS)
            lembar.alpha_composite(kecil, (x, 680))
            x += 110
        for nama in ('rintangan_pot', 'rintangan_peti', 'rintangan_tinggi', 'bunga'):
            im = hasil[nama]
            kecil = im.resize((im.size[0] // 3, im.size[1] // 3), Image.LANCZOS)
            lembar.alpha_composite(kecil, (x, 800 - kecil.size[1]))
            x += kecil.size[0] + 14
        lembar.convert('RGB').save(os.path.join(sys.argv[1], 'pratinjau_aset.png'))


if __name__ == '__main__':
    main()
