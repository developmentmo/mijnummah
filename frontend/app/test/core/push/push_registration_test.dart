import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/core/api/api_client.dart';
import 'package:mijnummah_app/core/api/response_cache.dart';
import 'package:mijnummah_app/core/push/push_messaging.dart';
import 'package:mijnummah_app/core/push/push_registration.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../helpers/fake_backend.dart';
import '../../helpers/fake_push.dart';

void main() {
  late FakeBackend backend;
  late FakePushMessaging push;
  late PushRegistration registration;

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    backend = FakeBackend()
      ..on('PUT', '/me/devices', null, status: 204)
      ..on('POST', '/me/devices/remove', null, status: 204);
    push = FakePushMessaging();
    final api = ApiClient(
      baseUrl: FakeBackend.baseUrl,
      httpClient: backend.client,
      cache: SharedPreferencesResponseCache(await SharedPreferences.getInstance()),
    );
    registration = PushRegistration(api, push);
  });

  tearDown(() => registration.dispose());

  List<Object?> registered() => backend.requestsTo('PUT', '/me/devices').map((r) => jsonDecode(r.body)).toList();

  test('vraagt bij inloggen geen toestemming en registreert niets zonder toestemming', () async {
    await registration.signedIn();
    expect(push.permissionRequests, 0);
    expect(registered(), isEmpty);
  });

  test('registreert het toestel bij inloggen als er al toestemming is', () async {
    push.current = PushPermission.granted;
    await registration.signedIn();
    expect(registered(), [
      {'token': 'fcm-token-1', 'platform': 'ANDROID'},
    ]);
  });

  test('vraagt toestemming bij inschakelen en registreert daarna', () async {
    await registration.signedIn();
    expect(await registration.enable(), PushPermission.granted);
    expect(push.permissionRequests, 1);
    expect(registered(), hasLength(1));
  });

  test('registreert niets als de gebruiker weigert', () async {
    push.answer = PushPermission.denied;
    await registration.signedIn();
    expect(await registration.enable(), PushPermission.denied);
    expect(registered(), isEmpty);
  });

  test('registreert een nieuw token na een tokenwissel, maar hetzelfde token maar één keer', () async {
    push.current = PushPermission.granted;
    await registration.signedIn();
    await registration.signedIn();
    push.refreshToken('fcm-token-2');
    await Future<void>.delayed(Duration.zero);
    expect(registered(), [
      {'token': 'fcm-token-1', 'platform': 'ANDROID'},
      {'token': 'fcm-token-2', 'platform': 'ANDROID'},
    ]);
  });

  test('meldt het toestel af bij uitloggen en reageert daarna niet meer op tokenwissels', () async {
    push.current = PushPermission.granted;
    await registration.signedIn();
    await registration.signingOut();
    expect(jsonDecode(backend.requestsTo('POST', '/me/devices/remove').single.body), {'token': 'fcm-token-1'});

    push.refreshToken('fcm-token-2');
    await Future<void>.delayed(Duration.zero);
    expect(registered(), hasLength(1));
  });

  test('uitloggen gaat door als afmelden niet lukt', () async {
    push.current = PushPermission.granted;
    await registration.signedIn();
    backend.offline = true;
    await registration.signingOut();
  });

  test('een mislukte registratie wordt bij de volgende keer opnieuw geprobeerd', () async {
    push.current = PushPermission.granted;
    backend.fail('PUT', '/me/devices', 500, 'INTERNAL_ERROR');
    await registration.signedIn();
    backend.on('PUT', '/me/devices', null, status: 204);
    await registration.signedIn();
    expect(backend.requestsTo('PUT', '/me/devices'), hasLength(2));
  });

  test('doet niets als push in deze build niet beschikbaar is', () async {
    final unsupported = PushRegistration(
      ApiClient(
        baseUrl: FakeBackend.baseUrl,
        httpClient: backend.client,
        cache: SharedPreferencesResponseCache(await SharedPreferences.getInstance()),
      ),
      const UnsupportedPushMessaging(),
    );
    await unsupported.signedIn();
    expect(await unsupported.enable(), PushPermission.denied);
    expect(backend.requests, isEmpty);
  });

  group('PushNotice.location', () {
    PushNotice notice(String type, [String id = 'abc']) => PushNotice(data: {'type': type, 'id': id});

    test('wijst naar het juiste scherm', () {
      expect(notice('announcement').location, '/aankondiging/abc');
      expect(notice('event').location, '/activiteit/abc');
      expect(notice('media').location, '/lezing/abc');
    });

    test('negeert onbekende of onvolledige meldingen', () {
      expect(notice('iets anders').location, isNull);
      expect(notice('event', '').location, isNull);
      expect(const PushNotice().location, isNull);
    });
  });
}
