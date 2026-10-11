import 'package:intl/date_symbol_data_local.dart';
import 'package:intl/intl.dart';
import 'package:timezone/data/latest_10y.dart' as tzdata;
import 'package:timezone/timezone.dart' as tz;

import '../config/app_config.dart';

/// Nederlandse datum- en tijdnotaties in de tijdzone van de moskee (`Europe/Amsterdam`), ongeacht de tijdzone
/// van het toestel. De backend levert alle tijden in UTC.
abstract final class Dates {
  static const _locale = 'nl';

  static final bool _ready = () {
    tzdata.initializeTimeZones();
    initializeDateFormatting(_locale);
    return true;
  }();

  /// Sinds tzdata 2022b is `Europe/Amsterdam` een verwijzing naar `Europe/Brussels` (sinds 1970 dezelfde regels).
  /// De compacte database van het timezone-pakket bevat geen verwijzingen, daarom de terugval.
  static final tz.Location _zone = () {
    _ensureReady();
    final locations = tz.timeZoneDatabase.locations;
    return locations[AppConfig.timeZone] ?? locations[_canonicalZones[AppConfig.timeZone]] ?? tz.UTC;
  }();

  static const _canonicalZones = {'Europe/Amsterdam': 'Europe/Brussels'};

  static void _ensureReady() {
    if (!_ready) throw StateError('Datumgegevens niet geladen');
  }

  static DateFormat _format(String pattern) {
    _ensureReady();
    return DateFormat(pattern, _locale);
  }

  static final _weekdayDate = _format('EEEE d MMMM y');
  static final _date = _format('d MMMM y');
  static final _dayMonth = _format('d MMMM');
  static final _shortDate = _format('EEE d MMM');
  static final _time = _format('HH:mm');
  static final _monthAbbr = _format('MMM');

  /// Zet een UTC-tijd om naar de lokale tijd van de moskee.
  static tz.TZDateTime local(DateTime utc) => tz.TZDateTime.from(utc, _zone);

  /// `vrijdag 6 november 2026`
  static String weekdayDate(DateTime utc) => _weekdayDate.format(local(utc));

  /// `6 november 2026`
  static String date(DateTime utc) => _date.format(local(utc));

  /// `6 november 2026` voor een kalenderdatum zonder tijd (zoals een opnamedatum).
  static String calendarDate(DateTime date) => _date.format(date);

  /// `18:30`
  static String time(DateTime utc) => _time.format(local(utc));

  /// `vr 6 nov`
  static String shortDate(DateTime utc) => _shortDate.format(local(utc));

  /// Dagnummer en maand (`6`, `nov`) voor een datumblokje.
  static (String day, String month) dayAndMonth(DateTime utc) {
    final value = local(utc);
    return ('${value.day}', _monthAbbr.format(value).replaceAll('.', ''));
  }

  /// `18:30 – 20:00`, of met datums als de activiteit over middernacht loopt.
  static String timeRange(DateTime startUtc, DateTime endUtc) {
    final start = local(startUtc);
    final end = local(endUtc);
    final sameDay = start.year == end.year && start.month == end.month && start.day == end.day;
    return sameDay
        ? '${time(startUtc)} – ${time(endUtc)}'
        : '${shortDate(startUtc)} ${time(startUtc)} – ${shortDate(endUtc)} ${time(endUtc)}';
  }

  /// `vr 6 nov · 18:30` voor kaarten.
  static String compact(DateTime utc) => '${shortDate(utc)} · ${time(utc)}';

  /// `1 u 05 min` of `45 min`
  static String duration(Duration value) {
    final hours = value.inHours;
    final minutes = value.inMinutes.remainder(60);
    if (hours == 0) return '${value.inMinutes} min';
    return '$hours u ${minutes.toString().padLeft(2, '0')} min';
  }

  /// Hoe lang geleden de offline getoonde gegevens zijn opgehaald: `vandaag om 14:05` of `3 november om 09:12`.
  static String savedAt(DateTime value, {DateTime? now}) {
    final saved = local(value.toUtc());
    final today = local((now ?? DateTime.now()).toUtc());
    final sameDay = saved.year == today.year && saved.month == today.month && saved.day == today.day;
    return sameDay ? 'vandaag om ${_time.format(saved)}' : '${_dayMonth.format(saved)} om ${_time.format(saved)}';
  }
}
