import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models.dart';

/// Of de ingelogde gebruiker de moskee volgt, met de meldingsvoorkeuren. Null voor anonieme bezoekers en voor
/// gebruikers die de moskee niet volgen.
final followStatusProvider = FutureProvider.autoDispose<FollowStatus?>((ref) async {
  if (ref.watch(currentUserIdProvider) == null) return null;
  final mosque = await ref.watch(mosqueProvider.future);
  return ref.watch(meRepositoryProvider).followStatus(mosque.data.id);
});
