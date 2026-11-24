#!/usr/bin/env bash
# Menguji aturan main dan merender contoh layar di komputer, tanpa HP.
# Kode game yang asli (GameWorld, GameView) dijalankan dengan kelas Android
# tiruan di folder tiruan/, yang mengubah perintah gambar menjadi SVG.
#
# Butuh: JDK (javac, java) dan rsvg-convert.
# Pakai: alat/pratinjau/jalankan.sh [folder_keluaran]
set -euo pipefail

AKAR="$(cd "$(dirname "$0")/../.." && pwd)"
KELUAR="${1:-/tmp/pratinjau-lompat}"
RES="$AKAR/app/src/main/res"
KODE="$AKAR/app/src/main/java/com/proyekapk/lompat"
BANGUN="$(mktemp -d)"
trap 'rm -rf "$BANGUN"' EXIT

mkdir -p "$KELUAR" "$BANGUN/kelas"

# Kelas R tiruan, dibuat dari nama file gambar yang ada
{
  echo "package com.proyekapk.lompat;"
  echo "public final class R { public static final class drawable {"
  i=1
  for f in "$RES"/drawable-nodpi/*.png; do
    echo "  public static final int $(basename "$f" .png) = $i;"
    i=$((i + 1))
  done
  echo "} }"
} > "$BANGUN/R.java"

# shellcheck disable=SC2046
javac -nowarn -d "$BANGUN/kelas" \
  $(find "$AKAR/alat/pratinjau/tiruan" -name '*.java') \
  "$BANGUN/R.java" "$KODE/GameWorld.java" "$KODE/GameView.java" \
  "$AKAR/alat/pratinjau/Pratinjau.java"

cp "$RES"/drawable-nodpi/*.png "$KELUAR"/
java -cp "$BANGUN/kelas" -Dpratinjau.res="$RES" com.proyekapk.lompat.Pratinjau "$KELUAR"

for f in "$KELUAR"/layar_*.svg; do
  rsvg-convert "$f" -o "${f%.svg}.png"
done
echo "Gambar contoh ada di $KELUAR"
