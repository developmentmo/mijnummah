import 'dart:async';

import 'package:mijnummah_app/core/push/push_messaging.dart';

/// Testdubbel voor pushmeldingen van het toestel: toestemming en token zijn in te stellen, meldingen zijn na te
/// bootsen.
class FakePushMessaging implements PushMessaging {
  FakePushMessaging({this.current = PushPermission.notDetermined, this.answer = PushPermission.granted});

  /// De toestemming zoals het toestel die nu kent.
  PushPermission current;

  /// Wat de gebruiker antwoordt op de systeemvraag.
  PushPermission answer;

  String? deviceToken = 'fcm-token-1';
  int permissionRequests = 0;
  PushNotice? launch;

  final _refreshes = StreamController<String>.broadcast();
  final _received = StreamController<PushNotice>.broadcast();
  final _opened = StreamController<PushNotice>.broadcast();

  void refreshToken(String token) {
    deviceToken = token;
    _refreshes.add(token);
  }

  void receive(PushNotice notice) => _received.add(notice);

  void open(PushNotice notice) => _opened.add(notice);

  @override
  bool get supported => true;

  @override
  PushPlatform get platform => PushPlatform.android;

  @override
  Future<PushPermission> permission() async => current;

  @override
  Future<PushPermission> requestPermission() async {
    permissionRequests++;
    return current = answer;
  }

  @override
  Future<String?> token() async => deviceToken;

  @override
  Stream<String> get tokenRefreshed => _refreshes.stream;

  @override
  Stream<PushNotice> get received => _received.stream;

  @override
  Stream<PushNotice> get opened => _opened.stream;

  @override
  Future<PushNotice?> launchNotice() async => launch;
}
