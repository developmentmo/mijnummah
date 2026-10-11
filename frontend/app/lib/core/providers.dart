import 'dart:async';

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
import 'push/push_messaging.dart';
import 'push/push_registration.dart';

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

/// Pushmeldingen van het toestel. Wordt in `main` overschreven als Firebase is ingesteld; anders (en in tests)
/// zijn meldingen niet beschikbaar.
final pushMessagingProvider = Provider<PushMessaging>((ref) => const UnsupportedPushMessaging());

final pushRegistrationProvider = Provider<PushRegistration>((ref) {
  final registration = PushRegistration(ref.watch(apiClientProvider), ref.watch(pushMessagingProvider));
  ref.onDispose(registration.dispose);
  return registration;
});

/// Of meldingen op dit toestel zijn toegestaan; null als deze build geen pushmeldingen ondersteunt.
final pushPermissionProvider = FutureProvider.autoDispose<PushPermission?>((ref) async {
  final push = ref.watch(pushMessagingProvider);
  return push.supported ? push.permission() : null;
});

/// De ingelogde gebruiker, of null voor een anonieme bezoeker.
final authControllerProvider = AsyncNotifierProvider<AuthController, UserProfile?>(AuthController.new);

class AuthController extends AsyncNotifier<UserProfile?> {
  @override
  Future<UserProfile?> build() async {
    final service = ref.watch(authServiceProvider);
    final subscription = service.sessionEnded.listen((_) {
      ref.read(pushRegistrationProvider).signedOut();
      state = const AsyncData(null);
    });
    ref.onDispose(subscription.cancel);
    return _signedIn(await service.restore());
  }

  /// Logt in; fouten (zoals `INVALID_CREDENTIALS`) gaan naar de aanroeper, de huidige toestand blijft staan.
  Future<void> login(String email, String password) async {
    state = AsyncData(_signedIn(await ref.read(authServiceProvider).login(email, password)));
  }

  /// Maakt een account aan en logt in; fouten gaan naar de aanroeper.
  Future<void> register({required String email, required String displayName, required String password}) async {
    final profile = await ref
        .read(authServiceProvider)
        .register(email: email, displayName: displayName, password: password);
    state = AsyncData(_signedIn(profile));
  }

  /// Zet een nieuw wachtwoord met de code uit de e-mail en logt in; fouten gaan naar de aanroeper.
  Future<void> confirmPasswordReset({required String email, required String code, required String newPassword}) async {
    final profile = await ref
        .read(authServiceProvider)
        .confirmPasswordReset(email: email, code: code, newPassword: newPassword);
    state = AsyncData(_signedIn(profile));
  }

  Future<void> logout() async {
    await ref.read(pushRegistrationProvider).signingOut();
    await ref.read(authServiceProvider).logout();
    state = const AsyncData(null);
  }

  /// Verwijdert het account; de backend ruimt ook volgen, voorkeuren, aanmeldingen en toestellen op.
  Future<void> deleteAccount(String password) async {
    await ref.read(authServiceProvider).deleteAccount(password);
    ref.read(pushRegistrationProvider).signedOut();
    state = const AsyncData(null);
  }

  UserProfile? _signedIn(UserProfile? profile) {
    if (profile != null) unawaited(ref.read(pushRegistrationProvider).signedIn());
    return profile;
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
