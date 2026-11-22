import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../data/models.dart';

/// De gevolgde moskeeën van de ingelogde gebruiker, favorieten eerst; leeg voor een anonieme bezoeker.
final myFollowsProvider = FutureProvider.autoDispose<List<FollowStatus>>((ref) async {
  if (ref.watch(currentUserIdProvider) == null) return const [];
  return ref.watch(meRepositoryProvider).follows();
});

/// Of de gebruiker een moskee volgt (op id), met de meldingsvoorkeuren; null als hij haar niet volgt.
final followOfProvider = Provider.autoDispose.family<AsyncValue<FollowStatus?>, String>(
  (ref, mosqueId) => ref
      .watch(myFollowsProvider)
      .whenData((follows) => follows.where((follow) => follow.mosqueId == mosqueId).firstOrNull),
);

/// Volgen, ontvolgen en favoriet maken. Elke actie haalt daarna de lijst gevolgde moskeeën opnieuw op.
extension FollowActions on WidgetRef {
  Future<void> follow(String mosqueId) async {
    await read(meRepositoryProvider).follow(mosqueId);
    invalidate(myFollowsProvider);
  }

  Future<void> unfollow(String mosqueId) async {
    await read(meRepositoryProvider).unfollow(mosqueId);
    invalidate(myFollowsProvider);
  }

  Future<void> setFavoriteMosque(String mosqueId, bool favorite) async {
    await read(meRepositoryProvider).setFavorite(mosqueId, favorite);
    invalidate(myFollowsProvider);
  }

  Future<void> updateNotification(String mosqueId, NotificationTopic topic, bool enabled) async {
    await read(meRepositoryProvider).updateNotification(mosqueId, topic, enabled);
    invalidate(myFollowsProvider);
    await read(myFollowsProvider.future);
  }
}

/// IDs van de favoriete video's; leeg voor een anonieme bezoeker.
final favoriteMediaIdsProvider = AsyncNotifierProvider.autoDispose<FavoriteMediaController, Set<String>>(
  FavoriteMediaController.new,
);

class FavoriteMediaController extends AsyncNotifier<Set<String>> {
  @override
  Future<Set<String>> build() async {
    if (ref.watch(currentUserIdProvider) == null) return const {};
    return ref.watch(meRepositoryProvider).favoriteMediaIds();
  }

  /// Bewaart of verwijdert een video. Het hartje verandert meteen; mislukt het opslaan, dan gaat het terug en
  /// krijgt de aanroeper de fout.
  Future<void> set(String mediaId, bool favorite) async {
    final before = state.value ?? const <String>{};
    state = AsyncData(
      {...before}
        ..removeWhere((id) => id == mediaId)
        ..addAll([if (favorite) mediaId]),
    );
    try {
      await ref.read(meRepositoryProvider).setFavoriteMedia(mediaId, favorite);
      ref.invalidate(favoriteMediaProvider);
    } catch (_) {
      if (ref.mounted) state = AsyncData(before);
      rethrow;
    }
  }
}

/// De favoriete video's die nu te bekijken zijn.
final favoriteMediaProvider = FutureProvider.autoDispose<PageResult<MediaItem>>((ref) async {
  if (ref.watch(currentUserIdProvider) == null) return _emptyPage();
  return ref.watch(meRepositoryProvider).favoriteMedia(size: 50);
});

/// Meldingen van gevolgde moskeeën (laatste 30 dagen).
final inboxProvider = FutureProvider.autoDispose<PageResult<InboxItem>>((ref) async {
  if (ref.watch(currentUserIdProvider) == null) return _emptyPage();
  return ref.watch(meRepositoryProvider).notifications(size: 50);
});

PageResult<T> _emptyPage<T>() => PageResult<T>(items: const [], page: 0, totalItems: 0, totalPages: 0);
