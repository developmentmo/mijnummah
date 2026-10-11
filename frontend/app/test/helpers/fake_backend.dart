import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

/// Testdubbel voor de backend: beantwoordt verzoeken op methode en pad en houdt bij wat er is gevraagd.
/// Alleen voor tests; de app zelf gebruikt nooit voorbeelddata.
class FakeBackend {
  final _routes = <String, Future<http.Response> Function(http.Request request)>{};
  final requests = <http.Request>[];

  /// Laat alle verzoeken mislukken alsof er geen verbinding is.
  bool offline = false;

  static const baseUrl = 'http://backend.test';

  void on(String method, String path, Object? body, {int status = 200, Map<String, String> headers = const {}}) =>
      _routes['$method $path'] = (_) async => json(body, status: status, headers: headers);

  void onRequest(String method, String path, Future<http.Response> Function(http.Request request) handler) =>
      _routes['$method $path'] = handler;

  /// Een foutrespons in het formaat van de backend.
  void fail(String method, String path, int status, String code, {Map<String, String> headers = const {}}) => on(
    method,
    path,
    {'timestamp': '2026-10-11T10:00:00Z', 'status': status, 'code': code, 'message': code, 'path': path},
    status: status,
    headers: headers,
  );

  static http.Response json(Object? body, {int status = 200, Map<String, String> headers = const {}}) =>
      http.Response.bytes(
        body == null ? const [] : utf8.encode(jsonEncode(body)),
        status,
        headers: {'content-type': 'application/json', ...headers},
      );

  List<http.Request> requestsTo(String method, String path) =>
      requests.where((r) => r.method == method && r.url.path == '/api/v1$path').toList();

  http.Client get client => MockClient((request) async {
    requests.add(request);
    if (offline) throw http.ClientException('offline', request.url);
    final path = request.url.path.replaceFirst('/api/v1', '');
    final handler = _routes['${request.method} $path'];
    if (handler == null) {
      return json({
        'status': 404,
        'code': 'NOT_FOUND',
        'message': 'No fake route for ${request.method} $path',
        'path': path,
      }, status: 404);
    }
    return handler(request);
  });
}

/// Korte schrijfwijze voor een JSON-antwoord in een `onRequest`-handler.
abstract final class FakeBackendJson {
  static http.Response of(Object? body, {int status = 200}) => FakeBackend.json(body, status: status);
}
