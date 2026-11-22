import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/core/design/design.dart';

import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

void main() {
  testWidgets('toont welkom, moskeeën, activiteiten, video\'s en nieuws van alle moskeeën', (tester) async {
    final backend = standardBackend()
      ..on('GET', '$base/announcements', page([announcementJson(featured: true)]))
      ..on('GET', '$base/events', page([eventJson()]))
      ..on('GET', '$base/media', page([mediaJson()]));
    await pumpApp(tester, backend: backend);

    expect(find.byType(BrandLogo), findsWidgets);
    expect(find.text('Assalamoe alaikoem'), findsOneWidget);
    expect(find.textContaining('Welkom bij Mijn Ummah'), findsOneWidget);
    expect(find.text('Moskeeën ontdekken'), findsOneWidget);
    expect(find.text('Lezing over geduld'), findsOneWidget);
    // Zonder gevolgde moskeeën komt de content van alle moskeeën: geen mosque-filter.
    expect(backend.requestsTo('GET', '$base/events').last.url.queryParametersAll['mosque'], isNull);

    await tester.scrollUntilVisible(find.text('Nieuwe lestijden'), 300, scrollable: find.byType(Scrollable).first);
    expect(find.text('Tafsir van soera Al-Asr'), findsOneWidget);
    expect(find.text('Uitgelicht'), findsOneWidget);
  });

  testWidgets('wie moskeeën volgt, ziet hun content en de eigen moskeeën bovenaan', (tester) async {
    final backend = standardBackend()
      ..on('GET', '/me/follows', [followJson(favorite: true)])
      ..on('GET', '$base/events', page([eventJson()]));
    await pumpApp(tester, backend: backend, tokenStore: signedInStore(backend));

    expect(find.text('Assalamoe alaikoem, Fatima'), findsOneWidget);
    expect(find.text('Jouw moskeeën'), findsOneWidget);
    expect(find.textContaining('moskeeën die je volgt'), findsOneWidget);
    final events = backend.requestsTo('GET', '$base/events');
    expect(events, hasLength(1));
    expect(events.single.url.queryParametersAll['mosque'], [slug]);

    await tapVisible(tester, find.bySemanticsLabel('Mijn Ummah, favoriet'));
    expect(find.text('Een moskee midden in de wijk.'), findsOneWidget);
  });

  testWidgets('verbergt lege secties en legt uit dat er geen activiteiten zijn', (tester) async {
    await pumpApp(tester);
    expect(find.text('Nieuws'), findsNothing);
    expect(find.text('Nieuwe video\'s'), findsNothing);
    expect(find.textContaining('Er staan nu geen activiteiten gepland'), findsOneWidget);
  });

  testWidgets('een mislukte sectie toont een herstelmogelijkheid zonder de rest te blokkeren', (tester) async {
    final backend = standardBackend()
      ..fail('GET', '$base/events', 500, 'INTERNAL_ERROR')
      ..on('GET', '$base/announcements', page([announcementJson()]));
    await pumpApp(tester, backend: backend);
    expect(find.text('Er ging iets mis'), findsOneWidget);
    await tester.scrollUntilVisible(find.text('Nieuwe lestijden'), 300, scrollable: find.byType(Scrollable).first);

    backend.on('GET', '$base/events', page([eventJson()]));
    await tapVisible(tester, find.text('Opnieuw proberen'));
    expect(find.text('Lezing over geduld'), findsOneWidget);
  });

  testWidgets('nodigt een bezoeker uit om in te loggen en moskeeën te volgen', (tester) async {
    await pumpApp(tester);
    expect(find.text('Volg je moskeeën'), findsOneWidget);
    expect(find.textContaining('geen lidmaatschap'), findsOneWidget);

    await tapVisible(tester, find.text('Inloggen of account maken'));
    expect(find.text('E-mailadres'), findsOneWidget);
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

  testWidgets('bij de eerste start verschijnt de introductie, die je kunt overslaan', (tester) async {
    await pumpApp(tester, preferences: {'onboarding.done': false});
    expect(find.text('Moskeeën bij jou in de buurt'), findsOneWidget);

    await tester.tap(find.text('Overslaan'));
    await settle(tester);
    expect(find.text('Assalamoe alaikoem'), findsOneWidget);
  });

  testWidgets('de introductie eindigt met aan de slag', (tester) async {
    await pumpApp(tester, preferences: {'onboarding.done': false});
    await tester.tap(find.text('Volgende'));
    await settle(tester);
    await tester.tap(find.text('Volgende'));
    await settle(tester);
    expect(find.textContaining('geen lidmaatschap'), findsOneWidget);
    await tester.tap(find.text('Aan de slag met Mijn Ummah'));
    await settle(tester);
    expect(find.text('Assalamoe alaikoem'), findsOneWidget);
  });
}
