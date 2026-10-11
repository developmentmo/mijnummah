import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Een sessie: kortlevend access token plus het refresh token waarmee een nieuw paar wordt opgehaald.
class AuthSession {
  const AuthSession({required this.accessToken, required this.accessTokenExpiresAt, required this.refreshToken});

  factory AuthSession.fromJson(Map<String, dynamic> json) => AuthSession(
    accessToken: json['accessToken'] as String,
    accessTokenExpiresAt: DateTime.parse(json['accessTokenExpiresAt'] as String),
    refreshToken: json['refreshToken'] as String,
  );

  final String accessToken;
  final DateTime accessTokenExpiresAt;
  final String refreshToken;

  Map<String, dynamic> toJson() => {
    'accessToken': accessToken,
    'accessTokenExpiresAt': accessTokenExpiresAt.toUtc().toIso8601String(),
    'refreshToken': refreshToken,
  };

  @override
  String toString() => 'AuthSession(expiresAt: $accessTokenExpiresAt)';
}

/// Bewaart de sessie en het laatst bekende profiel tussen app-starts.
abstract interface class TokenStore {
  Future<AuthSession?> readSession();

  Future<Map<String, dynamic>?> readProfile();

  Future<void> save(AuthSession session, {Map<String, dynamic>? profile});

  Future<void> clear();
}

/// Bewaart tokens in de Keychain (iOS) of versleuteld met de Android Keystore, nooit in gewone voorkeuren.
class SecureTokenStore implements TokenStore {
  SecureTokenStore([FlutterSecureStorage? storage]) : _storage = storage ?? const FlutterSecureStorage();

  static const _sessionKey = 'auth.session';
  static const _profileKey = 'auth.profile';

  final FlutterSecureStorage _storage;

  @override
  Future<AuthSession?> readSession() async {
    final raw = await _read(_sessionKey);
    return raw == null ? null : AuthSession.fromJson(raw);
  }

  @override
  Future<Map<String, dynamic>?> readProfile() => _read(_profileKey);

  @override
  Future<void> save(AuthSession session, {Map<String, dynamic>? profile}) async {
    await _storage.write(key: _sessionKey, value: jsonEncode(session.toJson()));
    if (profile != null) await _storage.write(key: _profileKey, value: jsonEncode(profile));
  }

  @override
  Future<void> clear() async {
    await _storage.delete(key: _sessionKey);
    await _storage.delete(key: _profileKey);
  }

  Future<Map<String, dynamic>?> _read(String key) async {
    try {
      final raw = await _storage.read(key: key);
      return raw == null ? null : jsonDecode(raw) as Map<String, dynamic>;
    } on Exception {
      // Onleesbaar (bijvoorbeeld na het terugzetten van een back-up op een ander toestel): opnieuw inloggen.
      await _storage.delete(key: key);
      return null;
    }
  }
}
