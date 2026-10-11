import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_backend.dart';
import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

void main() {
  const path = '$base/events/e1';

  testWidgets('toont alle details van een activiteit', (tester) async {
    final backend = standardBackend()..on('GET', path, eventJson());
    await pumpApp(tester, backend: backend, location: '/activiteit/e1');

    expect(find.text('Lezing over geduld'), findsOneWidget);
    expect(find.text('vrijdag 8 november 2030'), findsOneWidget);
    expect(find.text('19:30 – 21:00'), findsOneWidget);
    expect(find.text('Grote zaal'), findsOneWidget);
    expect(find.text('Voor iedereen'), findsOneWidget);
    expect(find.textContaining('geduld in het dagelijks leven'), findsOneWidget);
    expect(find.text('Aanmelden'), findsNothing);
  });

  testWidgets('geeft aan dat een activiteit alleen voor ingelogde gebruikers is', (tester) async {
    final backend = standardBackend()..on('GET', path, eventJson(visibility: 'REGISTERED_USERS'));
    await pumpApp(tester, backend: backend, location: '/activiteit/e1');
    expect(find.text('Alleen voor ingelogde gebruikers'), findsOneWidget);
  });

  testWidgets('toont een geannuleerde activiteit zonder aanmeldknop', (tester) async {
    final backend = standardBackend()
      ..on('GET', path, eventJson(status: 'CANCELLED', registrationEnabled: true, registeredByMe: true));
    await pumpApp(tester, backend: backend, location: '/activiteit/e1', tokenStore: signedInStore(backend));
    expect(find.text('Deze activiteit gaat niet door.'), findsOneWidget);
    expect(find.text('Aanmelding annuleren'), findsNothing);
  });

  testWidgets('vraagt een bezoeker in te loggen om zich aan te melden', (tester) async {
    final backend = standardBackend()..on('GET', path, eventJson(registrationEnabled: true, availablePlaces: 5));
    await pumpApp(tester, backend: backend, location: '/activiteit/e1');
    await tapVisible(tester, find.text('Inloggen om je aan te melden'));
    expect(find.text('E-mailadres'), findsOneWidget);
  });

  testWidgets('meldt aan, één keer, ook bij dubbel tikken', (tester) async {
    late final FakeBackend backend;
    backend = standardBackend()
      ..on('GET', path, eventJson(registrationEnabled: true, capacity: 40, availablePlaces: 12, registeredByMe: false))
      ..onRequest('POST', '/events/e1/registrations', (_) async {
        backend.on('GET', path, eventJson(registrationEnabled: true, availablePlaces: 11, registeredByMe: true));
        await Future<void>.delayed(const Duration(milliseconds: 300));
        return FakeBackendJson.of({'id': 'r1'}, status: 201);
      });
    await pumpApp(tester, backend: backend, location: '/activiteit/e1', tokenStore: signedInStore(backend));
    expect(find.text('Nog 12 plaatsen vrij.'), findsOneWidget);

    final button = find.widgetWithText(FilledButton, 'Aanmelden');
    await tester.ensureVisible(button);
    await tester.tap(button);
    await tester.pump();
    await tester.tap(find.byType(FilledButton).last, warnIfMissed: false);
    await settle(tester);

    expect(backend.requestsTo('POST', '/events/e1/registrations'), hasLength(1));
    expect(find.text('Je bent aangemeld voor deze activiteit.'), findsOneWidget);
    expect(find.text('Aanmelding annuleren'), findsOneWidget);
  });

  testWidgets('een tweede aanmelding wordt gemeld als "al aangemeld"', (tester) async {
    late final FakeBackend backend;
    backend = standardBackend()
      ..on('GET', path, eventJson(registrationEnabled: true, registeredByMe: false))
      ..onRequest('POST', '/events/e1/registrations', (_) async {
        backend.on('GET', path, eventJson(registrationEnabled: true, registeredByMe: true));
        return FakeBackendJson.of({'code': 'ALREADY_REGISTERED', 'status': 409}, status: 409);
      });
    await pumpApp(tester, backend: backend, location: '/activiteit/e1', tokenStore: signedInStore(backend));
    await tapVisible(tester, find.widgetWithText(FilledButton, 'Aanmelden'));
    expect(find.text('Je was al aangemeld voor deze activiteit.'), findsOneWidget);
    expect(find.text('Je bent aangemeld voor deze activiteit.'), findsOneWidget);
  });

  testWidgets('meldt dat de activiteit net vol is geraakt', (tester) async {
    final backend = standardBackend()
      ..on('GET', path, eventJson(registrationEnabled: true, availablePlaces: 1, registeredByMe: false))
      ..fail('POST', '/events/e1/registrations', 409, 'EVENT_FULL');
    await pumpApp(tester, backend: backend, location: '/activiteit/e1', tokenStore: signedInStore(backend));
    await tapVisible(tester, find.widgetWithText(FilledButton, 'Aanmelden'));
    expect(find.text('Helaas, deze activiteit is inmiddels vol.'), findsOneWidget);
  });

  testWidgets('toont een volle activiteit zonder aanmeldknop', (tester) async {
    final backend = standardBackend()
      ..on('GET', path, eventJson(registrationEnabled: true, capacity: 10, availablePlaces: 0, registeredByMe: false));
    await pumpApp(tester, backend: backend, location: '/activiteit/e1', tokenStore: signedInStore(backend));
    expect(find.text('Deze activiteit is vol.'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Aanmelden'), findsNothing);
  });

  testWidgets('annuleert een aanmelding na bevestiging', (tester) async {
    late final FakeBackend backend;
    backend = standardBackend()
      ..on('GET', path, eventJson(registrationEnabled: true, registeredByMe: true))
      ..onRequest('DELETE', '/events/e1/registrations/me', (_) async {
        backend.on('GET', path, eventJson(registrationEnabled: true, availablePlaces: 3, registeredByMe: false));
        return FakeBackendJson.of(null, status: 204);
      });
    await pumpApp(tester, backend: backend, location: '/activiteit/e1', tokenStore: signedInStore(backend));

    await tapVisible(tester, find.text('Aanmelding annuleren'));
    expect(find.text('Aanmelding annuleren?'), findsOneWidget);
    await tester.tap(find.widgetWithText(FilledButton, 'Annuleren'));
    await settle(tester);

    expect(backend.requestsTo('DELETE', '/events/e1/registrations/me'), hasLength(1));
    expect(find.text('Je aanmelding is geannuleerd.'), findsOneWidget);
    expect(find.text('Nog 3 plaatsen vrij.'), findsOneWidget);
  });

  testWidgets('een activiteit die niet bestaat geeft een duidelijke melding', (tester) async {
    final backend = standardBackend()..fail('GET', path, 404, 'EVENT_NOT_FOUND');
    await pumpApp(tester, backend: backend, location: '/activiteit/e1');
    expect(find.text('Niet gevonden'), findsOneWidget);
  });
}
