import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

/// Een eerder opgehaald antwoord, met het moment waarop het is opgehaald.
class CachedResponse {
  const CachedResponse(this.body, this.savedAt);

  final String body;
  final DateTime savedAt;
}

/// Bewaart de laatst opgehaalde publieke antwoorden, zodat de app zonder verbinding de laatst bekende
/// gegevens kan tonen. Bevat alleen publieke content, nooit tokens of persoonsgegevens.
abstract interface class ResponseCache {
  Future<CachedResponse?> read(String key);

  Future<void> write(String key, String body);

  Future<void> clear();
}

class SharedPreferencesResponseCache implements ResponseCache {
  SharedPreferencesResponseCache(this._prefs, {DateTime Function()? now}) : _now = now ?? DateTime.now;

  static const _prefix = 'response-cache:';

  final SharedPreferences _prefs;
  final DateTime Function() _now;

  @override
  Future<CachedResponse?> read(String key) async {
    final raw = _prefs.getString('$_prefix$key');
    if (raw == null) return null;
    try {
      final entry = jsonDecode(raw) as Map<String, dynamic>;
      return CachedResponse(entry['body'] as String, DateTime.parse(entry['savedAt'] as String));
    } on FormatException {
      return null;
    } on TypeError {
      return null;
    }
  }

  @override
  Future<void> write(String key, String body) =>
      _prefs.setString('$_prefix$key', jsonEncode({'body': body, 'savedAt': _now().toUtc().toIso8601String()}));

  @override
  Future<void> clear() async {
    for (final key in _prefs.getKeys().where((k) => k.startsWith(_prefix)).toList()) {
      await _prefs.remove(key);
    }
  }
}
