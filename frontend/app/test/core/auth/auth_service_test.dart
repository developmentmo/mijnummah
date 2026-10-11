import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/core/api/api_client.dart';
import 'package:mijnummah_app/core/api/api_exception.dart';
import 'package:mijnummah_app/core/api/response_cache.dart';
import 'package:mijnummah_app/core/auth/auth_service.dart';
import 'package:mijnummah_app/core/auth/token_store.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../helpers/fake_backend.dart';
import '../../helpers/fixtures.dart';
import '../../helpers/test_app.dart';

void main() {
  late FakeBackend backend;
  late InMemoryTokenStore store;
  late AuthService auth;
  late SharedPreferences prefs;

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    prefs = await SharedPreferences.getInstance();
    backend = FakeBackend();
    store = InMemoryTokenStore();
    final client = ApiClient(
      baseUrl: FakeBackend.baseUrl,
      httpClient: backend.client,
      cache: SharedPreferencesResponseCache(prefs),
    );
    auth = AuthService(client, store, now: () => DateTime.utc(2026, 10, 11, 12));
  });

  tearDown(() => auth.dispose());

  test('logt in, bewaart sessie en profiel', () async {
    backend
      ..on('POST', '/auth/login', tokensJson())
      ..on('GET', '/auth/me', userJson());
    final profile = await auth.login(' fatima@example.org ', 'geheim');
    expect(profile.displayName, 'Fatima');
    expect(store.session?.refreshToken, 'refresh-1');
    expect(store.profile?['email'], 'fatima@example.org');
    expect(jsonDecode(backend.requestsTo('POST', '/auth/login').single.body), {
      'email': 'fatima@example.org',
      'password': 'geheim',
    });
    expect(backend.requestsTo('GET', '/auth/me').single.headers['Authorization'], 'Bearer access-1');
  });

  test('geeft INVALID_CREDENTIALS door en bewaart niets', () async {
    backend.fail('POST', '/auth/login', 401, 'INVALID_CREDENTIALS');
    await expectLater(
      auth.login('a@b.nl', 'fout'),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'INVALID_CREDENTIALS')),
    );
    expect(store.session, isNull);
    expect(auth.hasSession, isFalse);
  });

  test('herstelt een sessie offline met het laatst bekende profiel', () async {
    store
      ..session = AuthSession(accessToken: 'a', accessTokenExpiresAt: DateTime.utc(2099), refreshToken: 'r')
      ..profile = userJson();
    backend.offline = true;
    final profile = await auth.restore();
    expect(profile?.displayName, 'Fatima');
    expect(auth.hasSession, isTrue);
  });

  test('ververst een bijna verlopen token vooraf, maar één keer voor gelijktijdige verzoeken', () async {
    store.session = AuthSession(
      accessToken: 'old',
      accessTokenExpiresAt: DateTime.utc(2026, 10, 11, 12, 0, 10),
      refreshToken: 'r1',
    );
    await auth.restore().catchError((_) => null);
    backend.requests.clear();

    final release = Completer<void>();
    backend.onRequest('POST', '/auth/refresh', (_) async {
      await release.future;
      return FakeBackend.json(tokensJson(access: 'new', refresh: 'r2'));
    });
    final first = auth.currentAccessToken();
    final second = auth.currentAccessToken();
    release.complete();
    expect(await Future.wait([first, second]), ['new', 'new']);
    expect(backend.requestsTo('POST', '/auth/refresh'), hasLength(1));
    expect(store.session?.refreshToken, 'r2');
  });

  test('beëindigt de sessie als het refresh token is ingetrokken', () async {
    store.session = AuthSession(accessToken: 'a', accessTokenExpiresAt: DateTime.utc(2099), refreshToken: 'r');
    backend
      ..on('GET', '/auth/me', userJson())
      ..fail('POST', '/auth/refresh', 401, 'INVALID_REFRESH_TOKEN');
    await auth.restore();
    final ended = expectLater(auth.sessionEnded, emits(null));
    expect(await auth.refreshAccessToken(), isNull);
    await ended;
    expect(store.session, isNull);
    expect(auth.hasSession, isFalse);
  });

  test('wist bij uitloggen de offline-cache en logt ook uit als de server onbereikbaar is', () async {
    await prefs.setString('response-cache:x', '{"body":"{}","savedAt":"2026-10-11T10:00:00Z"}');
    store.session = AuthSession(accessToken: 'a', accessTokenExpiresAt: DateTime.utc(2099), refreshToken: 'r');
    backend.on('GET', '/auth/me', userJson());
    await auth.restore();
    backend.offline = true;
    await auth.logout();
    expect(store.session, isNull);
    expect(prefs.getKeys().where((k) => k.startsWith('response-cache:')), isEmpty);
  });

  group('account', () {
    test('registreert en logt direct in', () async {
      backend
        ..on('POST', '/auth/register', tokensJson(), status: 201)
        ..on('GET', '/auth/me', userJson());
      final profile = await auth.register(
        email: ' fatima@example.org ',
        displayName: ' Fatima ',
        password: 'lang wachtwoord',
      );
      expect(profile.displayName, 'Fatima');
      expect(store.session?.accessToken, 'access-1');
      expect(jsonDecode(backend.requestsTo('POST', '/auth/register').single.body), {
        'email': 'fatima@example.org',
        'displayName': 'Fatima',
        'password': 'lang wachtwoord',
      });
      expect(backend.requestsTo('POST', '/auth/register').single.headers['Authorization'], isNull);
    });

    test('geeft EMAIL_ALREADY_REGISTERED door zonder sessie', () async {
      backend.fail('POST', '/auth/register', 409, 'EMAIL_ALREADY_REGISTERED');
      await expectLater(
        auth.register(email: 'a@b.nl', displayName: 'A', password: 'lang wachtwoord'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'EMAIL_ALREADY_REGISTERED')),
      );
      expect(auth.hasSession, isFalse);
    });

    test('vraagt een herstelcode aan en logt in met de code', () async {
      backend
        ..on('POST', '/auth/password-reset', null, status: 202)
        ..on('POST', '/auth/password-reset/confirm', tokensJson(access: 'access-2'))
        ..on('GET', '/auth/me', userJson());
      await auth.requestPasswordReset(' fatima@example.org ');
      expect(jsonDecode(backend.requestsTo('POST', '/auth/password-reset').single.body), {
        'email': 'fatima@example.org',
      });

      await auth.confirmPasswordReset(
        email: 'fatima@example.org',
        code: ' abcd-efgh ',
        newPassword: 'nieuw wachtwoord',
      );
      expect(jsonDecode(backend.requestsTo('POST', '/auth/password-reset/confirm').single.body), {
        'email': 'fatima@example.org',
        'code': 'abcd-efgh',
        'newPassword': 'nieuw wachtwoord',
      });
      expect(store.session?.accessToken, 'access-2');
    });

    test('vervangt de sessie na het wijzigen van het wachtwoord', () async {
      backend
        ..on('POST', '/auth/login', tokensJson())
        ..on('GET', '/auth/me', userJson())
        ..on('PUT', '/me/password', tokensJson(access: 'access-2', refresh: 'refresh-2'));
      await auth.login('fatima@example.org', 'oud wachtwoord');
      await auth.changePassword(currentPassword: 'oud wachtwoord', newPassword: 'nieuw wachtwoord');

      final request = backend.requestsTo('PUT', '/me/password').single;
      expect(request.headers['Authorization'], 'Bearer access-1');
      expect(jsonDecode(request.body), {'currentPassword': 'oud wachtwoord', 'newPassword': 'nieuw wachtwoord'});
      expect(store.session?.refreshToken, 'refresh-2');
      expect(store.profile?['email'], 'fatima@example.org');
      expect(await auth.currentAccessToken(), 'access-2');
    });

    test('verwijdert het account en beëindigt de sessie', () async {
      backend
        ..on('POST', '/auth/login', tokensJson())
        ..on('GET', '/auth/me', userJson())
        ..on('POST', '/me/deletion', null, status: 204);
      await auth.login('fatima@example.org', 'wachtwoord');
      await auth.deleteAccount('wachtwoord');
      expect(jsonDecode(backend.requestsTo('POST', '/me/deletion').single.body), {'password': 'wachtwoord'});
      expect(auth.hasSession, isFalse);
      expect(store.session, isNull);
    });

    test('houdt de sessie als verwijderen wordt geweigerd', () async {
      backend
        ..on('POST', '/auth/login', tokensJson())
        ..on('GET', '/auth/me', userJson())
        ..fail('POST', '/me/deletion', 409, 'ACCOUNT_DELETION_BLOCKED');
      await auth.login('fatima@example.org', 'wachtwoord');
      await expectLater(auth.deleteAccount('wachtwoord'), throwsA(isA<ApiException>()));
      expect(auth.hasSession, isTrue);
      expect(store.session, isNotNull);
    });
  });
}
