#!/usr/bin/env python3
"""Maakt uit de variabele bronfonts de bestanden die de apps gebruiken.

- Flutter: statische TTF-instanties per gewicht (Flutter koppelt FontWeight niet aan de wght-as).
- Webportaal: WOFF2-bestanden van de variabele fonts (kleiner, alle gewichten in een bestand).

Gebruik: python3 design/fonts/build_fonts.py   (vereist design/requirements.txt)
"""
from pathlib import Path

from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

FONTS = Path(__file__).resolve().parent
ROOT = FONTS.parent.parent
FLUTTER_OUT = ROOT / "frontend/app/assets/fonts"
WEB_OUT = ROOT / "frontend/webportaal/public/fonts"

# Fraunces: zachte, rustige vorm (SOFT 50, WONK 0) met een optische maat voor koppen.
FRAUNCES_AXES = {"SOFT": 50, "WONK": 0}
FRAUNCES_OPSZ = 36

STATIC = [
    # (bron, doelnaam, as-instellingen)
    ("fraunces/Fraunces-VF.ttf", "Fraunces-Medium.ttf", {**FRAUNCES_AXES, "opsz": FRAUNCES_OPSZ, "wght": 500}),
    ("fraunces/Fraunces-VF.ttf", "Fraunces-SemiBold.ttf", {**FRAUNCES_AXES, "opsz": FRAUNCES_OPSZ, "wght": 600}),
    ("inter/Inter-VF.ttf", "Inter-Regular.ttf", {"opsz": 14, "wght": 400}),
    ("inter/Inter-VF.ttf", "Inter-Medium.ttf", {"opsz": 14, "wght": 500}),
    ("inter/Inter-VF.ttf", "Inter-SemiBold.ttf", {"opsz": 14, "wght": 600}),
    ("inter/Inter-VF.ttf", "Inter-Bold.ttf", {"opsz": 14, "wght": 700}),
    ("inter/Inter-Italic-VF.ttf", "Inter-Italic.ttf", {"opsz": 14, "wght": 400}),
    ("noto-naskh-arabic/NotoNaskhArabic-VF.ttf", "NotoNaskhArabic-Regular.ttf", {"wght": 400}),
    ("noto-naskh-arabic/NotoNaskhArabic-VF.ttf", "NotoNaskhArabic-Bold.ttf", {"wght": 700}),
]

WEB = [
    # Fraunces houdt alleen de gewichts- en optische-maat-as; SOFT en WONK liggen vast.
    ("fraunces/Fraunces-VF.ttf", "Fraunces-VF.woff2", FRAUNCES_AXES),
    ("inter/Inter-VF.ttf", "Inter-VF.woff2", {}),
    ("inter/Inter-Italic-VF.ttf", "Inter-Italic-VF.woff2", {}),
    ("noto-naskh-arabic/NotoNaskhArabic-VF.ttf", "NotoNaskhArabic-VF.woff2", {}),
]

LICENSES = {"fraunces": "Fraunces-OFL.txt", "inter": "Inter-OFL.txt", "noto-naskh-arabic": "NotoNaskhArabic-OFL.txt"}


def rename(font: TTFont, file_name: str) -> None:
    """Geeft een statische instantie een eigen naam, zodat hij niet botst met het variabele font."""
    family, style = Path(file_name).stem.split("-", 1)
    names = font["name"]
    names.removeNames(nameID=16)
    names.removeNames(nameID=17)
    for name_id, value in {1: family, 2: style, 4: f"{family} {style}", 6: f"{family}-{style}"}.items():
        names.setName(value, name_id, 3, 1, 0x409)
        names.setName(value, name_id, 1, 0, 0)


def main() -> None:
    FLUTTER_OUT.mkdir(parents=True, exist_ok=True)
    WEB_OUT.mkdir(parents=True, exist_ok=True)
    for src, name, axes in STATIC:
        font = instantiateVariableFont(TTFont(FONTS / src), axes)
        rename(font, name)
        font.save(FLUTTER_OUT / name)
        print(f"Flutter: {name}")
    for src, name, axes in WEB:
        font = TTFont(FONTS / src)
        if axes:
            font = instantiateVariableFont(font, axes, updateFontNames=False)
        font.flavor = "woff2"
        font.save(WEB_OUT / name)
        print(f"Web: {name}")
    for folder, name in LICENSES.items():
        text = (FONTS / folder / "OFL.txt").read_text(encoding="utf-8")
        for out in (FLUTTER_OUT, WEB_OUT):
            (out / name).write_text(text, encoding="utf-8")


if __name__ == "__main__":
    main()
