#!/usr/bin/env python3
"""Genereert alle logo- en app-icoonbestanden van Noeroel Islam.

Het beeldmerk is hieronder als geometrie vastgelegd; de woordmark wordt uit het font Fraunces omgezet
naar paden, zodat de SVG's niet afhankelijk zijn van geïnstalleerde fonts.

Uitvoer:
- design/branding/svg/   vector-bronbestanden van alle varianten
- design/branding/png/   rasterexports (store-iconen, previews)
- frontend/app/...        Android- en iOS-app-iconen en de assets die de app laadt
- frontend/webportaal/... favicon en logo's voor het portaal

Gebruik: python3 design/branding/build_branding.py   (vereist design/requirements.txt en de fonts uit
design/fonts/build_fonts.py)
"""
from __future__ import annotations

import io
import json
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

NAME = TOKENS["brand"]["name"]["$value"]


def palette(group: str, shade: str) -> str:
    return TOKENS["color"]["palette"][group][shade]["$value"]


GREEN_800 = palette("green", "800")  # donkergroen
GREEN_500 = palette("green", "500")  # jadegroen
GOLD_400 = palette("gold", "400")  # gedempt goud
IVORY = palette("neutral", "50")  # warm ivoor
BLACK = palette("neutral", "950")
WHITE = palette("neutral", "0")

# ---------------------------------------------------------------------------
# Beeldmerk op een raster van 96 x 96.
# Een minimalistische mihrab-boog, onderaan open als uitnodigende doorgang, met daarin een vlam:
# licht ("noer") en kennis. De vlam heeft een eigen kleur zodat het merk ook klein herkenbaar blijft.
# ---------------------------------------------------------------------------
MARK_SIZE = 96
ARCH_STROKE = 8
ARCH_PATH = "M22 87V47C22 30 34 19.5 48 10C62 19.5 74 30 74 47V87"
FLAME_PATH = "M48 31C54.5 40.5 60 47 60 56.5A12 12 0 0 1 36 56.5C36 47 41.5 40.5 48 31Z"
# Een dunne lijn onder de vlam verbindt de twee zijden van de boog: verbinding met de gemeenschap.
BASE_PATH = "M37 78.5H59"
BASE_STROKE = 4.5


def mark_group(arch: str, flame: str, base: str | None = None) -> str:
    base = base or arch
    return (
        f'<path d="{ARCH_PATH}" fill="none" stroke="{arch}" stroke-width="{ARCH_STROKE}" '
        f'stroke-linecap="round" stroke-linejoin="round"/>'
        f'<path d="{BASE_PATH}" fill="none" stroke="{base}" stroke-width="{BASE_STROKE}" stroke-linecap="round"/>'
        f'<path d="{FLAME_PATH}" fill="{flame}"/>'
    )


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


def write(path: Path, content: str | bytes) -> None:
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
        "logo-horizontal-light.svg": horizontal(GREEN_800, GOLD_400, GREEN_800),
        "logo-horizontal-dark.svg": horizontal(IVORY, GOLD_400, IVORY),
        "logo-horizontal-mono-black.svg": horizontal(BLACK, BLACK, BLACK),
        "logo-horizontal-mono-white.svg": horizontal(WHITE, WHITE, WHITE),
        "logo-stacked-light.svg": stacked(GREEN_800, GOLD_400, GREEN_800),
        "logo-stacked-dark.svg": stacked(IVORY, GOLD_400, IVORY),
        "logo-compact-light.svg": compact(GREEN_800, GOLD_400),
        "logo-compact-dark.svg": compact(IVORY, GOLD_400),
        "logo-compact-mono-black.svg": compact(BLACK, BLACK),
        "logo-compact-mono-white.svg": compact(WHITE, WHITE),
        # Volledig vlak; iOS en de stores ronden zelf de hoeken af.
        "app-icon.svg": icon(1024, GREEN_800, IVORY, GOLD_400, 0.6),
        # Android adaptive icon: voorgrond binnen de veilige zone (66 van 108 dp), achtergrond als vlak.
        "app-icon-foreground.svg": icon(432, None, IVORY, GOLD_400, 0.46),
        "app-icon-background.svg": svg(432, 432, f'<rect width="432" height="432" fill="{GREEN_800}"/>', f"{NAME} app-icoon achtergrond"),
        "app-icon-monochrome.svg": icon(432, None, WHITE, WHITE, 0.46),
        "favicon.svg": icon(64, GREEN_800, IVORY, GOLD_400, 0.8, radius=14),
    }
    print("SVG:")
    for name, content in variants.items():
        write(SVG_OUT / name, content)

    print("PNG (design/branding/png):")
    write(PNG_OUT / "app-icon-1024.png", png(variants["app-icon.svg"], 1024, opaque_bg=GREEN_800))
    write(PNG_OUT / "play-store-icon-512.png", png(variants["app-icon.svg"], 512, opaque_bg=GREEN_800))
    write(PNG_OUT / "logo-horizontal-light@2x.png", png(variants["logo-horizontal-light.svg"], 0, 192))
    write(PNG_OUT / "logo-horizontal-dark@2x.png", png(variants["logo-horizontal-dark.svg"], 0, 192))

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
            write(folder / "ic_launcher.png", png(icon(192, GREEN_800, IVORY, GOLD_400, 0.6, radius=40), int(48 * factor)))
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
        for old in ios_icons.glob("*.png"):
            old.unlink()
        write(ios_icons / "Icon-App-1024x1024@1x.png", png(variants["app-icon.svg"], 1024, opaque_bg=GREEN_800))
        contents = {
            "images": [
                {"filename": "Icon-App-1024x1024@1x.png", "idiom": "universal", "platform": "ios", "size": "1024x1024"}
            ],
            "info": {"author": "xcode", "version": 1},
        }
        write(ios_icons / "Contents.json", json.dumps(contents, indent=2) + "\n")

    print("Webportaal:")
    public = PORTAL / "public"
    write(public / "favicon.svg", variants["favicon.svg"])
    write(public / "apple-touch-icon.png", png(variants["app-icon.svg"], 180, opaque_bg=GREEN_800))
    for name in ["logo-horizontal-light.svg", "logo-horizontal-dark.svg", "logo-compact-light.svg", "logo-compact-dark.svg"]:
        write(public / "branding" / name, variants[name])


if __name__ == "__main__":
    main()
