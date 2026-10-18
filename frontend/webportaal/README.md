# Beheerportaal (Angular)

Beheeromgeving voor bestuurders en contentbeheerders: activiteiten, aanmeldingen, lezingen en video's,
aankondigingen, moskeegegevens, beheerders en (voor platformbeheerders) moskeeën toevoegen en accounts. Eén portaal
voor alle moskeeën; naam en kleuren komen van het standaardmerk in `brands/` (D-050, [WHITE_LABEL.md](../../WHITE_LABEL.md)). De designpreview uit fase 1 staat
in ontwikkelmodus op `/design`.

## Vereisten

- Node.js 24.15 of nieuwer (zie `.nvmrc`). Oudere versies, zoals 24.13, weigert de Angular CLI met de melding
  "The Angular CLI requires a minimum Node.js version". Controleer met `node -v`; bijwerken met
  `brew upgrade node@24`, `nvm install 24` of via https://nodejs.org.

## Commando's

```bash
npm ci
npm start                # ontwikkelserver op http://localhost:4200 (backend volgens public/config.json)
npm run lint
npm test                 # Vitest, eenmalig
npm run format:check
npm run build
```

## Configuratie

`public/config.json` bevat het adres van de backend en wordt bij het opstarten gelezen (D-027):

```json
{ "apiBaseUrl": "http://localhost:8082" }
```

Vervang dit bestand per omgeving; er horen geen geheimen in. De backend moet de herkomst van het portaal toestaan
(`CORS_ALLOWED_ORIGINS`). Lokaal draaien met testcontent: start de backend met profiel `local` (zie
`backend/README.md`) en daarna `npm start`.

## Productie

`Dockerfile` bouwt de app en serveert hem met `nginx-unprivileged` op poort 8080:

```sh
docker build -t mijnummah-webportaal .
docker run -e API_BASE_URL=https://api.example.nl -p 8081:8080 mijnummah-webportaal
```

nginx levert `config.json` uit `API_BASE_URL`, zet beveiligingsheaders en een Content-Security-Policy met een nonce
per verzoek (de placeholder `CSP_NONCE_PLACEHOLDER` in `src/index.html`), en stuurt onbekende paden naar de app.
Configuratie in `docker/`; de volledige productieopzet staat in [DEPLOYMENT.md](../../DEPLOYMENT.md).

## Structuur

| Pad | Inhoud |
|---|---|
| `src/styles/tokens.generated.css` | Gegenereerde CSS-variabelen (`--mu-*`); bron is `design/tokens/tokens.json` met het merk |
| `src/app/core/brand/brand.generated.ts` | Naam en slug van het merk (gegenereerd) |
| `src/styles/_material-theme.scss` | Angular Material M3, gekoppeld aan de tokens |
| `src/styles/_base.scss` | Basisstijlen en typografieklassen (`.mu-title-large` enz.) |
| `src/styles/fonts.css` | Meegeleverde fonts (`public/fonts/`) |
| `src/app/core/theme/` | Licht/donker/systeem (`data-theme` op `<html>`) |
| `src/app/shared/ui/` | Logo, melding, skeleton, lege toestand |
| `src/app/core/api/` | `AdminApi`, modellen, foutafhandeling met Nederlandse teksten |
| `src/app/core/auth/` | Inloggen, tokens (D-028), interceptor, guards |
| `src/app/core/mosque/` | Gekozen moskee en rol, guards voor `/m/:mosqueId` (D-031) |
| `src/app/core/format/` | Labels, datums en duur in de tijdzone van de moskee |
| `src/app/shared/` | Bevestigingsdialoog, meldingen, formulierhulpen, niet-opgeslagen-guard |
| `src/app/features/` | De schermen, één map per onderdeel; `notifications/` is het paneel Pushmelding (alleen beheerders, D-033) |
| `src/app/testing/` | Testhulpen (niet in de build) |
| `src/app/design-preview/` | Interne designpreview |

Gebruik in componenten alleen `var(--mu-…)`; `node design/tokens/check-hardcoded.mjs` controleert dat.
