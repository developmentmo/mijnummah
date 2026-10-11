import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_backend.dart';
import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

void main() {
  testWidgets('toont het profiel van de moskee met adres en contact', (tester) async {
    await pumpApp(tester, location: '/ontdekken');
    expect(find.text('Noeroel Islam'), findsWidgets);
    expect(find.text('Een moskee midden in de wijk.'), findsOneWidget);
    await tester.ensureVisible(find.text('Voorbeeldstraat 1, 1234 AB Voorbeeldstad'));
    expect(find.text('010 123 4567'), findsOneWidget);
    expect(find.text('info@example.org'), findsOneWidget);
    expect(find.text('example.org'), findsOneWidget);
  });

  testWidgets('verbergt contactgegevens die de moskee niet heeft ingevuld', (tester) async {
    final backend = FakeBackend()
      ..on('GET', base, {
        'id': mosqueId,
        'slug': slug,
        'name': 'Noeroel Islam',
        'address': <String, Object?>{},
        'contact': <String, Object?>{},
      });
    await pumpApp(tester, backend: backend, location: '/ontdekken');
    expect(find.text('Adres en contact'), findsNothing);
    expect(find.text('Kennismaken'), findsOneWidget);
  });

  testWidgets('opent de agenda gefilterd op een activiteitensoort', (tester) async {
    final backend = standardBackend();
    await pumpApp(tester, backend: backend, location: '/ontdekken');
    await tapVisible(tester, find.widgetWithText(ListTile, 'Jongeren'));
    expect(find.text('Agenda'), findsWidgets);
    expect(backend.requestsTo('GET', '$base/events').last.url.queryParameters['category'], 'YOUTH');
  });
}
