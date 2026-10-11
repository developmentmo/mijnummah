import 'dart:async';

/// Toestemming voor meldingen op dit toestel.
enum PushPermission {
  granted,
  denied,

  /// Nog niet gevraagd.
  notDetermined,
}

/// Het platform zoals de backend het kent (`PUT /me/devices`).
enum PushPlatform {
  android('ANDROID'),
  ios('IOS');

  const PushPlatform(this.apiValue);

  final String apiValue;
}

/// Een ontvangen melding. [data] komt van de backend: `type` (`announcement`, `event` of `media`), `id` en
/// `mosqueId`.
class PushNotice {
  const PushNotice({this.title, this.body, this.data = const {}});

  final String? title;
  final String? body;
  final Map<String, String> data;

  /// Het scherm dat bij deze melding hoort, of null als de melding nergens naartoe wijst.
  String? get location {
    final id = data['id'];
    if (id == null || id.isEmpty) return null;
    final encoded = Uri.encodeComponent(id);
    return switch (data['type']) {
      'announcement' => '/aankondiging/$encoded',
      'event' => '/activiteit/$encoded',
      'media' => '/lezing/$encoded',
      _ => null,
    };
  }
}

/// Pushmeldingen van het toestel (Firebase Cloud Messaging; op iOS via APNs). Achter een interface zodat tests en
/// builds zonder Firebase-instellingen een vervanger gebruiken.
abstract interface class PushMessaging {
  /// Of pushmeldingen in deze build werken. Zonder Firebase-instellingen of op een niet-ondersteund platform is
  /// dit false en doet de rest niets.
  bool get supported;

  PushPlatform? get platform;

  Future<PushPermission> permission();

  /// Vraagt de gebruiker om toestemming (op Android 13+ en iOS een systeemvraag).
  Future<PushPermission> requestPermission();

  /// Het token waarmee de backend dit toestel bereikt, of null als het (nog) niet beschikbaar is.
  Future<String?> token();

  Stream<String> get tokenRefreshed;

  /// Meldingen die binnenkomen terwijl de app open is; het systeem toont die niet zelf.
  Stream<PushNotice> get received;

  /// Meldingen waarop de gebruiker tikte terwijl de app op de achtergrond stond.
  Stream<PushNotice> get opened;

  /// De melding waarmee de app werd gestart, als de gebruiker erop tikte terwijl de app niet draaide.
  Future<PushNotice?> launchNotice();
}

/// Voor builds zonder Firebase-instellingen, het web en tests: meldingen zijn niet beschikbaar.
class UnsupportedPushMessaging implements PushMessaging {
  const UnsupportedPushMessaging();

  @override
  bool get supported => false;

  @override
  PushPlatform? get platform => null;

  @override
  Future<PushPermission> permission() async => PushPermission.denied;

  @override
  Future<PushPermission> requestPermission() async => PushPermission.denied;

  @override
  Future<String?> token() async => null;

  @override
  Stream<String> get tokenRefreshed => const Stream.empty();

  @override
  Stream<PushNotice> get received => const Stream.empty();

  @override
  Stream<PushNotice> get opened => const Stream.empty();

  @override
  Future<PushNotice?> launchNotice() async => null;
}
