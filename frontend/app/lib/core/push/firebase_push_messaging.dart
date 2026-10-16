import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';

import 'push_messaging.dart';

/// Firebase-instellingen per build, meegegeven met `--dart-define`. Ze staan niet in de repository: elke moskee-app
/// is een eigen app in het Firebase-project van het platform (D-049).
///
/// | Instelling | |
/// |---|---|
/// | `FIREBASE_PROJECT_ID` | Project-ID |
/// | `FIREBASE_MESSAGING_SENDER_ID` | Sender-ID (projectnummer) |
/// | `FIREBASE_ANDROID_API_KEY`, `FIREBASE_ANDROID_APP_ID` | Uit de Android-app in de Firebase-console |
/// | `FIREBASE_IOS_API_KEY`, `FIREBASE_IOS_APP_ID` | Uit de iOS-app in de Firebase-console |
abstract final class FirebaseConfig {
  static const _projectId = String.fromEnvironment('FIREBASE_PROJECT_ID');
  static const _senderId = String.fromEnvironment('FIREBASE_MESSAGING_SENDER_ID');
  static const _androidApiKey = String.fromEnvironment('FIREBASE_ANDROID_API_KEY');
  static const _androidAppId = String.fromEnvironment('FIREBASE_ANDROID_APP_ID');
  static const _iosApiKey = String.fromEnvironment('FIREBASE_IOS_API_KEY');
  static const _iosAppId = String.fromEnvironment('FIREBASE_IOS_APP_ID');

  /// De instellingen voor dit platform, of null als ze ontbreken of het platform geen push ondersteunt.
  static FirebaseOptions? forCurrentPlatform() {
    if (kIsWeb || _projectId.isEmpty || _senderId.isEmpty) return null;
    final (apiKey, appId) = switch (defaultTargetPlatform) {
      TargetPlatform.android => (_androidApiKey, _androidAppId),
      TargetPlatform.iOS => (_iosApiKey, _iosAppId),
      _ => ('', ''),
    };
    if (apiKey.isEmpty || appId.isEmpty) return null;
    return FirebaseOptions(
      apiKey: apiKey,
      appId: appId,
      messagingSenderId: _senderId,
      projectId: _projectId,
      iosBundleId: defaultTargetPlatform == TargetPlatform.iOS ? 'nl.mijnummah.noeroelislam' : null,
    );
  }
}

/// Pushmeldingen via Firebase Cloud Messaging.
class FirebasePushMessaging implements PushMessaging {
  FirebasePushMessaging._(this._messaging, this.platform);

  /// Start Firebase. Zonder instellingen, of als starten mislukt, zijn meldingen niet beschikbaar; de rest van de
  /// app werkt gewoon.
  static Future<PushMessaging> initialize() async {
    final options = FirebaseConfig.forCurrentPlatform();
    if (options == null) return const UnsupportedPushMessaging();
    try {
      await Firebase.initializeApp(options: options);
    } on Exception catch (e) {
      debugPrint('Firebase kon niet starten; pushmeldingen staan uit: $e');
      return const UnsupportedPushMessaging();
    }
    final platform = defaultTargetPlatform == TargetPlatform.iOS ? PushPlatform.ios : PushPlatform.android;
    return FirebasePushMessaging._(FirebaseMessaging.instance, platform);
  }

  final FirebaseMessaging _messaging;

  @override
  final PushPlatform platform;

  @override
  bool get supported => true;

  @override
  Future<PushPermission> permission() async => _map((await _messaging.getNotificationSettings()).authorizationStatus);

  @override
  Future<PushPermission> requestPermission() async => _map((await _messaging.requestPermission()).authorizationStatus);

  static PushPermission _map(AuthorizationStatus status) => switch (status) {
    AuthorizationStatus.authorized || AuthorizationStatus.provisional => PushPermission.granted,
    AuthorizationStatus.denied || AuthorizationStatus.deniedPermanently => PushPermission.denied,
    AuthorizationStatus.notDetermined => PushPermission.notDetermined,
  };

  @override
  Future<String?> token() async {
    try {
      // Automatisch een token maken staat uit in het manifest en Info.plist, zodat er vóór toestemming geen token
      // bestaat. Deze methode wordt alleen aangeroepen als de gebruiker toestemming heeft gegeven.
      await _messaging.setAutoInitEnabled(true);
      return await _messaging.getToken();
    } on FirebaseException catch (e) {
      // Op iOS is er pas een token als APNs het toestel heeft geregistreerd; `tokenRefreshed` meldt het later.
      debugPrint('Geen pushtoken beschikbaar: ${e.code}');
      return null;
    }
  }

  @override
  Stream<String> get tokenRefreshed => _messaging.onTokenRefresh;

  @override
  Stream<PushNotice> get received => FirebaseMessaging.onMessage.map(_notice);

  @override
  Stream<PushNotice> get opened => FirebaseMessaging.onMessageOpenedApp.map(_notice);

  @override
  Future<PushNotice?> launchNotice() async {
    final message = await _messaging.getInitialMessage();
    return message == null ? null : _notice(message);
  }

  static PushNotice _notice(RemoteMessage message) => PushNotice(
    title: message.notification?.title,
    body: message.notification?.body,
    data: {for (final entry in message.data.entries) entry.key: '${entry.value}'},
  );
}
