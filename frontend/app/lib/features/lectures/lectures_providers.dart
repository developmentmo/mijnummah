import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/providers.dart';
import '../../data/models.dart';
import '../../data/paged.dart';

/// Zoekopdracht in de bibliotheek: tekst (titel en spreker) en categorie.
typedef LibraryQuery = ({String text, MediaCategory? category});

/// De mediabibliotheek, nieuwste eerst, met "meer laden".
final libraryProvider = AsyncNotifierProvider.autoDispose.family<LibraryController, Paged<MediaItem>, LibraryQuery>(
  LibraryController.new,
);

class LibraryController extends AsyncNotifier<Paged<MediaItem>> {
  LibraryController(this.query);

  final LibraryQuery query;

  @override
  Future<Paged<MediaItem>> build() async {
    ref.watch(currentUserIdProvider);
    return Paged.first(await ref.watch(mosqueRepositoryProvider).media(query: query.text, category: query.category));
  }

  Future<void> loadMore() async {
    final current = state.value;
    if (current == null || !current.hasMore || current.loadingMore) return;
    state = AsyncData(current.withLoadingMore(loading: true));
    try {
      final next = await ref
          .read(mosqueRepositoryProvider)
          .media(query: query.text, category: query.category, page: current.nextPage);
      if (ref.mounted) state = AsyncData(current.append(next));
    } on AppFailure {
      if (ref.mounted) state = AsyncData(current.withLoadingMore(loading: false, failed: true));
    }
  }
}

/// Recent toegevoegde video's voor de startpagina.
final latestMediaProvider = FutureProvider.autoDispose<Fetched<PageResult<MediaItem>>>((ref) {
  ref.watch(currentUserIdProvider);
  return ref.watch(mosqueRepositoryProvider).media(size: 6);
});

/// Recente lezingen (categorie Lezing) voor de startpagina.
final latestLecturesProvider = FutureProvider.autoDispose<Fetched<PageResult<MediaItem>>>((ref) {
  ref.watch(currentUserIdProvider);
  return ref.watch(mosqueRepositoryProvider).media(category: MediaCategory.lecture, size: 4);
});

final mediaItemProvider = FutureProvider.autoDispose.family<Fetched<MediaItem>, String>((ref, id) {
  ref.watch(currentUserIdProvider);
  return ref.watch(mosqueRepositoryProvider).mediaItem(id);
});

final relatedMediaProvider = FutureProvider.autoDispose.family<Fetched<List<MediaItem>>, String>((ref, id) {
  ref.watch(currentUserIdProvider);
  return ref.watch(mosqueRepositoryProvider).relatedMedia(id);
});
