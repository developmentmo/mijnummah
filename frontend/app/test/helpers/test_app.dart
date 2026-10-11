import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/app.dart';
import 'package:mijnummah_app/core/auth/token_store.dart';
import 'package:mijnummah_app/core/config/app_config.dart';
import 'package:mijnummah_app/core/providers.dart';
import 'package:mijnummah_app/features/lectures/video_player.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'fake_backend.dart';
import 'fixtures.dart';

/// Bewaart de sessie in het geheugen in plaats van in de beveiligde opslag van het toestel.
class InMemoryTokenStore implements TokenStore {
  AuthSession? session;
  Map<String, dynamic>? profile;

  @override
  Future<AuthSession?> readSession() async => session;

  @override
  Future<Map<String, dynamic>?> readProfile() async => profile;

  @override
  Future<void> save(AuthSession session, {Map<String, dynamic>? profile}) async {
    this.session = session;
    if (profile != null) this.profile = profile;
  }

  @override
  Future<void> clear() async {
    session = null;
    profile = null;
  }
}

/// Een backend met een moskee en lege lijsten; tests vullen aan wat ze nodig hebben.
FakeBackend standardBackend() => FakeBackend()
  ..on('GET', base, mosqueJson())
  ..on('GET', '$base/events', page([]))
  ..on('GET', '$base/media', page([]))
  ..on('GET', '$base/announcements', page([]))
  ..fail('GET', '/me/follows/$mosqueId', 404, 'NOT_FOLLOWING')
  ..on('GET', '/me/event-registrations', page([]));

/// Zet een ingelogde sessie klaar.
InMemoryTokenStore signedInStore(FakeBackend backend) {
  backend.on('GET', '/auth/me', userJson());
  return InMemoryTokenStore()
    ..session = AuthSession(
      accessToken: 'access-1',
      accessTokenExpiresAt: DateTime.utc(2099),
      refreshToken: 'refresh-1',
    )
    ..profile = userJson();
}

/// Plaatsvervanger voor de YouTube-speler (een WebView werkt niet in widgettests).
class FakeVideoPlayer extends StatelessWidget {
  const FakeVideoPlayer({super.key, required this.videoId, required this.onUnavailable});

  final String videoId;
  final VoidCallback onUnavailable;

  @override
  Widget build(BuildContext context) => AspectRatio(
    aspectRatio: 16 / 9,
    child: TextButton(onPressed: onUnavailable, child: Text('speler $videoId')),
  );
}

Future<void> pumpApp(
  WidgetTester tester, {
  FakeBackend? backend,
  TokenStore? tokenStore,
  String location = '/',
  Size size = const Size(390, 844),
  double textScale = 1,
  Map<String, Object> preferences = const {},
}) async {
  tester.view.physicalSize = size;
  tester.view.devicePixelRatio = 1;
  tester.platformDispatcher.textScaleFactorTestValue = textScale;
  addTearDown(tester.view.reset);
  addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);

  SharedPreferences.setMockInitialValues(preferences);
  final prefs = await SharedPreferences.getInstance();
  final fake = backend ?? standardBackend();

  await tester.pumpWidget(
    ProviderScope(
      retry: (_, _) => null,
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        appConfigProvider.overrideWithValue(const AppConfig(apiBaseUrl: FakeBackend.baseUrl, mosqueSlug: slug)),
        httpClientProvider.overrideWithValue(fake.client),
        tokenStoreProvider.overrideWithValue(tokenStore ?? InMemoryTokenStore()),
        videoPlayerBuilderProvider.overrideWithValue(
          (videoId, onUnavailable) => FakeVideoPlayer(videoId: videoId, onUnavailable: onUnavailable),
        ),
      ],
      child: NoeroelIslamApp(initialLocation: location),
    ),
  );
  await settle(tester);
}

/// Laat verzoeken, animaties en timers afronden. `pumpAndSettle` werkt niet met de pulserende skeletons.
Future<void> settle(WidgetTester tester) async {
  for (var i = 0; i < 20; i++) {
    await tester.pump(const Duration(milliseconds: 100));
  }
}

/// Scrollt een lijst helemaal door, zodat ook onderaan gebouwde widgets op overflow worden gecontroleerd.
Future<void> scrollThrough(WidgetTester tester, {Finder? scrollable}) async {
  final list = scrollable ?? find.byType(Scrollable).last;
  for (var i = 0; i < 15; i++) {
    await tester.drag(list, const Offset(0, -500), warnIfMissed: false);
    await tester.pump(const Duration(milliseconds: 50));
  }
}

/// Scrollt een widget in beeld en tikt erop.
Future<void> tapVisible(WidgetTester tester, Finder finder) async {
  await tester.ensureVisible(finder);
  await tester.pump();
  await tester.tap(finder);
  await settle(tester);
}
