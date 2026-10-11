import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import 'api_exception.dart';
import 'response_cache.dart';

/// Gegevens plus, als ze uit de offline-cache komen, het moment waarop ze zijn opgehaald.
class Fetched<T> {
  const Fetched(this.data, {this.cachedAt});

  final T data;

  /// Gezet als de server niet bereikbaar was en de laatst bekende gegevens worden getoond.
  final DateTime? cachedAt;

  bool get isFromCache => cachedAt != null;

  Fetched<R> map<R>(R Function(T data) convert) => Fetched(convert(data), cachedAt: cachedAt);
}

/// Levert het access token voor verzoeken en ververst de sessie als de server `401` geeft.
abstract interface class AccessTokenSource {
  /// Het huidige access token, of null zonder sessie.
  Future<String?> currentAccessToken();

  /// Ververst de sessie na een `401`. Geeft het nieuwe access token, of null als de sessie is beëindigd.
  Future<String?> refreshAccessToken();
}

/// Praat met de Mijn Ummah-backend (`/api/v1`). Zet fouten om naar [ApiException] of [NetworkException].
class ApiClient {
  ApiClient({
    required String baseUrl,
    required http.Client httpClient,
    required ResponseCache cache,
    this.timeout = const Duration(seconds: 20),
  }) : _baseUrl = baseUrl.endsWith('/') ? baseUrl.substring(0, baseUrl.length - 1) : baseUrl,
       _http = httpClient,
       _cache = cache; // ignore: prefer_initializing_formals

  final String _baseUrl;
  final http.Client _http;
  final ResponseCache _cache;
  final Duration timeout;

  /// Wordt gezet door de authenticatie; zonder bron gaan alle verzoeken anoniem.
  AccessTokenSource? tokens;

  Uri uri(String path, [Map<String, Object?>? query]) {
    final params = <String, String>{
      for (final entry in (query ?? const <String, Object?>{}).entries)
        if (entry.value != null && '${entry.value}'.isNotEmpty) entry.key: '${entry.value}',
    };
    return Uri.parse('$_baseUrl/api/v1$path').replace(queryParameters: params.isEmpty ? null : params);
  }

  /// GET op publieke content. Bewaart het antwoord en valt zonder verbinding terug op de laatst bekende versie.
  Future<Fetched<Object?>> getCached(String path, {Map<String, Object?>? query}) async {
    final target = uri(path, query);
    final key = target.toString();
    try {
      final body = await _send('GET', target);
      unawaited(_cache.write(key, body));
      return Fetched(_decode(body));
    } on NetworkException {
      final cached = await _cache.read(key);
      if (cached == null) rethrow;
      return Fetched(_decode(cached.body), cachedAt: cached.savedAt);
    }
  }

  Future<Object?> get(String path, {Map<String, Object?>? query}) async =>
      _decode(await _send('GET', uri(path, query)));

  Future<Object?> post(String path, {Object? body, bool authenticated = true}) async =>
      _decode(await _send('POST', uri(path), body: body, authenticated: authenticated));

  Future<Object?> put(String path, {Object? body}) async => _decode(await _send('PUT', uri(path), body: body));

  Future<void> delete(String path) async => _send('DELETE', uri(path));

  /// Verwijdert de offline-cache, bijvoorbeeld bij uitloggen (de cache kan `registeredByMe` bevatten).
  Future<void> clearCache() => _cache.clear();

  Future<String> _send(String method, Uri target, {Object? body, bool authenticated = true}) async {
    final source = authenticated ? tokens : null;
    final token = await source?.currentAccessToken();
    var response = await _request(method, target, body, token);
    if (response.statusCode == 401 && token != null && source != null) {
      // Verlopen of ingetrokken token: één keer verversen en opnieuw proberen. Lukt verversen niet, dan
      // anoniem opnieuw, zodat publieke content zichtbaar blijft na het verlopen van een sessie.
      final refreshed = await source.refreshAccessToken();
      response = await _request(method, target, body, refreshed);
    }
    final text = utf8.decode(response.bodyBytes, allowMalformed: true);
    if (response.statusCode >= 400) throw _error(response, text);
    return text;
  }

  Future<http.Response> _request(String method, Uri target, Object? body, String? token) async {
    final request = http.Request(method, target)
      ..headers['Accept'] = 'application/json'
      ..followRedirects = false;
    if (token != null) request.headers['Authorization'] = 'Bearer $token';
    if (body != null) {
      request.headers['Content-Type'] = 'application/json';
      request.body = jsonEncode(body);
    }
    try {
      final streamed = await _http.send(request).timeout(timeout);
      return await http.Response.fromStream(streamed).timeout(timeout);
    } on http.ClientException catch (e) {
      throw NetworkException(e);
    } on TimeoutException catch (e) {
      throw NetworkException(e);
    }
  }

  ApiException _error(http.Response response, String text) {
    String code = 'HTTP_${response.statusCode}';
    String? message;
    try {
      final json = jsonDecode(text);
      if (json is Map<String, dynamic>) {
        code = json['code'] as String? ?? code;
        message = json['message'] as String?;
      }
    } on FormatException {
      // Geen JSON (bijvoorbeeld een proxyfout); de statuscode volstaat.
    }
    final retryAfter = int.tryParse(response.headers['retry-after'] ?? '');
    return ApiException(
      status: response.statusCode,
      code: code,
      message: message,
      retryAfter: retryAfter == null ? null : Duration(seconds: retryAfter),
    );
  }

  Object? _decode(String body) {
    if (body.isEmpty) return null;
    try {
      return jsonDecode(body);
    } on FormatException catch (e) {
      throw NetworkException(e);
    }
  }
}
