import 'dart:async';

import 'package:flutter/foundation.dart';

import '../api/api_client.dart';
import '../api/api_exception.dart';
import 'push_messaging.dart';

/// Koppelt het pushtoken van dit toestel aan de ingelogde gebruiker (`PUT /me/devices`) en ontkoppelt het bij
/// uitloggen. Welke meldingen het toestel krijgt, bepaalt de backend: volgen, meldingsvoorkeuren en moskee.
///
/// Toestemming wordt pas gevraagd als de gebruiker iets doet waarvoor meldingen nodig zijn ([enable]), niet bij
/// het starten van de app.
class PushRegistration {
  PushRegistration(this._api, this._push);

  final ApiClient _api;
  final PushMessaging _push;

  bool _signedIn = false;
  String? _registered;
  StreamSubscription<String>? _refreshes;

  bool get supported => _push.supported;

  /// Na inloggen of bij het starten met een bewaarde sessie. Registreert het toestel als er al toestemming is.
  Future<void> signedIn() async {
    _signedIn = true;
    if (!_push.supported) return;
    _refreshes ??= _push.tokenRefreshed.listen(_register);
    if (await _push.permission() == PushPermission.granted) await _register(await _push.token());
  }

  /// Vraagt toestemming en registreert het toestel als die er is.
  Future<PushPermission> enable() async {
    if (!_push.supported) return PushPermission.denied;
    final permission = await _push.requestPermission();
    if (permission == PushPermission.granted && _signedIn) await _register(await _push.token());
    return permission;
  }

  /// Vóór het uitloggen, zolang de sessie nog geldig is: dit toestel krijgt dan geen meldingen meer voor deze
  /// gebruiker. Lukt het niet (bijvoorbeeld offline), dan gaat het uitloggen gewoon door.
  Future<void> signingOut() async {
    final token = _registered;
    _forget();
    if (token == null) return;
    try {
      await _api.post('/me/devices/remove', body: {'token': token});
    } on AppFailure catch (e) {
      debugPrint('Toestel kon niet worden afgemeld voor meldingen: $e');
    }
  }

  /// Als de sessie al weg is (verlopen, ingetrokken of account verwijderd): niets meer naar de server sturen.
  void signedOut() => _forget();

  void _forget() {
    _signedIn = false;
    _registered = null;
    unawaited(_refreshes?.cancel());
    _refreshes = null;
  }

  Future<void> _register(String? token) async {
    final platform = _push.platform;
    if (!_signedIn || token == null || token.isEmpty || platform == null || token == _registered) return;
    try {
      await _api.put('/me/devices', body: {'token': token, 'platform': platform.apiValue});
      if (_signedIn) _registered = token;
    } on AppFailure catch (e) {
      // Bij de volgende start of tokenwissel wordt het opnieuw geprobeerd.
      debugPrint('Toestel kon niet worden aangemeld voor meldingen: $e');
    }
  }

  void dispose() => _forget();
}
