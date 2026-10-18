import 'package:flutter/foundation.dart';

import '../brand/brand.g.dart';

/// Instellingen die per build worden meegegeven met `--dart-define`.
///
/// ```bash
/// flutter run --dart-define=API_BASE_URL=https://api.voorbeeld.nl
/// ```
///
/// Zonder `API_BASE_URL` wijst de app naar de backend op de eigen computer (profiel `local`): vanuit de
/// Android-emulator is dat `10.0.2.2`, vanuit de iOS-simulator en desktop `localhost` (zie [localApiBaseUrl]). De
/// moskee is standaard die van het toegepaste merk ([Brand.slug]); `MOSQUE_SLUG` is alleen nodig om tijdelijk een
/// andere moskee uit dezelfde backend te tonen.
class AppConfig {
  const AppConfig({required this.apiBaseUrl, required this.mosqueSlug});

  factory AppConfig.fromEnvironment() => AppConfig(
    apiBaseUrl: _apiBaseUrl.isNotEmpty ? _apiBaseUrl : localApiBaseUrl(defaultTargetPlatform),
    mosqueSlug: const String.fromEnvironment('MOSQUE_SLUG', defaultValue: Brand.slug),
  );

  static const _apiBaseUrl = String.fromEnvironment('API_BASE_URL');

  /// De backend op de eigen computer, gezien vanaf [platform]. De Android-emulator bereikt de computer via
  /// `10.0.2.2`; de iOS-simulator en desktop delen het netwerk van de computer.
  static String localApiBaseUrl(TargetPlatform platform) =>
      platform == TargetPlatform.android ? 'http://10.0.2.2:8082' : 'http://localhost:8082';

  /// Basis-URL van de backend, zonder `/api/v1`.
  final String apiBaseUrl;

  /// De moskee waarvoor deze app is gebouwd.
  final String mosqueSlug;

  /// De tijdzone waarin alle tijden worden getoond.
  static const timeZone = 'Europe/Amsterdam';
}
