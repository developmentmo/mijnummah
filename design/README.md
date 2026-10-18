# Design

Bron van de huisstijl. Uitleg over gebruik staat in [BRAND_GUIDELINES.md](../BRAND_GUIDELINES.md).

| Map | Inhoud |
|---|---|
| `tokens/` | `tokens.json` (typografie, maten, status- en neutrale kleuren), de generator en controles; de merkkleuren komen uit [`brands/`](../brands) |
| `fonts/` | Variabele bronfonts met licenties en het script dat de app- en webbestanden maakt |
| `branding/` | Generator en bronbestanden van logo's en app-iconen (`svg/`, `png/`) |

Generatoren: `node tokens/build.mjs [--brand=<slug>]` (alleen Node), `python3 fonts/build_fonts.py` en
`python3 branding/build_branding.py [--brand=<slug>]` (vereisen `pip install -r requirements.txt`). Normaal draai
je ze via `node tools/brand/brand.mjs apply`, dat tokens, logo's en iconen voor één merk samen maakt
(zie [WHITE_LABEL.md](../WHITE_LABEL.md)). Zonder `--brand` gebruiken ze het standaardmerk uit `brands/index.json`.
