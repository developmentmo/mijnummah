# App Store: vermelding Mijn Ummah

Teksten om in App Store Connect over te nemen (taal Nederlands). Lengtes zijn gecontroleerd op de limieten van Apple.
Punten tussen [haken] vul je in zodra ze bekend zijn.

| Veld | Waarde |
|---|---|
| Naam (max. 30) | Mijn Ummah |
| Ondertitel (max. 30) | Je moskee: agenda en lezingen |
| Bundle-ID | `nl.mijnummah.app` |
| Primaire categorie | Lifestyle |
| Secundaire categorie | Sociaal netwerken (of Nieuws) |
| Leeftijdsclassificatie | 4+ |
| Support-URL | [https://\<domein\>] |
| Privacybeleid-URL | https://beheer.\<domein\>/privacy |
| Prijs | Gratis |

## Promotietekst (max. 170)

Volg je moskeeën en mis geen les, lezing of aankondiging meer. Kies zelf waarover je een melding krijgt.

## Beschrijving (max. 4000)

Gelijk aan de volledige beschrijving in [google-play.md](google-play.md#volledige-beschrijving-max-4000); vervang
de opsommingstekens eventueel door streepjes.

## Trefwoorden (max. 100, komma's zonder spaties)

moskee,islam,agenda,lezing,gebed,ramadan,iftar,les,gemeenschap,ummah,nieuws,meldingen

## App-privacy ("Privacylabels")

Gebruikt voor tracking: nee. Gekoppeld aan de identiteit van de gebruiker (alleen bij een account):

| Gegevenstype | Doel |
|---|---|
| Contactgegevens: naam, e-mailadres | App-functionaliteit |
| Identificatiegegevens: gebruikers-ID | App-functionaliteit |
| Gebruiksgegevens: productinteractie (gevolgde moskeeën, aanmeldingen, bewaarde video's) | App-functionaliteit |
| Identificatiegegevens: apparaat-ID (pushtoken) | App-functionaliteit |

## Gegevens voor de beoordeling (App Review)

- Inloggen is optioneel; de reviewer kan alles bekijken zonder account. Geef toch een testaccount op, zodat volgen,
  meldingen en aanmelden te zien zijn: [e-mailadres testaccount] / [wachtwoord testaccount]. Maak dat account aan
  in de productieomgeving en laat het een testmoskee volgen.
- Account verwijderen (verplicht bij Apple): Profiel, Account verwijderen.
- Notitie: "De content komt van aangesloten moskeeën via een beheerportaal. Gebruikers kunnen geen content plaatsen
  of elkaar berichten sturen."

## Afbeeldingen

| Wat | Formaat | Bron |
|---|---|---|
| App-pictogram | 1024 × 1024 zonder transparantie | Zit in de build (`frontend/app/ios/Runner/Assets.xcassets`) |
| iPhone-screenshots 6,9" | 1320 × 2868 (of 1290 × 2796), 3 tot 10 | Zie `store/README.md` |
