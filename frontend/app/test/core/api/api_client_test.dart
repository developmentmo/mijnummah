import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/core/api/api_client.dart';
import 'package:mijnummah_app/core/api/api_exception.dart';
import 'package:mijnummah_app/core/api/response_cache.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../helpers/fake_backend.dart';

class _StaticTokens implements AccessTokenSource {
  _StaticTokens(this.token, {this.refreshed});

  String? token;
  final String? refreshed;
  int refreshCalls = 0;

  @override
  Future<String?> currentAccessToken() async => token;

  @override
  Future<String?> refreshAccessToken() async {
    refreshCalls++;
    token = refreshed;
    return refreshed;
  }
}

void main() {
  late FakeBackend backend;
  late ApiClient client;

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    backend = FakeBackend();
    client = ApiClient(
      baseUrl: '${FakeBackend.baseUrl}/',
      httpClient: backend.client,
      cache: SharedPreferencesResponseCache(await SharedPreferences.getInstance()),
    );
  });

  test('bouwt URL\'s met /api/v1 en laat lege parameters weg', () {
    expect(
      client.uri('/public/mosques', {'q': '', 'category': null, 'page': 0}).toString(),
      'http://backend.test/api/v1/public/mosques?page=0',
    );
  });

  test('zet een foutrespons om naar de code van de backend', () async {
    backend.fail('POST', '/auth/login', 429, 'TOO_MANY_LOGIN_ATTEMPTS', headers: {'retry-after': '120'});
    final error = await client.post('/auth/login', body: {}).then<Object?>((_) => null, onError: (Object e) => e);
    expect(error, isA<ApiException>());
    error as ApiException;
    expect(error.status, 429);
    expect(error.code, 'TOO_MANY_LOGIN_ATTEMPTS');
    expect(error.retryAfter, const Duration(seconds: 120));
  });

  test('geeft een algemene code als de fout geen JSON is', () async {
    backend.onRequest('GET', '/x', (_) async => FakeBackend.json(null, status: 502));
    await expectLater(client.get('/x'), throwsA(isA<ApiException>().having((e) => e.code, 'code', 'HTTP_502')));
  });

  test('meldt een netwerkfout als NetworkException', () async {
    backend.offline = true;
    await expectLater(client.get('/x'), throwsA(isA<NetworkException>()));
  });

  test('valt zonder verbinding terug op het laatst opgehaalde antwoord', () async {
    backend.on('GET', '/public/mosques/a', {'name': 'A'});
    final online = await client.getCached('/public/mosques/a');
    expect(online.isFromCache, isFalse);

    backend.offline = true;
    final offline = await client.getCached('/public/mosques/a');
    expect(offline.isFromCache, isTrue);
    expect(offline.data, {'name': 'A'});
  });

  test('zonder verbinding en zonder cache volgt een NetworkException', () async {
    backend.offline = true;
    await expectLater(client.getCached('/public/mosques/b'), throwsA(isA<NetworkException>()));
  });

  test('stuurt het access token mee en ververst één keer na 401', () async {
    final tokens = _StaticTokens('old', refreshed: 'new');
    client.tokens = tokens;
    backend.onRequest('GET', '/me/follows', (request) async {
      return request.headers['Authorization'] == 'Bearer new'
          ? FakeBackend.json([])
          : FakeBackend.json({'code': 'UNAUTHORIZED', 'status': 401}, status: 401);
    });
    expect(await client.get('/me/follows'), isEmpty);
    expect(tokens.refreshCalls, 1);
    expect(backend.requests.map((r) => r.headers['Authorization']), ['Bearer old', 'Bearer new']);
  });

  test('probeert het anoniem opnieuw als verversen niet lukt', () async {
    client.tokens = _StaticTokens('old');
    backend.onRequest('GET', '/public/mosques/a', (request) async {
      return request.headers.containsKey('Authorization')
          ? FakeBackend.json({'code': 'UNAUTHORIZED', 'status': 401}, status: 401)
          : FakeBackend.json({'name': 'A'});
    });
    final result = await client.getCached('/public/mosques/a');
    expect(result.data, {'name': 'A'});
  });

  test('stuurt geen token mee bij verzoeken zonder authenticatie', () async {
    client.tokens = _StaticTokens('secret');
    backend.on('POST', '/auth/login', {});
    await client.post('/auth/login', body: {}, authenticated: false);
    expect(backend.requests.single.headers.containsKey('Authorization'), isFalse);
  });
}
