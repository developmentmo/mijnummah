# Moskee-app (Flutter)

Mobiele app voor Android en iOS, per moskee gebouwd met een eigen naam, ID's, kleuren en iconen (merk uit
`brands/`, zie [WHITE_LABEL.md](../../WHITE_LABEL.md)); in de repository staat het merk van Noeroel Islam. Vijf tabbladen: Home, Agenda, Lezingen, Ontdekken en Profiel. De app praat met de
backend in `backend/`; zonder verbinding toont hij de laatst opgehaalde publieke gegevens.

## Vereisten

- Flutter 3.47 (stable), Dart 3.13
- Voor Android-builds: Android SDK en Java 17. Voor iOS-builds: macOS met Xcode.
- Een draaiende backend (zie `backend/README.md`, profiel `local` op poort 8082).

## Starten tegen de lokale backend

```bash
flutter pub get
# Android-emulator (10.0.2.2 is de computer zelf; dit is ook de standaard):
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8082
# iOS-simulator of desktop:
flutter run --dart-define=API_BASE_URL=http://localhost:8082
```

| Instelling | Standaard | |
|---|---|---|
| `API_BASE_URL` | `http://10.0.2.2:8082` | Basis-URL van de backend, zonder `/api/v1` |
| `MOSQUE_SLUG` | slug van het toegepaste merk | Alleen om tijdelijk een andere moskee te tonen; normaal pas je het merk toe |

Http zonder TLS werkt alleen in Android-debugbuilds (`android/app/src/debug/AndroidManifest.xml`) en op iOS alleen
naar het lokale netwerk. Productie gebruikt https.

Gebruikers maken zelf een account aan (Profiel > Account aanmaken), herstellen hun wachtwoord met een code per e-mail
en kunnen hun account zelf verwijderen (Profiel > Accountinstellingen). Tegen de backend met profiel `local` staat de
herstelcode in het log van de backend; er wordt dan geen e-mail verstuurd.

## Pushmeldingen

Pushmeldingen gaan via Firebase Cloud Messaging (op iOS via APNs). Het platform heeft één Firebase-project met per
moskee een Android- en iOS-app (D-049); de instellingen staan niet in de repository maar worden per build meegegeven
(voor releases door de workflow Release-app, zie WHITE_LABEL.md):

```bash
flutter build apk \
  --dart-define=API_BASE_URL=https://api.example.nl \
  --dart-define=FIREBASE_PROJECT_ID=... \
  --dart-define=FIREBASE_MESSAGING_SENDER_ID=... \
  --dart-define=FIREBASE_ANDROID_API_KEY=... \
  --dart-define=FIREBASE_ANDROID_APP_ID=...
# iOS: FIREBASE_IOS_API_KEY en FIREBASE_IOS_APP_ID in plaats van de Android-waarden
```

| Instelling | Waar te vinden |
|---|---|
| `FIREBASE_PROJECT_ID`, `FIREBASE_MESSAGING_SENDER_ID` | Firebase-console > Projectinstellingen > Algemeen (projectnummer is de sender-ID) |
| `FIREBASE_ANDROID_API_KEY`, `FIREBASE_ANDROID_APP_ID` | Dezelfde pagina, bij de Android-app met het `android.applicationId` van het merk |
| `FIREBASE_IOS_API_KEY`, `FIREBASE_IOS_APP_ID` | Dezelfde pagina, bij de iOS-app met het `ios.bundleId` van het merk |

Zonder deze instellingen (en op het web) werkt de app gewoon, maar zijn pushmeldingen uit; bij de meldingsvoorkeuren
staat dan dat pushmeldingen in deze versie niet zijn ingesteld. De backend heeft daarnaast `PUSH_PROVIDER=fcm` met een
serviceaccount nodig (zie `backend/README.md`).

Voor iOS is verder nodig, en nog niet gedaan: in Xcode de capability Push Notifications aanzetten bij target Runner, en
in Firebase een APNs-sleutel uit het Apple Developer-account uploaden.

De app vraagt pas om toestemming als de gebruiker de moskee gaat volgen of een onderwerp aanzet. Het toestel wordt na
het inloggen bij de backend aangemeld (voor de moskee van deze app) en vóór het uitloggen afgemeld. Tikken op een melding opent de aankondiging,
activiteit of lezing.

## Controles

```bash
dart format --output=none --set-exit-if-changed $(find lib test -name '*.dart' ! -name '*.g.dart')
flutter analyze
flutter test
flutter build apk --debug
```

Een andere moskee-app bouwen: `node tools/brand/brand.mjs apply <slug>` in de root, daarna dezelfde commando's; zet
het standaardmerk terug met `node tools/brand/brand.mjs apply` vóór het committen.

## Structuur

| Pad | Inhoud |
|---|---|
| `lib/core/brand/brand.g.dart` | Slug, naam en privacylink van het merk (gegenereerd) |
| `lib/core/api/` | API-client: foutcodes, `401` en verversen, offline-cache |
| `lib/core/auth/` | Inloggen, registreren, wachtwoordherstel, account, tokens in de beveiligde opslag |
| `lib/core/push/` | Pushmeldingen: Firebase-koppeling, aan- en afmelden van het toestel |
| `lib/core/format/dates.dart` | Nederlandse datums en tijden in `Europe/Amsterdam` |
| `lib/core/routing/` | Tabbladen en detailroutes (`/activiteit/:id`, `/lezing/:id`, `/aankondiging/:id`, `/inloggen`, `/account-aanmaken`, `/wachtwoord-vergeten`, `/account`) |
| `lib/core/widgets/` | Laad-, fout-, lege en offline-toestanden en kleine bouwstenen |
| `lib/core/design/` | Design tokens (gegenereerd), thema, logo en basiscomponenten uit fase 1 |
| `lib/data/` | Modellen en repositories |
| `lib/features/` | Schermen per onderdeel |
| `test/helpers/` | Nep-backend, nep-pushmeldingen en testdata (alleen voor tests) |

Beslissingen staan in `DECISIONS.md` (D-022 t/m D-026, D-039, D-046 t/m D-051), voortgang en beperkingen in `IMPLEMENTATION_PLAN.md`.
De designpreview uit fase 1 is in debug-builds bereikbaar via Profiel > Ontwikkeling.

App-ID's: Android `nl.mijnummah.noeroelislam`, iOS `nl.mijnummah.noeroelislam`. Zie BRAND_GUIDELINES.md in de root.
