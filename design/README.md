# Design

Bron van de huisstijl. Uitleg over gebruik staat in [BRAND_GUIDELINES.md](../BRAND_GUIDELINES.md).

| Map | Inhoud |
|---|---|
| `tokens/` | `tokens.json` (typografie, maten, status- en neutrale kleuren), de generator en controles; de merkkleuren komen uit [`brands/mijnummah/`](../brands/mijnummah/brand.json) |
| `fonts/` | Variabele bronfonts met licenties en het script dat de app- en webbestanden maakt |
| `branding/` | Generator en bronbestanden van logo's en app-iconen (`svg/`, `png/`) |

Generatoren: `node tokens/build.mjs` (alleen Node), `python3 fonts/build_fonts.py` en
`python3 branding/build_branding.py` (vereisen `pip install -r requirements.txt`). Normaal draai je ze via
`node tools/brand/brand.mjs apply`, dat tokens, namen, ID's, logo's en iconen van het Mijn Ummah-merk samen maakt.
CI controleert met `node tools/brand/brand.mjs check` dat alle gegenereerde bestanden actueel zijn.

Er is één merk: Mijn Ummah. Moskeeën hebben geen eigen app of eigen merk; hun naam, logo en omslagfoto komen uit de
backend en worden in het portaal beheerd (D-053).
