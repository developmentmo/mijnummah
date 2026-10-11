import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import '../data/models.dart';
import '../data/mosque_repository.dart';
import 'api/api_client.dart';
import 'api/response_cache.dart';
import 'auth/auth_service.dart';
import 'auth/token_store.dart';
import 'config/app_config.dart';

/// Wordt in `main` overschreven met de geladen voorkeuren.
final sharedPreferencesProvider = Provider<SharedPreferences>(
  (ref) => throw StateError('sharedPreferencesProvider moet in main worden overschreven'),
);

final appConfigProvider = Provider<AppConfig>((ref) => AppConfig.fromEnvironment());

final httpClientProvider = Provider<http.Client>((ref) {
  final client = http.Client();
  ref.onDispose(client.close);
  return client;
});

final tokenStoreProvider = Provider<TokenStore>((ref) => SecureTokenStore());

final apiClientProvider = Provider<ApiClient>(
  (ref) => ApiClient(
    baseUrl: ref.watch(appConfigProvider).apiBaseUrl,
    httpClient: ref.watch(httpClientProvider),
    cache: SharedPreferencesResponseCache(ref.watch(sharedPreferencesProvider)),
  ),
);

final authServiceProvider = Provider<AuthService>((ref) {
  final service = AuthService(ref.watch(apiClientProvider), ref.watch(tokenStoreProvider));
  ref.onDispose(service.dispose);
  return service;
});

/// De ingelogde gebruiker, of null voor een anonieme bezoeker.
final authControllerProvider = AsyncNotifierProvider<AuthController, UserProfile?>(AuthController.new);

class AuthController extends AsyncNotifier<UserProfile?> {
  @override
  Future<UserProfile?> build() async {
    final service = ref.watch(authServiceProvider);
    final subscription = service.sessionEnded.listen((_) => state = const AsyncData(null));
    ref.onDispose(subscription.cancel);
    return service.restore();
  }

  /// Logt in; fouten (zoals `INVALID_CREDENTIALS`) gaan naar de aanroeper, de huidige toestand blijft staan.
  Future<void> login(String email, String password) async {
    final profile = await ref.read(authServiceProvider).login(email, password);
    state = AsyncData(profile);
  }

  Future<void> logout() async {
    await ref.read(authServiceProvider).logout();
    state = const AsyncData(null);
  }
}

/// Id van de ingelogde gebruiker. Lijsten die per gebruiker verschillen (zoals `registeredByMe`) luisteren
/// hiernaar, zodat ze na in- of uitloggen opnieuw worden opgehaald.
final currentUserIdProvider = Provider<String?>((ref) => ref.watch(authControllerProvider).value?.id);

final mosqueRepositoryProvider = Provider<MosqueRepository>(
  (ref) => MosqueRepository(ref.watch(apiClientProvider), ref.watch(appConfigProvider).mosqueSlug),
);

final meRepositoryProvider = Provider<MeRepository>((ref) => MeRepository(ref.watch(apiClientProvider)));

/// Profiel van de moskee (naam, beschrijving, adres, contact).
final mosqueProvider = FutureProvider<Fetched<Mosque>>((ref) => ref.watch(mosqueRepositoryProvider).mosque());

/// Licht, donker of de instelling van het toestel; bewaard tussen app-starts.
final themeModeProvider = NotifierProvider<ThemeModeController, ThemeMode>(ThemeModeController.new);

class ThemeModeController extends Notifier<ThemeMode> {
  static const _key = 'settings.themeMode';

  @override
  ThemeMode build() {
    final saved = ref.watch(sharedPreferencesProvider).getString(_key);
    return ThemeMode.values.firstWhere((mode) => mode.name == saved, orElse: () => ThemeMode.system);
  }

  Future<void> set(ThemeMode mode) async {
    state = mode;
    await ref.read(sharedPreferencesProvider).setString(_key, mode.name);
  }
}
