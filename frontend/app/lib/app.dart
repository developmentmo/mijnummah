import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'core/design/design.dart';
import 'core/providers.dart';
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

  @override
  void dispose() {
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
      routerConfig: _router,
    );
  }
}
