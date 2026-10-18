import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/core/config/app_config.dart';

void main() {
  test('kiest het adres van de eigen computer per platform', () {
    expect(AppConfig.localApiBaseUrl(TargetPlatform.android), 'http://10.0.2.2:8082');
    expect(AppConfig.localApiBaseUrl(TargetPlatform.iOS), 'http://localhost:8082');
    expect(AppConfig.localApiBaseUrl(TargetPlatform.macOS), 'http://localhost:8082');
  });

  test('zonder API_BASE_URL volgt de app het platform', () {
    debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
    addTearDown(() => debugDefaultTargetPlatformOverride = null);
    expect(AppConfig.fromEnvironment().apiBaseUrl, 'http://localhost:8082');
  });
}
