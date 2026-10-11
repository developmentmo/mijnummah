import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/core/push/push_messaging.dart';
import 'package:mijnummah_app/features/agenda/event_detail_page.dart';
import 'package:mijnummah_app/features/announcements/announcement_detail_page.dart';
import 'package:mijnummah_app/features/lectures/media_detail_page.dart';

import '../helpers/fake_backend.dart';
import '../helpers/fake_push.dart';
import '../helpers/fixtures.dart';
import '../helpers/test_app.dart';

void main() {
  FakeBackend pushBackend() => standardBackend()
    ..on('PUT', '/me/devices', null, status: 204)
    ..on('POST', '/me/devices/remove', null, status: 204)
    ..on('POST', '/auth/logout', null, status: 204);

  List<Object?> registered(FakeBackend backend) =>
      backend.requestsTo('PUT', '/me/devices').map((r) => jsonDecode(r.body)).toList();

  group('tikken op een melding', () {
    testWidgets('opent de aankondiging, activiteit of lezing', (tester) async {
      final push = FakePushMessaging();
      await pumpApp(tester, push: push);

      push.open(const PushNotice(data: {'type': 'announcement', 'id': 'a1'}));
      await settle(tester);
      expect(find.byType(AnnouncementDetailPage), findsOneWidget);

      push.open(const PushNotice(data: {'type': 'event', 'id': 'e1'}));
      await settle(tester);
      expect(find.byType(EventDetailPage), findsOneWidget);

      push.open(const PushNotice(data: {'type': 'media', 'id': 'm1'}));
      await settle(tester);
      expect(find.byType(MediaDetailPage), findsOneWidget);
    });

    testWidgets('opent het scherm ook als de app door de melding werd gestart', (tester) async {
      final push = FakePushMessaging()..launch = const PushNotice(data: {'type': 'event', 'id': 'e1'});
      await pumpApp(tester, push: push);
      expect(find.byType(EventDetailPage), findsOneWidget);
    });
  });

  testWidgets('een melding terwijl de app open is verschijnt onderin met een knop', (tester) async {
    final push = FakePushMessaging();
    await pumpApp(tester, push: push);
    push.receive(const PushNotice(title: 'Iftar vanavond', data: {'type': 'announcement', 'id': 'a1'}));
    await settle(tester);

    expect(find.text('Iftar vanavond'), findsOneWidget);
    await tester.tap(find.text('Bekijken'));
    await settle(tester);
    expect(find.byType(AnnouncementDetailPage), findsOneWidget);
  });

  testWidgets('volgen vraagt toestemming en meldt het toestel aan', (tester) async {
    late final FakeBackend backend;
    backend = pushBackend()
      ..onRequest('PUT', '/me/follows/$mosqueId', (_) async {
        backend.on('GET', '/me/follows/$mosqueId', followJson());
        return FakeBackendJson.of(followJson());
      });
    final push = FakePushMessaging();
    await pumpApp(tester, backend: backend, tokenStore: signedInStore(backend), push: push, location: '/profiel');
    expect(push.permissionRequests, 0, reason: 'niet vragen bij het openen van de app');

    await tapVisible(tester, find.widgetWithText(FilledButton, 'Volgen'));
    expect(push.permissionRequests, 1);
    expect(registered(backend), [
      {'token': 'fcm-token-1', 'platform': 'ANDROID'},
    ]);
  });

  testWidgets('een voorkeur aanzetten vraagt toestemming als die nog niet is gegeven', (tester) async {
    final backend = pushBackend()
      ..on('GET', '/me/follows/$mosqueId', followJson(media: false))
      ..on('PUT', '/me/follows/$mosqueId/notifications', followJson(media: true));
    final push = FakePushMessaging();
    await pumpApp(tester, backend: backend, tokenStore: signedInStore(backend), push: push, location: '/profiel');
    expect(find.widgetWithText(TextButton, 'Meldingen toestaan'), findsOneWidget);

    await tapVisible(tester, find.widgetWithText(SwitchListTile, 'Nieuwe lezingen'));
    expect(push.permissionRequests, 1);
    expect(registered(backend), hasLength(1));
    expect(find.widgetWithText(TextButton, 'Meldingen toestaan'), findsNothing);
  });

  testWidgets('zegt hoe meldingen weer aan kunnen als de gebruiker ze heeft geweigerd', (tester) async {
    final backend = pushBackend()..on('GET', '/me/follows/$mosqueId', followJson());
    final push = FakePushMessaging(current: PushPermission.denied);
    await pumpApp(tester, backend: backend, tokenStore: signedInStore(backend), push: push, location: '/profiel');
    expect(find.textContaining('Zet ze aan in de instellingen van je toestel'), findsOneWidget);
    expect(registered(backend), isEmpty);
  });

  testWidgets('meldt het toestel aan bij het starten en af bij uitloggen', (tester) async {
    final backend = pushBackend()..on('GET', '/me/follows/$mosqueId', followJson());
    final push = FakePushMessaging(current: PushPermission.granted);
    await pumpApp(tester, backend: backend, tokenStore: signedInStore(backend), push: push, location: '/profiel');
    expect(registered(backend), hasLength(1));

    await tapVisible(tester, find.widgetWithText(OutlinedButton, 'Uitloggen'));
    await tester.tap(find.widgetWithText(FilledButton, 'Uitloggen'));
    await settle(tester);

    final remove = backend.requestsTo('POST', '/me/devices/remove').single;
    expect(jsonDecode(remove.body), {'token': 'fcm-token-1'});
    expect(remove.headers['Authorization'], 'Bearer access-1', reason: 'afmelden gebeurt vóór het uitloggen');
    final logout = backend.requests.indexWhere((r) => r.url.path.endsWith('/auth/logout'));
    expect(backend.requests.indexOf(remove), lessThan(logout));
  });
}
