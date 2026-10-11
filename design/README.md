# Design

Bron van de huisstijl. Uitleg over gebruik staat in [BRAND_GUIDELINES.md](../BRAND_GUIDELINES.md).

| Map | Inhoud |
|---|---|
| `tokens/` | `tokens.json` (enige bron van kleuren, typografie, maten), de generator en controles |
| `fonts/` | Variabele bronfonts met licenties en het script dat de app- en webbestanden maakt |
| `branding/` | Generator en bronbestanden van logo's en app-iconen (`svg/`, `png/`) |

Generatoren: `node tokens/build.mjs` (alleen Node), `python3 fonts/build_fonts.py` en
`python3 branding/build_branding.py` (vereisen `pip install -r requirements.txt`).
