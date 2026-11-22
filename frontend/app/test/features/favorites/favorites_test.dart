import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_backend.dart';
import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

void main() {
  testWidgets('een video bewaren bij de lezing en terugvinden bij Favorieten', (tester) async {
    late final FakeBackend backend;
    backend = standardBackend()
      ..on('GET', '$base/media/m1', mediaJson())
      ..on('GET', '$base/media/m1/related', [])
      ..onRequest('PUT', '/me/favorite-media/m1', (_) async {
        backend
          ..on('GET', '/me/favorite-media/ids', ['m1'])
          ..on('GET', '/me/favorite-media', page([mediaJson()]));
        return FakeBackendJson.of(null, status: 204);
      });
    await pumpApp(tester, backend: backend, tokenStore: signedInStore(backend), location: '/lezing/m1');

    await tester.tap(find.byTooltip('Bewaren bij favorieten'));
    await settle(tester);
    expect(backend.requestsTo('PUT', '/me/favorite-media/m1'), hasLength(1));
    expect(find.byTooltip('Verwijderen uit favorieten'), findsOneWidget);
    expect(find.text('Bewaard bij je favorieten.'), findsOneWidget);
  });

  testWidgets('Favorieten toont favoriete moskeeën en bewaarde video\'s', (tester) async {
    final backend = standardBackend()
      ..on('GET', '/me/follows', [followJson(favorite: true)])
      ..on('GET', '/me/favorite-media', page([mediaJson()]));
    await pumpApp(tester, backend: backend, tokenStore: signedInStore(backend), location: '/favorieten');
    expect(find.text('Mijn Ummah'), findsWidgets);
    expect(find.text('Tafsir van soera Al-Asr'), findsOneWidget);
  });

  testWidgets('een bezoeker die een video wil bewaren, gaat naar inloggen', (tester) async {
    final backend = standardBackend()
      ..on('GET', '$base/media/m1', mediaJson())
      ..on('GET', '$base/media/m1/related', []);
    await pumpApp(tester, backend: backend, location: '/lezing/m1');
    await tester.tap(find.byTooltip('Bewaren bij favorieten'));
    await settle(tester);
    expect(find.text('E-mailadres'), findsOneWidget);
    expect(backend.requestsTo('GET', '/me/favorite-media/ids'), isEmpty);
  });
}
