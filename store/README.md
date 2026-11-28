# Storevermelding

Alles wat nodig is om de app Mijn Ummah in Google Play en de App Store te zetten. De builds komen uit de workflow
Release-app (zie [DEPLOYMENT.md](../DEPLOYMENT.md#release-van-de-app)); uploaden doet iemand met rechten in de
store-accounts.

- [google-play.md](google-play.md): teksten, Data safety, inhoudsclassificatie.
- [app-store.md](app-store.md): teksten, privacylabels, gegevens voor App Review.
- Privacyverklaring: openbare pagina `/privacy` in het beheerportaal (`frontend/webportaal/src/app/features/legal/`).
  Na vaststelling: `PRIVACY_STATEMENT_IS_CONCEPT` op `false` en het adres in `brands/mijnummah/brand.json`
  (`links.privacyPolicyUrl`), dan `node tools/brand/brand.mjs apply` zodat de app de link bij Profiel toont.

## Screenshots

Maak ze op een emulator of toestel met de release-build tegen de productie-API, met echte content van een
aangesloten moskee (of de testmoskeeën uit `tools/demo/` op een testomgeving). Geen persoonsgegevens van echte
gebruikers in beeld.

1. Home met gevolgde moskeeën en de eerstvolgende activiteiten.
2. Agenda met het filter per moskee.
3. Een activiteit met de knop Aanmelden.
4. Lezingen met een video.
5. Profiel van een moskee (logo, omslag, adres).
6. Meldingen kiezen per onderwerp.

Formaten: Google Play 1080 × 1920 of groter (9:16), App Store 6,9" 1320 × 2868. Licht thema; eventueel één
screenshot in donker thema.

## Release-opmerkingen 1.0.0

Google Play (max. 500 tekens) en App Store ("Wat is er nieuw"):

> De eerste versie van Mijn Ummah. Volg je moskeeën, bekijk de agenda en lezingen, lees het nieuws en kies zelf
> waarover je een melding krijgt.
