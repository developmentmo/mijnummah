#!/usr/bin/env python3
"""Genereert alle logo- en app-icoonbestanden van een moskee-app uit haar merkconfiguratie.

Het beeldmerk komt uit brands/<slug>/<logo.mark> (SVG op 96 x 96, gecontroleerd door tools/brand/brand.mjs) en wordt
per variant herkleurd; de woordmark wordt uit het font Fraunces omgezet naar paden, zodat de SVG's niet afhankelijk
zijn van geïnstalleerde fonts.

Uitvoer:
- design/branding/svg/   vector-bronbestanden van alle varianten
- design/branding/png/   rasterexports (store-iconen, previews)
- frontend/app/...        Android- en iOS-app-iconen en de assets die de app laadt
- frontend/webportaal/... favicon en logo's voor het portaal

Gebruik: python3 design/branding/build_branding.py [--brand=<slug>]   (standaard het merk uit brands/index.json;
vereist design/requirements.txt). Normaal via `node tools/brand/brand.mjs apply <slug>`, dat eerst valideert.
"""
from __future__ import annotations

import io
import json
import os
import re
import sys
from pathlib import Path

import cairosvg
import uharfbuzz as hb
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

BRANDING = Path(__file__).resolve().parent
ROOT = BRANDING.parent.parent
TOKENS = json.loads((ROOT / "design/tokens/tokens.json").read_text(encoding="utf-8"))
WORDMARK_FONT = ROOT / "frontend/app/assets/fonts/Fraunces-SemiBold.ttf"

SVG_OUT = BRANDING / "svg"
PNG_OUT = BRANDING / "png"
APP = ROOT / "frontend/app"
PORTAL = ROOT / "frontend/webportaal"


def selected_brand() -> str:
    for arg in sys.argv[1:]:
        if arg.startswith("--brand="):
            return arg.split("=", 1)[1]
    return os.environ.get("BRAND") or json.loads((ROOT / "brands/index.json").read_text(encoding="utf-8"))["default"]


BRAND_SLUG = selected_brand()
if not re.fullmatch(r"[a-z0-9]+(-[a-z0-9]+)*", BRAND_SLUG):
    sys.exit(f"Ongeldige merknaam: {BRAND_SLUG}")
BRAND_DIR = ROOT / "brands" / BRAND_SLUG
BRAND = json.loads((BRAND_DIR / "brand.json").read_text(encoding="utf-8"))
NAME = BRAND["name"]


def palette(group: str, shade: str) -> str:
    """Merkkleuren uit brand.json; neutral uit brand.json als het merk die heeft, anders uit tokens.json."""
    if group in BRAND["colors"]:
        return BRAND["colors"][group][shade]
    return TOKENS["color"]["palette"][group][shade]["$value"]


PRIMARY_800 = palette("primary", "800")  # donkerste merktint
ACCENT_400 = palette("accent", "400")  # accent
IVORY = palette("neutral", "50")  # lichte achtergrond
BLACK = palette("neutral", "950")
WHITE = palette("neutral", "0")

# ---------------------------------------------------------------------------
# Beeldmerk op een raster van 96 x 96, uit brands/<slug>/. Het bestand gebruikt primary.800 en accent.400;
# per variant worden die vervangen door de kleuren van die variant.
# ---------------------------------------------------------------------------
MARK_SIZE = 96
_mark_svg = (BRAND_DIR / BRAND["logo"]["mark"]).read_text(encoding="utf-8")
_mark_svg = re.sub(r"<!--.*?-->", "", _mark_svg, flags=re.S)
MARK_BODY = "".join(
    line.strip() for line in re.sub(r"</svg>.*$", "", re.sub(r"^.*?<svg[^>]*>", "", _mark_svg, flags=re.S), flags=re.S).split("\n")
)
_MARK_COLORS = re.compile(f"({re.escape(PRIMARY_800)}|{re.escape(ACCENT_400)})", re.I)


def mark_group(primary: str, accent: str) -> str:
    """Het beeldmerk met primary.800 vervangen door `primary` en accent.400 door `accent`."""
    return _MARK_COLORS.sub(lambda m: primary if m.group(1).upper() == PRIMARY_800.upper() else accent, MARK_BODY)


# ---------------------------------------------------------------------------
# Woordmark
# ---------------------------------------------------------------------------


def wordmark_path(text: str, font_size: float, letter_spacing_em: float = -0.01) -> tuple[str, float, float, float]:
    """Zet tekst om naar één SVG-pad. Geeft (pad, breedte, cap-hoogte, descender) terug in px."""
    blob = hb.Blob.from_file_path(str(WORDMARK_FONT))
    face = hb.Face(blob)
    font = hb.Font(face)
    buf = hb.Buffer()
    buf.add_str(text)
    buf.guess_segment_properties()
    hb.shape(font, buf, {"kern": True, "liga": True})

    tt = TTFont(WORDMARK_FONT)
    upem = tt["head"].unitsPerEm
    scale = font_size / upem
    glyph_set = tt.getGlyphSet()
    order = tt.getGlyphOrder()
    cap_height = getattr(tt["OS/2"], "sCapHeight", 0) or 0.7 * upem
    spacing = letter_spacing_em * upem

    pen = SVGPathPen(glyph_set)
    x = 0.0
    for info, pos in zip(buf.glyph_infos, buf.glyph_positions):
        name = order[info.codepoint]
        # Spiegelen over de y-as: fonts tellen omhoog, SVG telt omlaag. Basislijn op y = 0.
        tpen = TransformPen(pen, (scale, 0, 0, -scale, (x + pos.x_offset) * scale, -pos.y_offset * scale))
        glyph_set[name].draw(tpen)
        x += pos.x_advance + spacing
    width = (x - spacing) * scale
    return pen.getCommands(), width, cap_height * scale, abs(tt["OS/2"].sTypoDescender) * scale


def svg(width: float, height: float, body: str, title: str) -> str:
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {fmt(width)} {fmt(height)}" '
        f'width="{fmt(width)}" height="{fmt(height)}" role="img" aria-labelledby="title">'
        f'<title id="title">{title}</title>{body}</svg>\n'
    )


def fmt(n: float) -> str:
    return f"{n:.2f}".rstrip("0").rstrip(".")


def horizontal(arch: str, flame: str, text: str) -> str:
    font_size = 46
    path, text_width, cap, _ = wordmark_path(NAME, font_size)
    gap = 18
    height = MARK_SIZE
    baseline = 48 + cap / 2 + 4  # optisch gecentreerd op de boog
    width = MARK_SIZE + gap + text_width + 6
    body = (
        mark_group(arch, flame)
        + f'<g transform="translate({fmt(MARK_SIZE + gap)} {fmt(baseline)})"><path d="{path}" fill="{text}"/></g>'
    )
    return svg(width, height, body, NAME)


def stacked(arch: str, flame: str, text: str) -> str:
    font_size = 40
    path, text_width, cap, desc = wordmark_path(NAME, font_size)
    pad = 8
    width = max(text_width, MARK_SIZE) + 2 * pad
    mark_x = (width - MARK_SIZE) / 2
    baseline = MARK_SIZE + 18 + cap
    height = baseline + desc * 0.6 + pad
    body = (
        f'<g transform="translate({fmt(mark_x)} 0)">{mark_group(arch, flame)}</g>'
        f'<g transform="translate({fmt((width - text_width) / 2)} {fmt(baseline)})"><path d="{path}" fill="{text}"/></g>'
    )
    return svg(width, height, body, NAME)


def compact(arch: str, flame: str) -> str:
    return svg(MARK_SIZE, MARK_SIZE, mark_group(arch, flame), NAME)


def icon(size: int, background: str | None, arch: str, flame: str, mark_scale: float, radius: float = 0) -> str:
    """App-icoon: beeldmerk gecentreerd op een vlak. mark_scale is de beeldmerkbreedte t.o.v. het icoon."""
    scale = size * mark_scale / MARK_SIZE
    offset = (size - MARK_SIZE * scale) / 2
    # Iets omhoog, zodat de open onderkant van de boog optisch in balans is.
    lift = size * 0.015
    bg = f'<rect width="{size}" height="{size}" rx="{fmt(radius)}" fill="{background}"/>' if background else ""
    body = bg + f'<g transform="translate({fmt(offset)} {fmt(offset - lift)}) scale({fmt(scale)})">{mark_group(arch, flame)}</g>'
    return svg(size, size, body, f"{NAME} app-icoon")


# ---------------------------------------------------------------------------
# Rasteren en wegschrijven
# ---------------------------------------------------------------------------


CHECK = "--check" in sys.argv[1:]
STALE: list[str] = []


def write(path: Path, content: str | bytes) -> None:
    if CHECK:
        # Alleen tekstbestanden (SVG, JSON, XML) zijn reproduceerbaar; rasterbestanden hangen af van de cairo-versie.
        if isinstance(content, str) and (not path.exists() or path.read_text(encoding="utf-8") != content):
            STALE.append(str(path.relative_to(ROOT)))
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(content, str):
        path.write_text(content, encoding="utf-8")
    else:
        path.write_bytes(content)
    print(f"  {path.relative_to(ROOT)}")


def png(svg_text: str, width: int, height: int | None = None, opaque_bg: str | None = None) -> bytes:
    data = cairosvg.svg2png(
        bytestring=svg_text.encode(), output_width=width, output_height=height or width, background_color=opaque_bg
    )
    if opaque_bg:
        # App Store-iconen mogen geen alfakanaal hebben: omzetten naar RGB.
        from PIL import Image  # cairosvg installeert Pillow als dependency

        img = Image.open(io.BytesIO(data)).convert("RGB")
        out = io.BytesIO()
        img.save(out, format="PNG", optimize=True)
        return out.getvalue()
    return data


def main() -> None:
    variants = {
        "logo-horizontal-light.svg": horizontal(PRIMARY_800, ACCENT_400, PRIMARY_800),
        "logo-horizontal-dark.svg": horizontal(IVORY, ACCENT_400, IVORY),
        "logo-horizontal-mono-black.svg": horizontal(BLACK, BLACK, BLACK),
        "logo-horizontal-mono-white.svg": horizontal(WHITE, WHITE, WHITE),
        "logo-stacked-light.svg": stacked(PRIMARY_800, ACCENT_400, PRIMARY_800),
        "logo-stacked-dark.svg": stacked(IVORY, ACCENT_400, IVORY),
        "logo-compact-light.svg": compact(PRIMARY_800, ACCENT_400),
        "logo-compact-dark.svg": compact(IVORY, ACCENT_400),
        "logo-compact-mono-black.svg": compact(BLACK, BLACK),
        "logo-compact-mono-white.svg": compact(WHITE, WHITE),
        # Volledig vlak; iOS en de stores ronden zelf de hoeken af.
        "app-icon.svg": icon(1024, PRIMARY_800, IVORY, ACCENT_400, 0.6),
        # Android adaptive icon: voorgrond binnen de veilige zone (66 van 108 dp), achtergrond als vlak.
        "app-icon-foreground.svg": icon(432, None, IVORY, ACCENT_400, 0.46),
        "app-icon-background.svg": svg(432, 432, f'<rect width="432" height="432" fill="{PRIMARY_800}"/>', f"{NAME} app-icoon achtergrond"),
        "app-icon-monochrome.svg": icon(432, None, WHITE, WHITE, 0.46),
        "favicon.svg": icon(64, PRIMARY_800, IVORY, ACCENT_400, 0.8, radius=14),
    }
    if not CHECK:
        print("SVG:")
    for name, content in variants.items():
        write(SVG_OUT / name, content)

    if not CHECK:
        print("PNG (design/branding/png):")
    write(PNG_OUT / "app-icon-1024.png", png(variants["app-icon.svg"], 1024, opaque_bg=PRIMARY_800))
    write(PNG_OUT / "play-store-icon-512.png", png(variants["app-icon.svg"], 512, opaque_bg=PRIMARY_800))
    write(PNG_OUT / "logo-horizontal-light@2x.png", png(variants["logo-horizontal-light.svg"], 0, 192))
    write(PNG_OUT / "logo-horizontal-dark@2x.png", png(variants["logo-horizontal-dark.svg"], 0, 192))

    if not CHECK:
        print("Flutter-app:")
    for name in [
        "logo-horizontal-light.svg",
        "logo-horizontal-dark.svg",
        "logo-stacked-light.svg",
        "logo-stacked-dark.svg",
        "logo-compact-light.svg",
        "logo-compact-dark.svg",
    ]:
        write(APP / "assets/branding" / name, variants[name])

    android_res = APP / "android/app/src/main/res"
    if android_res.exists():
        densities = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
        for density, factor in densities.items():
            folder = android_res / f"mipmap-{density}"
            write(folder / "ic_launcher.png", png(icon(192, PRIMARY_800, IVORY, ACCENT_400, 0.6, radius=40), int(48 * factor)))
            for layer in ("foreground", "background", "monochrome"):
                write(folder / f"ic_launcher_{layer}.png", png(variants[f"app-icon-{layer}.svg"], int(108 * factor)))
        adaptive = (
            '<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@mipmap/ic_launcher_background"/>\n'
            '    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>\n'
            '    <monochrome android:drawable="@mipmap/ic_launcher_monochrome"/>\n'
            "</adaptive-icon>\n"
        )
        write(android_res / "mipmap-anydpi-v26/ic_launcher.xml", adaptive)

    ios_icons = APP / "ios/Runner/Assets.xcassets/AppIcon.appiconset"
    if ios_icons.exists():
        for old in ios_icons.glob("*.png") if not CHECK else []:
            old.unlink()
        write(ios_icons / "Icon-App-1024x1024@1x.png", png(variants["app-icon.svg"], 1024, opaque_bg=PRIMARY_800))
        contents = {
            "images": [
                {"filename": "Icon-App-1024x1024@1x.png", "idiom": "universal", "platform": "ios", "size": "1024x1024"}
            ],
            "info": {"author": "xcode", "version": 1},
        }
        write(ios_icons / "Contents.json", json.dumps(contents, indent=2) + "\n")

    if not CHECK:
        print("Webportaal:")
    public = PORTAL / "public"
    write(public / "favicon.svg", variants["favicon.svg"])
    write(public / "apple-touch-icon.png", png(variants["app-icon.svg"], 180, opaque_bg=PRIMARY_800))
    for name in ["logo-horizontal-light.svg", "logo-horizontal-dark.svg", "logo-compact-light.svg", "logo-compact-dark.svg"]:
        write(public / "branding" / name, variants[name])


if __name__ == "__main__":
    main()
    if CHECK:
        for stale in STALE:
            print(f"Niet actueel voor merk {BRAND_SLUG}: {stale}", file=sys.stderr)
        sys.exit(1 if STALE else 0)
