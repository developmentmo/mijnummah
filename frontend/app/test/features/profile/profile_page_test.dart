import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/core/brand/brand.g.dart';
import 'package:mijnummah_app/features/profile/profile_page.dart';

import '../../helpers/fake_backend.dart';
import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

void main() {
  testWidgets('een bezoeker ziet uitleg, een inlogknop en de vier statussen', (tester) async {
    await pumpApp(tester, location: '/profiel');
    expect(find.text('Je bent bezoeker'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, 'Account aanmaken'), findsOneWidget);
    for (final status in ['Bezoeker', 'Account', 'Volger', 'Lid']) {
      await tester.ensureVisible(find.text(status));
      expect(find.text(status), findsOneWidget);
    }
    expect(find.textContaining('niet via de app'), findsOneWidget);
  });

  testWidgets('een volger ziet en wijzigt de meldingsvoorkeuren', (tester) async {
    late final FakeBackend backend;
    backend = standardBackend()
      ..on('GET', '/me/follows/$mosqueId', followJson(media: false))
      ..onRequest('PUT', '/me/follows/$mosqueId/notifications', (request) async {
        backend.on('GET', '/me/follows/$mosqueId', followJson(media: true));
        return FakeBackendJson.of(followJson(media: true));
      });
    await pumpApp(tester, backend: backend, location: '/profiel', tokenStore: signedInStore(backend));

    expect(find.text('Je volgt Noeroel Islam sinds 1 september 2026.'), findsOneWidget);
    expect(find.textContaining('Pushmeldingen zijn in deze versie van de app niet ingesteld'), findsOneWidget);
    final media = find.widgetWithText(SwitchListTile, 'Nieuwe lezingen');
    expect(tester.widget<SwitchListTile>(media).value, isFalse);

    await tapVisible(tester, media);
    expect(jsonDecode(backend.requestsTo('PUT', '/me/follows/$mosqueId/notifications').single.body), {'media': true});
    expect(tester.widget<SwitchListTile>(media).value, isTrue);
  });

  testWidgets('ontvolgen vraagt om bevestiging', (tester) async {
    late final FakeBackend backend;
    backend = standardBackend()
      ..on('GET', '/me/follows/$mosqueId', followJson())
      ..onRequest('DELETE', '/me/follows/$mosqueId', (_) async {
        backend.fail('GET', '/me/follows/$mosqueId', 404, 'NOT_FOLLOWING');
        return FakeBackendJson.of(null, status: 204);
      });
    await pumpApp(tester, backend: backend, location: '/profiel', tokenStore: signedInStore(backend));

    await tapVisible(tester, find.widgetWithText(TextButton, 'Ontvolgen'));
    expect(find.text('Noeroel Islam ontvolgen?'), findsOneWidget);
    await tester.tap(find.widgetWithText(FilledButton, 'Ontvolgen'));
    await settle(tester);
    expect(backend.requestsTo('DELETE', '/me/follows/$mosqueId'), hasLength(1));
    expect(find.text('Volg Noeroel Islam'), findsOneWidget);
  });

  testWidgets('toont de eigen komende aanmeldingen', (tester) async {
    final backend = standardBackend()
      ..on(
        'GET',
        '/me/event-registrations',
        page([
          {
            'id': 'r1',
            'eventId': 'e1',
            'eventTitle': 'Lezing over geduld',
            'eventStartsAt': '2030-11-08T18:30:00Z',
            'mosqueId': mosqueId,
            'mosqueSlug': slug,
            'mosqueName': 'Noeroel Islam',
            'status': 'CONFIRMED',
            'registeredAt': '2026-10-01T10:00:00Z',
          },
        ]),
      );
    await pumpApp(tester, backend: backend, location: '/profiel', tokenStore: signedInStore(backend));
    await tester.ensureVisible(find.text('Lezing over geduld'));
    expect(find.text('vr 8 nov · 19:30'), findsOneWidget);
  });

  testWidgets('uitloggen na bevestiging maakt je weer bezoeker', (tester) async {
    final backend = standardBackend()..on('POST', '/auth/logout', null);
    final store = signedInStore(backend);
    await pumpApp(tester, backend: backend, location: '/profiel', tokenStore: store);

    await tapVisible(tester, find.widgetWithText(OutlinedButton, 'Uitloggen'));
    await tester.tap(find.widgetWithText(FilledButton, 'Uitloggen'));
    await settle(tester);
    expect(find.text('Je bent bezoeker'), findsOneWidget);
    expect(store.session, isNull);
    expect(backend.requestsTo('POST', '/auth/logout'), hasLength(1));
  });

  testWidgets('bewaart de gekozen weergave', (tester) async {
    await pumpApp(tester, location: '/profiel');
    await tapVisible(tester, find.widgetWithText(ChoiceChip, 'Donker'));
    expect(Theme.of(tester.element(find.text('Profiel').first)).brightness, Brightness.dark);
  });

  testWidgets('toont de privacyverklaring alleen als het merk er een heeft', (tester) async {
    await pumpApp(tester, location: '/profiel');
    await scrollThrough(tester);
    expect(find.text('Privacyverklaring'), Brand.privacyPolicyUrl == null ? findsNothing : findsOneWidget);
  });

  testWidgets('de privacylink noemt de site waar hij naartoe gaat', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(body: PrivacyPolicyTile(url: Uri.parse('https://moskee.example/privacy'))),
      ),
    );
    expect(find.text('Privacyverklaring'), findsOneWidget);
    expect(find.text('moskee.example'), findsOneWidget);
  });
}
