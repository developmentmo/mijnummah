import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'core/design/design.dart';
import 'features/design_preview/design_preview_page.dart';

class NoeroelIslamApp extends StatefulWidget {
  const NoeroelIslamApp({super.key});

  @override
  State<NoeroelIslamApp> createState() => _NoeroelIslamAppState();
}

class _NoeroelIslamAppState extends State<NoeroelIslamApp> {
  ThemeMode _themeMode = ThemeMode.system;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Noeroel Islam',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      darkTheme: AppTheme.dark,
      themeMode: _themeMode,
      locale: const Locale('nl', 'NL'),
      supportedLocales: const [Locale('nl', 'NL')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      // Fase 1: alleen de interne designpreview. De echte navigatie volgt in fase 2.
      home: DesignPreviewPage(themeMode: _themeMode, onThemeModeChanged: (mode) => setState(() => _themeMode = mode)),
    );
  }
}
