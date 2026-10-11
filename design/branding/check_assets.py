#!/usr/bin/env python3
"""Controleert de merkassets zonder extra dependencies (alleen de Python-standaardbibliotheek).

- SVG's zijn geldige XML met een viewBox, zonder <text>, scripts, afbeeldingen of externe verwijzingen.
- App-iconen hebben de juiste afmetingen; het iOS-icoon heeft geen alfakanaal (eis van de App Store).
- De kopieën in de apps zijn gelijk aan de bron in design/branding/svg.
"""
import struct
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SVG_NS = "{http://www.w3.org/2000/svg}"
FORBIDDEN = {"text", "script", "image", "foreignObject", "style"}
errors: list[str] = []


def check_svg(path: Path) -> None:
    rel = path.relative_to(ROOT)
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError as e:
        errors.append(f"{rel}: geen geldige XML ({e})")
        return
    if root.tag != f"{SVG_NS}svg":
        errors.append(f"{rel}: hoofdelement is geen <svg> met de SVG-namespace")
    if "viewBox" not in root.attrib:
        errors.append(f"{rel}: viewBox ontbreekt")
    if root.find(f"{SVG_NS}title") is None:
        errors.append(f"{rel}: <title> ontbreekt (toegankelijke naam)")
    for el in root.iter():
        tag = el.tag.replace(SVG_NS, "")
        if tag in FORBIDDEN:
            errors.append(f"{rel}: verboden element <{tag}>")
        for key, value in el.attrib.items():
            if key.endswith("href") or "url(" in value:
                errors.append(f"{rel}: externe of interne verwijzing in {key}")


def png_info(path: Path) -> tuple[int, int, int]:
    data = path.read_bytes()[:26]
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("geen PNG")
    width, height, _depth, color_type = struct.unpack(">IIBB", data[16:26])
    return width, height, color_type


def check_png(path: Path, size: int, opaque: bool = False) -> None:
    rel = path.relative_to(ROOT)
    if not path.exists():
        errors.append(f"{rel}: ontbreekt")
        return
    width, height, color_type = png_info(path)
    if (width, height) != (size, size):
        errors.append(f"{rel}: {width}x{height}, verwacht {size}x{size}")
    if opaque and color_type in (4, 6):
        errors.append(f"{rel}: heeft een alfakanaal, maar moet ondoorzichtig zijn")


def main() -> None:
    sources = sorted((ROOT / "design/branding/svg").glob("*.svg"))
    if not sources:
        errors.append("design/branding/svg bevat geen SVG's")
    for svg in sources:
        check_svg(svg)

    copies = list((ROOT / "frontend/app/assets/branding").glob("*.svg")) + list(
        (ROOT / "frontend/webportaal/public/branding").glob("*.svg")
    )
    for copy in copies:
        check_svg(copy)
        source = ROOT / "design/branding/svg" / copy.name
        if not source.exists() or source.read_bytes() != copy.read_bytes():
            errors.append(f"{copy.relative_to(ROOT)}: wijkt af van design/branding/svg; draai build_branding.py")

    check_png(ROOT / "design/branding/png/app-icon-1024.png", 1024, opaque=True)
    check_png(ROOT / "design/branding/png/play-store-icon-512.png", 512, opaque=True)
    check_png(ROOT / "frontend/app/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-1024x1024@1x.png", 1024, opaque=True)
    res = ROOT / "frontend/app/android/app/src/main/res"
    for density, factor in {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}.items():
        check_png(res / f"mipmap-{density}/ic_launcher.png", int(48 * factor))
        for layer in ("foreground", "background", "monochrome"):
            check_png(res / f"mipmap-{density}/ic_launcher_{layer}.png", int(108 * factor))
    check_png(ROOT / "frontend/webportaal/public/apple-touch-icon.png", 180, opaque=True)

    if errors:
        print("\n".join(errors), file=sys.stderr)
        sys.exit(1)
    print(f"{len(sources)} bron-SVG's, {len(copies)} kopieën en alle app-iconen zijn in orde.")


if __name__ == "__main__":
    main()
