import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

Map<String, Object?> otherMosque() => {
  ...mosqueJson(),
  'id': '33333333-3333-3333-3333-333333333333',
  'slug': 'an-noor',
  'name': 'Moskee An-Noor',
  'address': {'city': 'Amsterdam'},
};

void main() {
  testWidgets('toont de aangesloten moskeeën met plaats', (tester) async {
    final backend = standardBackend()..on('GET', '$base/mosques', page([mosqueJson(), otherMosque()]));
    await pumpApp(tester, backend: backend, location: '/ontdekken');
    expect(find.text('Moskeeën ontdekken'), findsOneWidget);
    expect(find.text('Mijn Ummah'), findsOneWidget);
    expect(find.text('Moskee An-Noor'), findsOneWidget);
    expect(find.text('Amsterdam'), findsOneWidget);
  });

  testWidgets('zoekt op naam en filtert op plaats via de server', (tester) async {
    final backend = standardBackend()..on('GET', '$base/mosques/cities', ['Amsterdam', 'Voorbeeldstad']);
    await pumpApp(tester, backend: backend, location: '/ontdekken');

    await tester.enterText(find.byType(TextField), 'noor');
    await tester.testTextInput.receiveAction(TextInputAction.search);
    await settle(tester);
    expect(backend.requestsTo('GET', '$base/mosques').last.url.queryParameters['q'], 'noor');

    await tester.tap(find.text('Amsterdam'));
    await settle(tester);
    final last = backend.requestsTo('GET', '$base/mosques').last.url.queryParameters;
    expect(last['city'], 'Amsterdam');
    expect(last['q'], 'noor');
  });

  testWidgets('legt uit dat er niets is gevonden', (tester) async {
    final backend = standardBackend();
    await pumpApp(tester, backend: backend, location: '/ontdekken');
    backend.on('GET', '$base/mosques', page([]));
    await tester.enterText(find.byType(TextField), 'onbekend');
    await tester.testTextInput.receiveAction(TextInputAction.search);
    await settle(tester);
    expect(find.text('Geen moskee gevonden'), findsOneWidget);
  });

  testWidgets('een bezoeker die wil volgen, gaat naar inloggen', (tester) async {
    await pumpApp(tester, location: '/ontdekken');
    await tester.tap(find.text('Volgen'));
    await settle(tester);
    expect(find.text('E-mailadres'), findsOneWidget);
  });

  testWidgets('een ingelogde gebruiker volgt een moskee vanuit de lijst', (tester) async {
    final backend = standardBackend()..on('PUT', '/me/follows/$mosqueId', followJson());
    await pumpApp(tester, backend: backend, tokenStore: signedInStore(backend), location: '/ontdekken');

    backend.on('GET', '/me/follows', [followJson()]);
    await tester.tap(find.text('Volgen'));
    await settle(tester);
    expect(backend.requestsTo('PUT', '/me/follows/$mosqueId'), hasLength(1));
    expect(find.text('Je volgt nu Mijn Ummah.'), findsOneWidget);
    expect(find.text('Volgend'), findsOneWidget);
  });

  testWidgets('tikken op een moskee opent haar profiel', (tester) async {
    await pumpApp(tester, location: '/ontdekken');
    await tester.tap(find.text('Mijn Ummah'));
    await settle(tester);
    expect(find.text('Een moskee midden in de wijk.'), findsOneWidget);
  });
}
