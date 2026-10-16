import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'app.dart';
import 'core/providers.dart';
import 'core/push/firebase_push_messaging.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
  final push = await FirebasePushMessaging.initialize();
  runApp(
    ProviderScope(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs), pushMessagingProvider.overrideWithValue(push)],
      // Geen automatische herhaling bij fouten: het scherm toont direct een melding met "Opnieuw proberen".
      retry: (retryCount, error) => null,
      child: const MosqueApp(),
    ),
  );
}
