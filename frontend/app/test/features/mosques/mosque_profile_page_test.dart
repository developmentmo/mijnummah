import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

void main() {
  const profile = '/moskee/$slug';

  testWidgets('toont het profiel met adres, contact en de content van deze moskee', (tester) async {
    final backend = standardBackend()
      ..on('GET', '$base/events', page([eventJson()]))
      ..on('GET', '$base/media', page([mediaJson()]))
      ..on('GET', '$base/announcements', page([announcementJson()]));
    await pumpApp(tester, backend: backend, location: profile);

    expect(find.text('Mijn Ummah'), findsWidgets);
    expect(find.text('Een moskee midden in de wijk.'), findsOneWidget);
    expect(find.text('Lezing over geduld'), findsOneWidget);
    for (final path in ['$base/events', '$base/media', '$base/announcements']) {
      expect(backend.requestsTo('GET', path).last.url.queryParametersAll['mosque'], [slug], reason: path);
    }
    await tester.ensureVisible(find.text('Voorbeeldstraat 1, 1234 AB Voorbeeldstad'));
    expect(find.text('010 123 4567'), findsOneWidget);
    expect(find.text('info@example.org'), findsOneWidget);
    expect(find.text('example.org'), findsOneWidget);
  });

  testWidgets('verbergt contactgegevens die de moskee niet heeft ingevuld', (tester) async {
    final backend = standardBackend()
      ..on('GET', mosqueBase, {
        'id': mosqueId,
        'slug': slug,
        'name': 'Mijn Ummah',
        'address': <String, Object?>{},
        'contact': <String, Object?>{},
      });
    await pumpApp(tester, backend: backend, location: profile);
    expect(find.text('Adres en contact'), findsNothing);
    expect(find.text('Gebedstijden'), findsNothing);
  });

  testWidgets('toont een link naar de gebedstijden als de moskee die heeft', (tester) async {
    final backend = standardBackend()
      ..on('GET', mosqueBase, {...mosqueJson(), 'prayerTimesUrl': 'https://example.org/gebedstijden'});
    await pumpApp(tester, backend: backend, location: profile);
    expect(find.text('Gebedstijden'), findsOneWidget);
  });

  testWidgets('"Hele agenda" opent de agenda van deze moskee', (tester) async {
    final backend = standardBackend();
    await pumpApp(tester, backend: backend, location: profile);
    await tapVisible(tester, find.text('Hele agenda'));
    expect(find.text('Agenda'), findsWidgets);
    expect(backend.requestsTo('GET', '$base/events').last.url.queryParametersAll['mosque'], [slug]);
  });

  testWidgets('een volger maakt de moskee favoriet', (tester) async {
    final backend = standardBackend()
      ..on('GET', '/me/follows', [followJson()])
      ..on('PUT', '/me/follows/$mosqueId/favorite', followJson(favorite: true));
    await pumpApp(tester, backend: backend, tokenStore: signedInStore(backend), location: profile);
    expect(find.text('Volgend'), findsOneWidget);

    backend.on('GET', '/me/follows', [followJson(favorite: true)]);
    await tester.tap(find.byTooltip('Favoriete moskee'));
    await settle(tester);
    expect(backend.requestsTo('PUT', '/me/follows/$mosqueId/favorite'), hasLength(1));
    expect(find.byTooltip('Niet meer als favoriet'), findsOneWidget);
  });

  testWidgets('een onbekende moskee geeft een foutmelding', (tester) async {
    final backend = standardBackend()..fail('GET', '$base/mosques/bestaat-niet', 404, 'MOSQUE_NOT_FOUND');
    await pumpApp(tester, backend: backend, location: '/moskee/bestaat-niet');
    expect(find.text('Opnieuw proberen'), findsOneWidget);
  });
}
