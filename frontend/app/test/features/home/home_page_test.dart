import 'package:flutter/material.dart';

import '../../helpers/fake_backend.dart';

import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/core/design/design.dart';

import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

void main() {
  testWidgets('toont welkom, aankondigingen, activiteiten en lezingen uit de backend', (tester) async {
    final backend = standardBackend()
      ..on('GET', '$base/announcements', page([announcementJson(featured: true)]))
      ..on('GET', '$base/events', page([eventJson()]))
      ..onRequest('GET', '$base/media', (request) async {
        final lectureOnly = request.url.queryParameters['category'] == 'LECTURE';
        return FakeBackendJson.of(
          page(
            lectureOnly
                ? [mediaJson()]
                : [mediaJson(), mediaJson(id: 'm2', title: 'Vrijdagpreek over familie', category: 'KHUTBAH')],
          ),
        );
      });
    await pumpApp(tester, backend: backend);

    expect(find.byType(BrandLogo), findsWidgets);
    expect(find.text('Assalamoe alaikoem'), findsOneWidget);
    expect(find.textContaining('Welkom bij Noeroel Islam'), findsOneWidget);
    expect(find.text('Nieuwe lestijden'), findsOneWidget);
    expect(find.text('Uitgelicht'), findsOneWidget);
    expect(find.text('Lezing over geduld'), findsOneWidget);
    expect(find.text('Bekijk de hele agenda'), findsOneWidget);

    await tester.scrollUntilVisible(find.text('Nieuw toegevoegd'), 300, scrollable: find.byType(Scrollable).first);
    expect(find.text('Tafsir van soera Al-Asr'), findsOneWidget);
    // Een video die al bij "Lezingen" staat, wordt bij "Nieuw toegevoegd" niet herhaald.
    expect(find.text('Vrijdagpreek over familie'), findsOneWidget);
  });

  testWidgets('verbergt lege secties en legt uit dat er geen activiteiten zijn', (tester) async {
    await pumpApp(tester);
    expect(find.text('Aankondigingen'), findsNothing);
    expect(find.text('Lezingen'), findsWidgets); // tabblad
    expect(find.textContaining('Er staan nu geen activiteiten gepland'), findsOneWidget);
  });

  testWidgets('een mislukte sectie toont een herstelmogelijkheid zonder de rest te blokkeren', (tester) async {
    final backend = standardBackend()
      ..fail('GET', '$base/events', 500, 'INTERNAL_ERROR')
      ..on('GET', '$base/announcements', page([announcementJson()]));
    await pumpApp(tester, backend: backend);
    expect(find.text('Er ging iets mis'), findsOneWidget);
    expect(find.text('Nieuwe lestijden'), findsOneWidget);

    backend.on('GET', '$base/events', page([eventJson()]));
    await tapVisible(tester, find.text('Opnieuw proberen'));
    expect(find.text('Lezing over geduld'), findsOneWidget);
  });

  testWidgets('nodigt een bezoeker uit om in te loggen en te volgen', (tester) async {
    await pumpApp(tester);
    expect(find.text('Volg Noeroel Islam'), findsOneWidget);
    expect(find.textContaining('geen lidmaatschap'), findsOneWidget);

    await tapVisible(tester, find.text('Inloggen om te volgen'));
    expect(find.text('E-mailadres'), findsOneWidget);
  });

  testWidgets('een ingelogde gebruiker kan direct volgen', (tester) async {
    final backend = standardBackend()..on('PUT', '/me/follows/$mosqueId', followJson());
    await pumpApp(tester, backend: backend, tokenStore: signedInStore(backend));
    expect(find.text('Assalamoe alaikoem, Fatima'), findsOneWidget);

    backend.on('GET', '/me/follows/$mosqueId', followJson());
    await tapVisible(tester, find.text('Volgen'));
    expect(backend.requestsTo('PUT', '/me/follows/$mosqueId'), hasLength(1));
    expect(find.text('Je volgt nu Noeroel Islam.'), findsOneWidget);
    expect(find.text('Volg Noeroel Islam'), findsNothing);
  });

  testWidgets('toont zonder verbinding de laatst opgehaalde gegevens met een melding', (tester) async {
    final backend = standardBackend()..on('GET', '$base/events', page([eventJson()]));
    await pumpApp(tester, backend: backend);
    expect(find.text('Lezing over geduld'), findsOneWidget);

    backend.offline = true;
    await tester.fling(find.byType(Scrollable).first, const Offset(0, 400), 1000);
    await settle(tester);
    expect(find.text('Je bent offline'), findsOneWidget);
    expect(find.text('Lezing over geduld'), findsOneWidget);
  });
}
