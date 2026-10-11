import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'core/design/design.dart';
import 'core/providers.dart';
import 'core/push/push_messaging.dart';
import 'core/routing/app_router.dart';

class NoeroelIslamApp extends ConsumerStatefulWidget {
  const NoeroelIslamApp({super.key, this.initialLocation = '/'});

  /// Alleen voor tests: het scherm waarmee de app start.
  final String initialLocation;

  @override
  ConsumerState<NoeroelIslamApp> createState() => _NoeroelIslamAppState();
}

class _NoeroelIslamAppState extends ConsumerState<NoeroelIslamApp> {
  late final GoRouter _router = buildRouter(initialLocation: widget.initialLocation);
  final _messenger = GlobalKey<ScaffoldMessengerState>();
  final _subscriptions = <StreamSubscription<PushNotice>>[];

  @override
  void initState() {
    super.initState();
    final push = ref.read(pushMessagingProvider);
    if (!push.supported) return;
    _subscriptions
      ..add(push.opened.listen(_open))
      ..add(push.received.listen(_show));
    unawaited(push.launchNotice().then((notice) => notice == null ? null : _open(notice)));
  }

  /// De gebruiker tikte op een melding: open het bijbehorende scherm.
  void _open(PushNotice notice) {
    final location = notice.location;
    if (location != null && mounted) unawaited(_router.push(location));
  }

  /// Een melding terwijl de app open is: het systeem toont die niet, dus de app laat hem zelf kort zien.
  void _show(PushNotice notice) {
    final text = notice.title ?? notice.body;
    if (text == null) return;
    final location = notice.location;
    _messenger.currentState?.showSnackBar(
      SnackBar(
        content: Text(text),
        action: location == null
            ? null
            : SnackBarAction(label: 'Bekijken', onPressed: () => unawaited(_router.push(location))),
      ),
    );
  }

  @override
  void dispose() {
    for (final subscription in _subscriptions) {
      unawaited(subscription.cancel());
    }
    _router.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Noeroel Islam',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      darkTheme: AppTheme.dark,
      themeMode: ref.watch(themeModeProvider),
      locale: const Locale('nl', 'NL'),
      supportedLocales: const [Locale('nl', 'NL')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      scaffoldMessengerKey: _messenger,
      routerConfig: _router,
    );
  }
}
