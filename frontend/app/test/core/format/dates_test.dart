import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/core/format/dates.dart';

void main() {
  group('Dates', () {
    test('toont wintertijd (CET, UTC+1) in Amsterdam', () {
      final utc = DateTime.utc(2026, 11, 6, 17, 30);
      expect(Dates.time(utc), '18:30');
      expect(Dates.weekdayDate(utc), 'vrijdag 6 november 2026');
    });

    test('toont zomertijd (CEST, UTC+2) in Amsterdam', () {
      final utc = DateTime.utc(2026, 7, 3, 11, 0);
      expect(Dates.time(utc), '13:00');
      expect(Dates.weekdayDate(utc), 'vrijdag 3 juli 2026');
    });

    test('gaat goed om met de overgang naar zomertijd', () {
      // 29 maart 2026: om 02:00 lokale tijd gaat de klok naar 03:00.
      expect(Dates.time(DateTime.utc(2026, 3, 29, 0, 30)), '01:30');
      expect(Dates.time(DateTime.utc(2026, 3, 29, 1, 30)), '03:30');
    });

    test('gebruikt de lokale datum van de moskee, ook vlak na middernacht', () {
      // 23:30 UTC op 31 december is 00:30 op 1 januari in Amsterdam.
      expect(Dates.date(DateTime.utc(2026, 12, 31, 23, 30)), '1 januari 2027');
    });

    test('toont een tijdvak, met datums als het over middernacht loopt', () {
      expect(Dates.timeRange(DateTime.utc(2026, 11, 6, 17, 30), DateTime.utc(2026, 11, 6, 19)), '18:30 – 20:00');
      expect(
        Dates.timeRange(DateTime.utc(2026, 11, 6, 21), DateTime.utc(2026, 11, 7, 1)),
        'vr 6 nov 22:00 – za 7 nov 02:00',
      );
    });

    test('geeft dag en maand voor het datumblok zonder punt', () {
      expect(Dates.dayAndMonth(DateTime.utc(2026, 11, 6, 17, 30)), ('6', 'nov'));
    });

    test('formatteert een duur', () {
      expect(Dates.duration(const Duration(minutes: 45)), '45 min');
      expect(Dates.duration(const Duration(hours: 1, minutes: 5)), '1 u 05 min');
    });

    test('zegt wanneer offline gegevens zijn opgehaald', () {
      final now = DateTime.utc(2026, 11, 6, 12);
      expect(Dates.savedAt(DateTime.utc(2026, 11, 6, 9, 5), now: now), 'vandaag om 10:05');
      expect(Dates.savedAt(DateTime.utc(2026, 11, 3, 8, 12), now: now), '3 november om 09:12');
    });
  });
}
