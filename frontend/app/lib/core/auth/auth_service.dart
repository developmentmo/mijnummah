import 'dart:async';

import '../../data/json.dart';
import '../../data/models.dart';
import '../api/api_client.dart';
import '../api/api_exception.dart';
import 'token_store.dart';

/// Inloggen, registreren, uitloggen, wachtwoordherstel en het verversen van tokens.
///
/// Refresh tokens roteren: elk token is één keer bruikbaar en hergebruik trekt de hele sessie in. Daarom loopt
/// er altijd hoogstens één verversing tegelijk; gelijktijdige verzoeken wachten op dezelfde verversing.
class AuthService implements AccessTokenSource {
  AuthService(this._api, this._store, {DateTime Function()? now}) : _now = now ?? DateTime.now {
    _api.tokens = this;
  }

  final ApiClient _api;
  final TokenStore _store;
  final DateTime Function() _now;

  AuthSession? _session;
  Future<String?>? _refreshing;
  final _signedOut = StreamController<void>.broadcast();

  /// Meldt dat de sessie buiten de gebruiker om is beëindigd (verlopen of ingetrokken).
  Stream<void> get sessionEnded => _signedOut.stream;

  bool get hasSession => _session != null;

  /// Herstelt een bewaarde sessie bij het starten van de app. Zonder verbinding wordt het laatst bekende profiel
  /// gebruikt; de sessie wordt dan bij het eerstvolgende verzoek ververst.
  Future<UserProfile?> restore() async {
    _session = await _store.readSession();
    if (_session == null) return null;
    try {
      final profile = UserProfile.fromJson(await _api.get('/auth/me') as Json);
      if (_session != null) await _store.save(_session!, profile: profile.toJson());
      return profile;
    } on NetworkException {
      final cached = await _store.readProfile();
      return cached == null ? null : UserProfile.fromJson(cached);
    } on ApiException catch (e) {
      if (e.status == 401 || e.status == 403) {
        await _endSession();
        return null;
      }
      rethrow;
    }
  }

  Future<UserProfile> login(String email, String password) async {
    final tokens = await _api.post(
      '/auth/login',
      body: {'email': email.trim(), 'password': password},
      authenticated: false,
    );
    return _start(tokens as Json);
  }

  /// Maakt een account aan en logt direct in. Fouten zoals `EMAIL_ALREADY_REGISTERED` of `INVALID_PASSWORD` gaan
  /// naar de aanroeper.
  Future<UserProfile> register({required String email, required String displayName, required String password}) async {
    final tokens = await _api.post(
      '/auth/register',
      body: {'email': email.trim(), 'displayName': displayName.trim(), 'password': password},
      authenticated: false,
    );
    return _start(tokens as Json);
  }

  /// Vraagt een herstelcode aan. De backend antwoordt altijd hetzelfde, ook als er geen account met dit adres is;
  /// zonder ingestelde e-mail geeft hij `PASSWORD_RESET_UNAVAILABLE`.
  Future<void> requestPasswordReset(String email) =>
      _api.post('/auth/password-reset', body: {'email': email.trim()}, authenticated: false);

  /// Zet een nieuw wachtwoord met de code uit de e-mail en logt in. Andere sessies worden door de backend beëindigd.
  Future<UserProfile> confirmPasswordReset({
    required String email,
    required String code,
    required String newPassword,
  }) async {
    final tokens = await _api.post(
      '/auth/password-reset/confirm',
      body: {'email': email.trim(), 'code': code.trim(), 'newPassword': newPassword},
      authenticated: false,
    );
    return _start(tokens as Json);
  }

  /// Wijzigt het wachtwoord. De backend beëindigt alle sessies en geeft dit toestel een nieuwe.
  Future<void> changePassword({required String currentPassword, required String newPassword}) async {
    final tokens = await _api.put(
      '/me/password',
      body: {'currentPassword': currentPassword, 'newPassword': newPassword},
    );
    _session = _sessionFrom(tokens as Json);
    await _store.save(_session!);
  }

  /// Verwijdert het account definitief en beëindigt de sessie. Fouten (zoals een verkeerd wachtwoord of
  /// `ACCOUNT_DELETION_BLOCKED`) gaan naar de aanroeper; de sessie blijft dan bestaan.
  Future<void> deleteAccount(String password) async {
    await _api.post('/me/deletion', body: {'password': password});
    await _endSession();
  }

  Future<UserProfile> _start(Json tokens) async {
    _session = _sessionFrom(tokens);
    try {
      final profile = UserProfile.fromJson(await _api.get('/auth/me') as Json);
      await _store.save(_session!, profile: profile.toJson());
      return profile;
    } on AppFailure {
      _session = null;
      rethrow;
    }
  }

  /// Beëindigt de sessie, ook als de server niet bereikbaar is.
  Future<void> logout() async {
    final session = _session;
    await _endSession();
    if (session == null) return;
    try {
      await _api.post('/auth/logout', body: {'refreshToken': session.refreshToken}, authenticated: false);
    } on AppFailure {
      // Lokaal is de sessie al weg; de server laat het refresh token vanzelf verlopen.
    }
  }

  @override
  Future<String?> currentAccessToken() async {
    final session = _session;
    if (session == null) return null;
    final expiresSoon = _now().add(const Duration(seconds: 30)).isAfter(session.accessTokenExpiresAt);
    return expiresSoon ? refreshAccessToken() : session.accessToken;
  }

  @override
  Future<String?> refreshAccessToken() => _refreshing ??= _refresh().whenComplete(() => _refreshing = null);

  Future<String?> _refresh() async {
    final session = _session;
    if (session == null) return null;
    try {
      final tokens = await _api.post(
        '/auth/refresh',
        body: {'refreshToken': session.refreshToken},
        authenticated: false,
      );
      _session = _sessionFrom(tokens as Json);
      await _store.save(_session!);
      return _session!.accessToken;
    } on ApiException catch (e) {
      if (e.status == 401 || e.status == 403) {
        await _endSession();
        _signedOut.add(null);
        return null;
      }
      rethrow;
    }
  }

  Future<void> _endSession() async {
    _session = null;
    await _store.clear();
    // De offline-cache kan gegevens van deze gebruiker bevatten (zoals `registeredByMe`).
    await _api.clearCache();
  }

  AuthSession _sessionFrom(Json json) => AuthSession(
    accessToken: json.str('accessToken'),
    accessTokenExpiresAt: json.time('accessTokenExpiresAt'),
    refreshToken: json.str('refreshToken'),
  );

  void dispose() => _signedOut.close();
}
