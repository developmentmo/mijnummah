# Noeroel Islam-app (Flutter)

Mobiele app voor Android en iOS. Fase 1 bevat het design system en een interne designpreview; de schermen
volgen in fase 2.

## Vereisten

- Flutter 3.47 (stable), Dart 3.13
- Voor Android-builds: Android SDK en Java 17. Voor iOS-builds: macOS met Xcode.

## Commando's

```bash
flutter pub get
flutter run                 # start de app (opent nu de designpreview)
flutter analyze
flutter test
dart format $(find lib test -name '*.dart' ! -name '*.g.dart')
flutter build apk --debug
```

## Structuur

| Pad | Inhoud |
|---|---|
| `lib/core/design/tokens.g.dart` | Gegenereerde design tokens (niet aanpassen; bron is `design/tokens/tokens.json`) |
| `lib/core/design/app_theme.dart` | Material 3-thema op basis van de tokens, `context.colors` |
| `lib/core/design/brand_logo.dart` | Logo in licht/donker |
| `lib/core/design/components/` | Melding, skeleton, lege/fouttoestand |
| `lib/features/design_preview/` | Interne designpreview |
| `assets/branding/`, `assets/fonts/` | Gegenereerd door de scripts in `design/` |

App-ID's: Android `nl.mijnummah.noeroelislam`, iOS `nl.mijnummah.noeroelislam`. Zie BRAND_GUIDELINES.md in de root.
