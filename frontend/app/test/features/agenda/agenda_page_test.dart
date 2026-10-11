import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_backend.dart';
import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

void main() {
  testWidgets('toont aankomende activiteiten met Nederlandse datum en tijd', (tester) async {
    final backend = standardBackend()
      ..on('GET', '$base/events', page([eventJson(), eventJson(id: 'e2', title: 'Iftar', status: 'CANCELLED')]));
    await pumpApp(tester, backend: backend, location: '/agenda');

    expect(find.text('Lezing over geduld'), findsOneWidget);
    // 18:30 UTC op 8 november 2030 is 19:30 in Amsterdam (wintertijd).
    expect(find.text('vr 8 nov · 19:30 – 21:00'), findsWidgets);
    expect(find.text('Geannuleerd'), findsOneWidget);
  });

  testWidgets('filtert op categorie via de server', (tester) async {
    final backend = standardBackend()
      ..onRequest('GET', '$base/events', (request) async {
        final youth = request.url.queryParameters['category'] == 'YOUTH';
        return FakeBackendJson.of(page(youth ? [] : [eventJson()]));
      });
    await pumpApp(tester, backend: backend, location: '/agenda');

    await tapVisible(tester, find.widgetWithText(ChoiceChip, 'Jongeren'));
    expect(backend.requestsTo('GET', '$base/events').last.url.queryParameters['category'], 'YOUTH');
    expect(find.text('Geen activiteiten in deze categorie'), findsOneWidget);

    await tapVisible(tester, find.text('Alle activiteiten tonen'));
    expect(find.text('Lezing over geduld'), findsOneWidget);
  });

  testWidgets('opent met een categorie uit de link', (tester) async {
    final backend = standardBackend();
    await pumpApp(tester, backend: backend, location: '/agenda?categorie=CLASS');
    expect(backend.requestsTo('GET', '$base/events').last.url.queryParameters['category'], 'CLASS');
    final chip = tester.widget<ChoiceChip>(find.widgetWithText(ChoiceChip, 'Les'));
    expect(chip.selected, isTrue);
  });

  testWidgets('laadt meer activiteiten op verzoek', (tester) async {
    final backend = standardBackend()
      ..onRequest('GET', '$base/events', (request) async {
        final pageNumber = int.parse(request.url.queryParameters['page'] ?? '0');
        return FakeBackendJson.of(
          page(
            [eventJson(id: 'e$pageNumber', title: 'Activiteit ${pageNumber + 1}')],
            page: pageNumber,
            totalPages: 2,
          ),
        );
      });
    await pumpApp(tester, backend: backend, location: '/agenda');
    expect(find.text('Activiteit 1'), findsOneWidget);

    await tapVisible(tester, find.text('Meer activiteiten laden'));
    expect(find.text('Activiteit 2'), findsOneWidget);
    expect(find.text('Meer activiteiten laden'), findsNothing);
  });

  testWidgets('toont een lege agenda met uitleg', (tester) async {
    await pumpApp(tester, location: '/agenda');
    expect(find.text('Nog geen activiteiten gepland'), findsOneWidget);
  });

  testWidgets('toont een fout met opnieuw proberen', (tester) async {
    final backend = standardBackend()..offline = true;
    await pumpApp(tester, backend: backend, location: '/agenda');
    expect(find.text('Geen verbinding'), findsOneWidget);

    backend.offline = false;
    backend.on('GET', '$base/events', page([eventJson()]));
    await tester.tap(find.text('Opnieuw proberen'));
    await settle(tester);
    expect(find.text('Lezing over geduld'), findsOneWidget);
  });

  testWidgets('toont offline de bewaarde agenda', (tester) async {
    final key = '${FakeBackend.baseUrl}/api/v1$base/events?page=0&size=20';
    final cached = jsonEncode({
      'body': jsonEncode(page([eventJson()])),
      'savedAt': '2026-01-15T08:00:00Z',
    });
    final backend = standardBackend()..offline = true;
    await pumpApp(tester, backend: backend, location: '/agenda', preferences: {'response-cache:$key': cached});
    expect(find.text('Je bent offline'), findsOneWidget);
    expect(find.textContaining('15 januari om 09:00'), findsOneWidget);
    expect(find.text('Lezing over geduld'), findsOneWidget);
  });
}
