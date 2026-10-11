# Noeroel Islam-beheerportaal (Angular)

Beheeromgeving voor bestuurders en contentbeheerders. Fase 1 bevat het design system en een interne
designpreview op `/`; de beheerschermen volgen in fase 4.

## Vereisten

- Node.js 24 (zie `.nvmrc`); Angular 22 vereist minimaal Node 22.22.3.

## Commando's

```bash
npm ci
npm start                # ontwikkelserver op http://localhost:4200
npm run lint
npm test                 # Vitest, eenmalig
npm run format:check
npm run build
```

## Structuur

| Pad | Inhoud |
|---|---|
| `src/styles/tokens.generated.css` | Gegenereerde CSS-variabelen (`--mu-*`); bron is `design/tokens/tokens.json` |
| `src/styles/_material-theme.scss` | Angular Material M3, gekoppeld aan de tokens |
| `src/styles/_base.scss` | Basisstijlen en typografieklassen (`.mu-title-large` enz.) |
| `src/styles/fonts.css` | Meegeleverde fonts (`public/fonts/`) |
| `src/app/core/theme/` | Licht/donker/systeem (`data-theme` op `<html>`) |
| `src/app/shared/ui/` | Logo, melding, skeleton, lege toestand |
| `src/app/design-preview/` | Interne designpreview |

Gebruik in componenten alleen `var(--mu-…)`; `node design/tokens/check-hardcoded.mjs` controleert dat.
