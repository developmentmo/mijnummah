/// Instellingen die per build worden meegegeven met `--dart-define`.
///
/// ```bash
/// flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8082 --dart-define=MOSQUE_SLUG=noeroel-islam
/// ```
///
/// De standaard-URL wijst vanuit de Android-emulator naar de backend op de eigen computer (profiel `local`).
class AppConfig {
  const AppConfig({required this.apiBaseUrl, required this.mosqueSlug});

  factory AppConfig.fromEnvironment() => const AppConfig(
    apiBaseUrl: String.fromEnvironment('API_BASE_URL', defaultValue: 'http://10.0.2.2:8082'),
    mosqueSlug: String.fromEnvironment('MOSQUE_SLUG', defaultValue: 'noeroel-islam'),
  );

  /// Basis-URL van de backend, zonder `/api/v1`.
  final String apiBaseUrl;

  /// De moskee waarvoor deze app is gebouwd.
  final String mosqueSlug;

  /// De tijdzone waarin alle tijden worden getoond.
  static const timeZone = 'Europe/Amsterdam';
}
