#!/usr/bin/env python3
"""Memeriksa file APK tanpa Android SDK: keutuhan ZIP, isi manifest, dan
keabsahan tanda tangan APK v2 (termasuk kecocokan dengan debug.keystore repo).

Butuh paket Python `cryptography`. Pakai:

    python3 -I alat/periksa_apk.py berkas.apk [nama.paket.yang.diharapkan]

Keluar dengan kode 1 jika ada pemeriksaan yang gagal.
"""
import hashlib
import io
import struct
import sys
import zipfile

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding

# Sidik jari SHA-256 sertifikat di debug.keystore repo ini
SIDIK_JARI_DEBUG = '13:DE:AD:2F:DB:B0:14:BB:70:13:60:64:09:81:16:C2:D2:93:1E:D9:68:B8:8D:2F:0B:28:81:83:71:48:48:86'

gagal = []


def cek(syarat, pesan):
    print(('  ok    ' if syarat else '  GAGAL ') + pesan)
    if not syarat:
        gagal.append(pesan)


def baca_manifest(b):
    """Mengurai AndroidManifest biner menjadi daftar (nama_elemen, atribut)."""
    hasil, teks, pos = [], [], 8
    while pos < len(b):
        jenis, kepala, ukuran = struct.unpack_from('<HHI', b, pos)
        if jenis == 0x0001:  # kumpulan teks
            jumlah, _, bendera, awal, _ = struct.unpack_from('<IIIII', b, pos + 8)
            utf8 = bool(bendera & 0x100)
            for o in struct.unpack_from('<%dI' % jumlah, b, pos + kepala):
                p = pos + awal + o
                if utf8:
                    n = b[p]; p += 1
                    if n & 0x80:
                        p += 1
                    n = b[p]; p += 1
                    if n & 0x80:
                        n = ((n & 0x7F) << 8) | b[p]; p += 1
                    teks.append(b[p:p + n].decode('utf-8', 'replace'))
                else:
                    n = struct.unpack_from('<H', b, p)[0]; p += 2
                    if n & 0x8000:
                        n = ((n & 0x7FFF) << 16) | struct.unpack_from('<H', b, p)[0]; p += 2
                    teks.append(b[p:p + n * 2].decode('utf-16le', 'replace'))
        elif jenis == 0x0102:  # awal elemen
            _, nama, awal_atr, ukuran_atr, jumlah_atr = struct.unpack_from('<iiHHH', b, pos + 16)
            atribut = {}
            for k in range(jumlah_atr):
                ap = pos + 16 + awal_atr + k * ukuran_atr
                _, anama, _, _, _, tipe, data = struct.unpack_from('<iiiHBBI', b, ap)
                if tipe == 0x03:
                    nilai = teks[data]
                elif tipe == 0x12:
                    nilai = bool(data)
                elif tipe in (0x10, 0x11):
                    nilai = data
                else:
                    nilai = '(tipe 0x%02x) 0x%x' % (tipe, data)
                atribut[teks[anama]] = nilai
            hasil.append((teks[nama], atribut))
        pos += ukuran
    return hasil


def lp(buf, pos):
    n = struct.unpack_from('<I', buf, pos)[0]
    return buf[pos + 4:pos + 4 + n], pos + 4 + n


def deret(buf):
    butir, pos = [], 0
    while pos < len(buf):
        b, pos = lp(buf, pos)
        butir.append(b)
    return butir


def main():
    berkas = sys.argv[1]
    paket_harapan = sys.argv[2] if len(sys.argv) > 2 else None
    data = open(berkas, 'rb').read()
    print('berkas : %s' % berkas)
    print('ukuran : %d byte' % len(data))
    print('sha256 : %s' % hashlib.sha256(data).hexdigest())

    print('ZIP')
    zf = zipfile.ZipFile(io.BytesIO(data))
    cek(zf.testzip() is None, 'semua entri utuh (%d entri)' % len(zf.namelist()))
    nama = zf.namelist()
    cek('AndroidManifest.xml' in nama and 'resources.arsc' in nama, 'ada manifest dan tabel resource')
    dex = [n for n in nama if n.startswith('classes') and n.endswith('.dex')]
    cek(bool(dex) and all(zf.read(n)[:4] == b'dex\n' for n in dex), 'ada kode (%s)' % ', '.join(dex))
    gambar = [n for n in nama if n.startswith('res/') and n.endswith('.png')]
    print('         gambar di dalam APK: %d' % len(gambar))

    print('Manifest')
    paket = None
    for elemen, atr in baca_manifest(zf.read('AndroidManifest.xml')):
        if elemen == 'manifest':
            paket = atr.get('package')
            print('         paket %s, versi %s (kode %s)' % (paket, atr.get('versionName'), atr.get('versionCode')))
        elif elemen == 'uses-sdk':
            print('         minSdk %s, targetSdk %s' % (atr.get('minSdkVersion'), atr.get('targetSdkVersion')))
        elif elemen == 'activity':
            print('         activity %s, exported=%s, orientasi=%s'
                  % (atr.get('name'), atr.get('exported'), atr.get('screenOrientation')))
        elif elemen == 'category':
            cek(atr.get('name') == 'android.intent.category.LAUNCHER', 'activity muncul di daftar aplikasi')
    cek(paket is not None and (paket_harapan is None or paket == paket_harapan), 'nama paket sesuai')
    kelas = ('L' + paket.replace('.', '/') + '/MainActivity;').encode()
    cek(any(kelas in zf.read(n) for n in dex), 'kelas MainActivity ada di dalam kode')

    print('Tanda tangan APK v2')
    eocd = data.rfind(b'PK\x05\x06')
    _, cd_awal = struct.unpack_from('<II', data, eocd + 12)
    ada_blok = data[cd_awal - 16:cd_awal] == b'APK Sig Block 42'
    cek(ada_blok, 'blok tanda tangan ada')
    if ada_blok:
        ukuran_blok = struct.unpack_from('<Q', data, cd_awal - 24)[0]
        blok_awal = cd_awal - ukuran_blok - 8
        pasangan, p = {}, blok_awal + 8
        while p < cd_awal - 24:
            n = struct.unpack_from('<Q', data, p)[0]
            pasangan[struct.unpack_from('<I', data, p + 8)[0]] = data[p + 12:p + 8 + n]
            p += 8 + n
        penanda = deret(lp(pasangan[0x7109871A], 0)[0])[0]
        data_ttd, pos = lp(penanda, 0)
        ttd_mentah, pos = lp(penanda, pos)
        kunci_der, pos = lp(penanda, pos)
        digest_mentah, q = lp(data_ttd, 0)
        sert_mentah, q = lp(data_ttd, q)
        digest = {struct.unpack_from('<I', d, 0)[0]: lp(d, 4)[0] for d in deret(digest_mentah)}
        sert = x509.load_der_x509_certificate(deret(sert_mentah)[0])
        sidik = sert.fingerprint(hashes.SHA256()).hex().upper()
        sidik = ':'.join(sidik[i:i + 2] for i in range(0, len(sidik), 2))
        print('         sertifikat %s' % sert.subject.rfc4514_string())
        cek(sidik == SIDIK_JARI_DEBUG, 'sertifikat sama dengan debug.keystore repo (versi baru bisa menimpa versi lama)')
        kunci = serialization.load_der_public_key(kunci_der)
        sah = False
        for s in deret(ttd_mentah):
            if struct.unpack_from('<I', s, 0)[0] == 0x0103:
                try:
                    kunci.verify(lp(s, 4)[0], data_ttd, padding.PKCS1v15(), hashes.SHA256())
                    sah = True
                except Exception:
                    sah = False
        cek(sah, 'tanda tangan RSA sah')
        ekor = bytearray(data[eocd:])
        struct.pack_into('<I', ekor, 16, blok_awal)
        potongan = []
        for bagian in (data[:blok_awal], data[cd_awal:eocd], bytes(ekor)):
            for o in range(0, len(bagian), 1 << 20):
                c = bagian[o:o + (1 << 20)]
                potongan.append(hashlib.sha256(b'\xa5' + struct.pack('<I', len(c)) + c).digest())
        puncak = hashlib.sha256(b'\x5a' + struct.pack('<I', len(potongan)) + b''.join(potongan)).digest()
        cek(digest.get(0x0103) == puncak, 'isi APK cocok dengan yang ditandatangani')

    print('HASIL: ' + ('ADA YANG GAGAL' if gagal else 'semua pemeriksaan lolos'))
    sys.exit(1 if gagal else 0)


if __name__ == '__main__':
    main()
