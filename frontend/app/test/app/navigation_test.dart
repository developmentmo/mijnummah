import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../helpers/fixtures.dart';
import '../helpers/test_app.dart';

void main() {
  testWidgets('heeft vijf hoofdonderdelen die elk een eigen scherm openen', (tester) async {
    await pumpApp(tester);
    final bar = find.byType(NavigationBar);
    expect(bar, findsOneWidget);
    for (final (label, title) in [
      ('Agenda', 'Agenda'),
      ('Lezingen', 'Lezingen'),
      ('Ontdekken', 'Ontdekken'),
      ('Profiel', 'Profiel'),
    ]) {
      await tester.tap(find.descendant(of: bar, matching: find.text(label)));
      await settle(tester);
      expect(find.descendant(of: find.byType(AppBar), matching: find.text(title)), findsOneWidget);
    }
    await tester.tap(find.descendant(of: bar, matching: find.text('Home')));
    await settle(tester);
    expect(find.text('Assalamoe alaikoem'), findsOneWidget);
  });

  testWidgets('gebruikt een zijbalk op een tablet', (tester) async {
    await pumpApp(tester, size: const Size(1024, 1366));
    expect(find.byType(NavigationRail), findsOneWidget);
    expect(find.byType(NavigationBar), findsNothing);
  });

  testWidgets('een direct geopende detailpagina heeft een knop naar Home', (tester) async {
    final backend = standardBackend()..on('GET', '$base/events/e1', eventJson());
    await pumpApp(tester, backend: backend, location: '/activiteit/e1');
    await tester.tap(find.byTooltip('Naar Home'));
    await settle(tester);
    expect(find.text('Assalamoe alaikoem'), findsOneWidget);
  });

  group('geen overflow', () {
    final backend = standardBackend()
      ..on('GET', '$base/announcements', page([announcementJson(featured: true), announcementJson(id: 'a2')]))
      ..on('GET', '$base/events', page([eventJson(), eventJson(id: 'e2', status: 'CANCELLED')]))
      ..on('GET', '$base/media', page([mediaJson(), mediaJson(id: 'm2')]))
      ..on('GET', '$base/events/e1', eventJson(registrationEnabled: true, availablePlaces: 3))
      ..on('GET', '$base/media/m1', mediaJson())
      ..on('GET', '$base/media/m1/related', [mediaJson(id: 'm2')])
      ..on('GET', '$base/announcements/a1', announcementJson());

    for (final location in [
      '/',
      '/agenda',
      '/lezingen',
      '/ontdekken',
      '/profiel',
      '/activiteit/e1',
      '/lezing/m1',
      '/aankondiging/a1',
      '/inloggen',
      '/account-aanmaken',
      '/wachtwoord-vergeten',
    ]) {
      for (final (device, size, scale) in [
        ('kleine telefoon, 200% tekst', const Size(360, 780), 2.0),
        ('tablet', const Size(1024, 1366), 1.0),
      ]) {
        testWidgets('$location op $device', (tester) async {
          await pumpApp(tester, backend: backend, location: location, size: size, textScale: scale);
          await scrollThrough(tester);
          expect(tester.takeException(), isNull);
        });
      }
    }

    // Alleen voor ingelogde gebruikers.
    for (final location in ['/profiel', '/account']) {
      for (final (device, size, scale) in [
        ('kleine telefoon, 200% tekst', const Size(360, 780), 2.0),
        ('tablet', const Size(1024, 1366), 1.0),
      ]) {
        testWidgets('$location ingelogd op $device', (tester) async {
          final signedIn = standardBackend();
          await pumpApp(
            tester,
            backend: signedIn,
            tokenStore: signedInStore(signedIn),
            location: location,
            size: size,
            textScale: scale,
          );
          await scrollThrough(tester);
          expect(tester.takeException(), isNull);
        });
      }
    }
  });
}
