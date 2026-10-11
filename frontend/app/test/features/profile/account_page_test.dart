import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

void main() {
  testWidgets('is bereikbaar vanaf het profiel', (tester) async {
    final backend = standardBackend();
    await pumpApp(tester, backend: backend, tokenStore: signedInStore(backend), location: '/profiel');
    await tapVisible(tester, find.text('Accountinstellingen'));
    expect(find.text('Ingelogd als fatima@example.org'), findsOneWidget);
  });

  testWidgets('wijzigt het wachtwoord en bewaart de nieuwe sessie', (tester) async {
    final backend = standardBackend()..on('PUT', '/me/password', tokensJson(access: 'access-2', refresh: 'refresh-2'));
    final store = signedInStore(backend);
    await pumpApp(tester, backend: backend, tokenStore: store, location: '/account');

    await tester.enterText(find.widgetWithText(TextFormField, 'Huidig wachtwoord'), 'oud wachtwoord');
    await tester.enterText(find.widgetWithText(TextFormField, 'Nieuw wachtwoord'), 'een nieuw wachtwoord');
    await tapVisible(tester, find.widgetWithText(FilledButton, 'Wachtwoord wijzigen'));

    expect(jsonDecode(backend.requestsTo('PUT', '/me/password').single.body), {
      'currentPassword': 'oud wachtwoord',
      'newPassword': 'een nieuw wachtwoord',
    });
    expect(store.session?.refreshToken, 'refresh-2');
    expect(find.textContaining('Op andere toestellen ben je uitgelogd'), findsOneWidget);
  });

  testWidgets('meldt een verkeerd huidig wachtwoord', (tester) async {
    final backend = standardBackend()..fail('PUT', '/me/password', 400, 'CURRENT_PASSWORD_INCORRECT');
    await pumpApp(tester, backend: backend, tokenStore: signedInStore(backend), location: '/account');
    await tester.enterText(find.widgetWithText(TextFormField, 'Huidig wachtwoord'), 'fout');
    await tester.enterText(find.widgetWithText(TextFormField, 'Nieuw wachtwoord'), 'een nieuw wachtwoord');
    await tapVisible(tester, find.widgetWithText(FilledButton, 'Wachtwoord wijzigen'));
    expect(find.text('Je huidige wachtwoord klopt niet.'), findsOneWidget);
  });

  Future<void> deleteWith(WidgetTester tester, String password) async {
    await tapVisible(tester, find.widgetWithText(OutlinedButton, 'Account verwijderen'));
    await tester.enterText(
      find.descendant(of: find.byType(AlertDialog), matching: find.byType(TextFormField)),
      password,
    );
    await tester.tap(find.widgetWithText(FilledButton, 'Verwijderen'));
    await settle(tester);
  }

  testWidgets('verwijdert het account na bevestiging met het wachtwoord', (tester) async {
    final backend = standardBackend()..on('POST', '/me/deletion', null, status: 204);
    final store = signedInStore(backend);
    await pumpApp(tester, backend: backend, tokenStore: store, location: '/account');
    await deleteWith(tester, 'mijn wachtwoord');

    expect(jsonDecode(backend.requestsTo('POST', '/me/deletion').single.body), {'password': 'mijn wachtwoord'});
    expect(store.session, isNull);
    expect(find.text('Je account is verwijderd.'), findsOneWidget);
    expect(find.text('Je bent bezoeker'), findsOneWidget);
  });

  testWidgets('legt uit waarom een beheerdersaccount niet zelf kan worden verwijderd', (tester) async {
    final backend = standardBackend()..fail('POST', '/me/deletion', 409, 'ACCOUNT_DELETION_BLOCKED');
    final store = signedInStore(backend);
    await pumpApp(tester, backend: backend, tokenStore: store, location: '/account');
    await deleteWith(tester, 'mijn wachtwoord');

    expect(find.textContaining('Je beheert een moskee'), findsOneWidget);
    expect(store.session, isNotNull);
  });
}
