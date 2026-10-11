import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_backend.dart';
import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

void main() {
  group('bibliotheek', () {
    testWidgets('toont lezingen met spreker en duur', (tester) async {
      final backend = standardBackend()..on('GET', '$base/media', page([mediaJson()]));
      await pumpApp(tester, backend: backend, location: '/lezingen');
      expect(find.text('Tafsir van soera Al-Asr'), findsOneWidget);
      expect(find.text('Imam Voorbeeld · 2 oktober 2030'), findsOneWidget);
      expect(find.text('45 min'), findsOneWidget);
    });

    testWidgets('zoekt na een korte pauze in het typen, via de server', (tester) async {
      final backend = standardBackend()
        ..onRequest('GET', '$base/media', (request) async {
          final q = request.url.queryParameters['q'];
          return FakeBackendJson.of(page(q == 'asr' ? [mediaJson()] : (q == null ? [mediaJson()] : [])));
        });
      await pumpApp(tester, backend: backend, location: '/lezingen');

      await tester.enterText(find.byType(TextField), 'as');
      await tester.pump(const Duration(milliseconds: 100));
      await tester.enterText(find.byType(TextField), 'asr');
      await settle(tester);
      final searches = backend.requestsTo('GET', '$base/media').where((r) => r.url.queryParameters['q'] != null);
      expect(searches.map((r) => r.url.queryParameters['q']), ['asr']);
      expect(find.text('1 resultaat'), findsOneWidget);

      await tester.enterText(find.byType(TextField), 'onbekend');
      await settle(tester);
      expect(find.text('Niets gevonden'), findsOneWidget);
      expect(find.textContaining('"onbekend"'), findsOneWidget);

      await tester.tap(find.text('Alles tonen'));
      await settle(tester);
      expect(find.text('Tafsir van soera Al-Asr'), findsOneWidget);
    });

    testWidgets('filtert op categorie', (tester) async {
      final backend = standardBackend();
      await pumpApp(tester, backend: backend, location: '/lezingen');
      await tapVisible(tester, find.widgetWithText(ChoiceChip, 'Khutbah'));
      expect(backend.requestsTo('GET', '$base/media').last.url.queryParameters['category'], 'KHUTBAH');
    });

    testWidgets('toont een lege bibliotheek met uitleg', (tester) async {
      await pumpApp(tester, location: '/lezingen');
      expect(find.text('Nog geen lezingen'), findsOneWidget);
    });
  });

  group('detail', () {
    FakeBackend detailBackend(Map<String, Object?> item) => standardBackend()
      ..on('GET', '$base/media/m1', item)
      ..on('GET', '$base/media/m1/related', [mediaJson(id: 'm2', title: 'Tafsir van soera Al-Kawthar')]);

    testWidgets('toont de video, gegevens en gerelateerde content', (tester) async {
      final backend = detailBackend(
        mediaJson(relatedEvent: {'id': 'e1', 'title': 'Lezingenavond', 'startsAt': '2030-10-01T17:00:00Z'}),
      );
      await pumpApp(tester, backend: backend, location: '/lezing/m1');

      expect(find.text('speler dQw4w9WgXcQ'), findsOneWidget);
      expect(find.text('Tafsir van soera Al-Asr'), findsOneWidget);
      expect(find.text('Imam Voorbeeld'), findsOneWidget);
      expect(find.text('Gepubliceerd op 2 oktober 2030'), findsOneWidget);
      expect(find.text('Opgenomen op 1 oktober 2030'), findsOneWidget);
      expect(find.text('Lezingenavond'), findsOneWidget);

      await tester.ensureVisible(find.text('Tafsir van soera Al-Kawthar'));
      expect(find.text('Meer zoals dit'), findsOneWidget);
    });

    testWidgets('navigeert naar de gerelateerde activiteit', (tester) async {
      final backend = detailBackend(
        mediaJson(relatedEvent: {'id': 'e1', 'title': 'Lezingenavond', 'startsAt': '2030-10-01T17:00:00Z'}),
      )..on('GET', '$base/events/e1', eventJson(title: 'Lezingenavond'));
      await pumpApp(tester, backend: backend, location: '/lezing/m1');
      await tapVisible(tester, find.text('Lezingenavond'));
      expect(find.text('Activiteit'), findsOneWidget);
    });

    testWidgets('meldt duidelijk als de video niet kan worden afgespeeld', (tester) async {
      await pumpApp(tester, backend: detailBackend(mediaJson()), location: '/lezing/m1');
      // De nep-speler meldt bij een tik dat de video niet beschikbaar is, zoals de echte speler bij een fout.
      await tester.tap(find.text('speler dQw4w9WgXcQ'));
      await settle(tester);
      expect(find.text('Video niet beschikbaar'), findsOneWidget);
      expect(find.text('Bekijken op YouTube'), findsOneWidget);
    });

    testWidgets('een item zonder video toont meteen de melding', (tester) async {
      await pumpApp(tester, backend: detailBackend(mediaJson(videoId: null)), location: '/lezing/m1');
      expect(find.text('Video niet beschikbaar'), findsOneWidget);
      expect(find.text('Bekijken op YouTube'), findsNothing);
    });
  });
}
