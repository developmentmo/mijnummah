import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

void main() {
  group('account aanmaken', () {
    Future<void> fillIn(WidgetTester tester, {String password = 'een lang wachtwoord'}) async {
      await tester.enterText(find.widgetWithText(TextFormField, 'Naam'), 'Fatima');
      await tester.enterText(find.widgetWithText(TextFormField, 'E-mailadres'), 'fatima@example.org');
      await tester.enterText(find.widgetWithText(TextFormField, 'Wachtwoord'), password);
      await tapVisible(tester, find.widgetWithText(FilledButton, 'Account aanmaken'));
    }

    testWidgets('is bereikbaar vanaf het inlogscherm', (tester) async {
      await pumpApp(tester, location: '/inloggen');
      await tapVisible(tester, find.widgetWithText(OutlinedButton, 'Account aanmaken'));
      expect(find.widgetWithText(AppBar, 'Account aanmaken'), findsOneWidget);
    });

    testWidgets('controleert de wachtwoordlengte zoals de backend', (tester) async {
      final backend = standardBackend();
      await pumpApp(tester, backend: backend, location: '/account-aanmaken');
      await fillIn(tester, password: 'kort');
      expect(find.text('Gebruik minstens 12 tekens.'), findsOneWidget);
      expect(backend.requestsTo('POST', '/auth/register'), isEmpty);
    });

    testWidgets('maakt een account aan en toont daarna het profiel', (tester) async {
      final backend = standardBackend()
        ..on('POST', '/auth/register', tokensJson(), status: 201)
        ..on('GET', '/auth/me', userJson());
      await pumpApp(tester, backend: backend, location: '/account-aanmaken');
      await fillIn(tester);

      expect(jsonDecode(backend.requestsTo('POST', '/auth/register').single.body), {
        'email': 'fatima@example.org',
        'displayName': 'Fatima',
        'password': 'een lang wachtwoord',
      });
      expect(find.text('Welkom, Fatima. Je account is aangemaakt.'), findsOneWidget);
      expect(find.text('fatima@example.org'), findsOneWidget);
    });

    testWidgets('legt uit wat te doen als het adres al bestaat', (tester) async {
      final backend = standardBackend()..fail('POST', '/auth/register', 409, 'EMAIL_ALREADY_REGISTERED');
      await pumpApp(tester, backend: backend, location: '/account-aanmaken');
      await fillIn(tester);
      expect(find.textContaining('Er is al een account met dit e-mailadres'), findsOneWidget);
    });
  });

  group('wachtwoord vergeten', () {
    testWidgets('neemt het adres van het inlogscherm over', (tester) async {
      await pumpApp(tester, location: '/inloggen');
      await tester.enterText(find.widgetWithText(TextFormField, 'E-mailadres'), 'fatima@example.org');
      await tapVisible(tester, find.text('Wachtwoord vergeten?'));
      expect(find.widgetWithText(TextFormField, 'fatima@example.org'), findsOneWidget);
    });

    testWidgets('vraagt een code aan, zet een nieuw wachtwoord en logt in', (tester) async {
      final backend = standardBackend()
        ..on('POST', '/auth/password-reset', null, status: 202)
        ..on('POST', '/auth/password-reset/confirm', tokensJson())
        ..on('GET', '/auth/me', userJson());
      await pumpApp(tester, backend: backend, location: '/wachtwoord-vergeten');
      await tester.enterText(find.widgetWithText(TextFormField, 'E-mailadres'), 'fatima@example.org');
      await tapVisible(tester, find.widgetWithText(FilledButton, 'Code versturen'));

      expect(find.textContaining('Als er een account is met fatima@example.org'), findsOneWidget);
      await tester.enterText(find.widgetWithText(TextFormField, 'Code uit de e-mail'), 'abcd-efgh');
      await tester.enterText(find.widgetWithText(TextFormField, 'Nieuw wachtwoord'), 'een nieuw wachtwoord');
      await tapVisible(tester, find.widgetWithText(FilledButton, 'Wachtwoord opslaan'));

      expect(jsonDecode(backend.requestsTo('POST', '/auth/password-reset/confirm').single.body), {
        'email': 'fatima@example.org',
        'code': 'abcd-efgh',
        'newPassword': 'een nieuw wachtwoord',
      });
      expect(find.text('Je wachtwoord is gewijzigd en je bent ingelogd.'), findsOneWidget);
      expect(find.text('Uitloggen'), findsOneWidget);
    });

    testWidgets('controleert de lengte van de code', (tester) async {
      final backend = standardBackend()..on('POST', '/auth/password-reset', null, status: 202);
      await pumpApp(tester, backend: backend, location: '/wachtwoord-vergeten');
      await tester.enterText(find.widgetWithText(TextFormField, 'E-mailadres'), 'fatima@example.org');
      await tapVisible(tester, find.widgetWithText(FilledButton, 'Code versturen'));
      await tester.enterText(find.widgetWithText(TextFormField, 'Code uit de e-mail'), 'ABC');
      await tester.enterText(find.widgetWithText(TextFormField, 'Nieuw wachtwoord'), 'een nieuw wachtwoord');
      await tapVisible(tester, find.widgetWithText(FilledButton, 'Wachtwoord opslaan'));
      expect(find.text('De code heeft 8 tekens, bijvoorbeeld ABCD-EFGH.'), findsOneWidget);
      expect(backend.requestsTo('POST', '/auth/password-reset/confirm'), isEmpty);
    });

    testWidgets('meldt een verkeerde of verlopen code en laat een nieuwe aanvragen', (tester) async {
      final backend = standardBackend()
        ..on('POST', '/auth/password-reset', null, status: 202)
        ..fail('POST', '/auth/password-reset/confirm', 400, 'INVALID_RESET_CODE');
      await pumpApp(tester, backend: backend, location: '/wachtwoord-vergeten');
      await tester.enterText(find.widgetWithText(TextFormField, 'E-mailadres'), 'fatima@example.org');
      await tapVisible(tester, find.widgetWithText(FilledButton, 'Code versturen'));
      await tester.enterText(find.widgetWithText(TextFormField, 'Code uit de e-mail'), 'ABCD-EFGH');
      await tester.enterText(find.widgetWithText(TextFormField, 'Nieuw wachtwoord'), 'een nieuw wachtwoord');
      await tapVisible(tester, find.widgetWithText(FilledButton, 'Wachtwoord opslaan'));
      expect(find.textContaining('Deze code klopt niet of is verlopen'), findsOneWidget);

      await tapVisible(tester, find.text('Nieuwe code aanvragen'));
      expect(find.widgetWithText(FilledButton, 'Code versturen'), findsOneWidget);
    });

    testWidgets('zegt eerlijk dat herstellen niet kan als de moskee geen e-mail heeft ingesteld', (tester) async {
      final backend = standardBackend()..fail('POST', '/auth/password-reset', 503, 'PASSWORD_RESET_UNAVAILABLE');
      await pumpApp(tester, backend: backend, location: '/wachtwoord-vergeten');
      await tester.enterText(find.widgetWithText(TextFormField, 'E-mailadres'), 'fatima@example.org');
      await tapVisible(tester, find.widgetWithText(FilledButton, 'Code versturen'));
      expect(find.textContaining('nog niet ingesteld'), findsOneWidget);
      expect(find.widgetWithText(TextFormField, 'Code uit de e-mail'), findsNothing);
    });
  });
}
