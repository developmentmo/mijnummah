import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

void main() {
  Future<void> fillIn(WidgetTester tester, {String email = 'fatima@example.org', String password = 'geheim'}) async {
    await tester.enterText(find.widgetWithText(TextFormField, 'E-mailadres'), email);
    await tester.enterText(find.widgetWithText(TextFormField, 'Wachtwoord'), password);
    await tapVisible(tester, find.widgetWithText(FilledButton, 'Inloggen'));
  }

  testWidgets('controleert de velden voordat er iets wordt verstuurd', (tester) async {
    final backend = standardBackend();
    await pumpApp(tester, backend: backend, location: '/inloggen');
    await fillIn(tester, email: 'geen-adres', password: '');
    expect(find.text('Vul een geldig e-mailadres in.'), findsOneWidget);
    expect(find.text('Vul je wachtwoord in.'), findsOneWidget);
    expect(backend.requestsTo('POST', '/auth/login'), isEmpty);
  });

  testWidgets('legt een verkeerd wachtwoord uit zonder te zeggen welk deel fout is', (tester) async {
    final backend = standardBackend()..fail('POST', '/auth/login', 401, 'INVALID_CREDENTIALS');
    await pumpApp(tester, backend: backend, location: '/inloggen');
    await fillIn(tester);
    expect(find.text('Het e-mailadres of wachtwoord klopt niet.'), findsOneWidget);
  });

  testWidgets('noemt de wachttijd na te veel pogingen', (tester) async {
    final backend = standardBackend()
      ..fail('POST', '/auth/login', 429, 'TOO_MANY_LOGIN_ATTEMPTS', headers: {'retry-after': '300'});
    await pumpApp(tester, backend: backend, location: '/inloggen');
    await fillIn(tester);
    expect(find.text('Te veel pogingen. Probeer het over 5 minuten opnieuw.'), findsOneWidget);
  });

  testWidgets('meldt een geblokkeerd account', (tester) async {
    final backend = standardBackend()..fail('POST', '/auth/login', 403, 'ACCOUNT_BLOCKED');
    await pumpApp(tester, backend: backend, location: '/inloggen');
    await fillIn(tester);
    expect(find.textContaining('geblokkeerd'), findsOneWidget);
  });

  testWidgets('logt in en toont daarna het profiel', (tester) async {
    final backend = standardBackend()
      ..on('POST', '/auth/login', tokensJson())
      ..on('GET', '/auth/me', userJson());
    await pumpApp(tester, backend: backend, location: '/profiel');
    await tapVisible(tester, find.widgetWithText(FilledButton, 'Inloggen'));
    await fillIn(tester);

    expect(find.text('Welkom, Fatima.'), findsOneWidget);
    expect(find.text('fatima@example.org'), findsOneWidget);
    expect(find.text('Uitloggen'), findsOneWidget);
  });
}
