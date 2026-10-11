# Noeroel Islam-beheerportaal (Angular)

Beheeromgeving voor bestuurders en contentbeheerders: activiteiten, aanmeldingen, lezingen en video's,
aankondigingen, moskeegegevens, beheerders en (voor platformbeheerders) accounts. De designpreview uit fase 1 staat
in ontwikkelmodus op `/design`.

## Vereisten

- Node.js 24 (zie `.nvmrc`); Angular 22 vereist minimaal Node 22.22.3.

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

## Structuur

| Pad | Inhoud |
|---|---|
| `src/styles/tokens.generated.css` | Gegenereerde CSS-variabelen (`--mu-*`); bron is `design/tokens/tokens.json` |
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
