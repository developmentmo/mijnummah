# Noeroel Islam-app (Flutter)

Mobiele app voor Android en iOS. Vijf tabbladen: Home, Agenda, Lezingen, Ontdekken en Profiel. De app praat met de
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
| `MOSQUE_SLUG` | `noeroel-islam` | De moskee waarvoor de app is gebouwd |

Http zonder TLS werkt alleen in Android-debugbuilds (`android/app/src/debug/AndroidManifest.xml`) en op iOS alleen
naar het lokale netwerk. Productie gebruikt https.

Inloggen kan met een account dat een platformbeheerder heeft aangemaakt (`POST /api/v1/admin/users`); zelf
registreren komt in fase 5.

## Controles

```bash
dart format --output=none --set-exit-if-changed $(find lib test -name '*.dart' ! -name '*.g.dart')
flutter analyze
flutter test
flutter build apk --debug
```

## Structuur

| Pad | Inhoud |
|---|---|
| `lib/core/api/` | API-client: foutcodes, `401` en verversen, offline-cache |
| `lib/core/auth/` | Inloggen, uitloggen, tokens in de beveiligde opslag |
| `lib/core/format/dates.dart` | Nederlandse datums en tijden in `Europe/Amsterdam` |
| `lib/core/routing/` | Tabbladen en detailroutes (`/activiteit/:id`, `/lezing/:id`, `/aankondiging/:id`, `/inloggen`) |
| `lib/core/widgets/` | Laad-, fout-, lege en offline-toestanden en kleine bouwstenen |
| `lib/core/design/` | Design tokens (gegenereerd), thema, logo en basiscomponenten uit fase 1 |
| `lib/data/` | Modellen en repositories |
| `lib/features/` | Schermen per onderdeel |
| `test/helpers/` | Nep-backend en testdata (alleen voor tests) |

Beslissingen staan in `DECISIONS.md` (D-022 t/m D-026), voortgang en beperkingen in `IMPLEMENTATION_PLAN.md`.
De designpreview uit fase 1 is in debug-builds bereikbaar via Profiel > Ontwikkeling.

App-ID's: Android `nl.mijnummah.noeroelislam`, iOS `nl.mijnummah.noeroelislam`. Zie BRAND_GUIDELINES.md in de root.
