import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

void main() {
  testWidgets('toont het nieuws van alle moskeeën met de naam van de moskee', (tester) async {
    final backend = standardBackend()..on('GET', '$base/announcements', page([announcementJson()]));
    await pumpApp(tester, backend: backend, location: '/nieuws');
    expect(find.text('Nieuwe lestijden'), findsOneWidget);
    expect(find.text('Mijn Ummah'), findsOneWidget);
    expect(backend.requestsTo('GET', '$base/announcements').last.url.queryParametersAll['mosque'], isNull);
  });

  testWidgets('filtert via de link op één moskee', (tester) async {
    final backend = standardBackend();
    await pumpApp(tester, backend: backend, location: '/nieuws?moskee=$slug');
    expect(backend.requestsTo('GET', '$base/announcements').last.url.queryParametersAll['mosque'], [slug]);
    expect(find.text('Geen nieuws gevonden'), findsOneWidget);

    await tester.tap(find.widgetWithText(ChoiceChip, 'Alle moskeeën'));
    await settle(tester);
    expect(backend.requestsTo('GET', '$base/announcements').last.url.queryParametersAll['mosque'], isNull);
  });
}
