import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/providers.dart';
import '../../data/models.dart';
import '../../data/paged.dart';
import '../mosques/mosque_filter.dart';

/// Zoekopdracht in de bibliotheek: tekst (titel en spreker), categorie en moskee.
typedef LibraryQuery = ({String text, MediaCategory? category, MosqueFilter mosque});

/// De videobibliotheek van alle (of de gekozen) moskeeën, nieuwste eerst, met "meer laden".
final libraryProvider = AsyncNotifierProvider.autoDispose.family<LibraryController, Paged<MediaItem>, LibraryQuery>(
  LibraryController.new,
);

class LibraryController extends AsyncNotifier<Paged<MediaItem>> {
  LibraryController(this.query);

  final LibraryQuery query;
  List<String>? _mosques;

  @override
  Future<Paged<MediaItem>> build() async {
    ref.watch(currentUserIdProvider);
    _mosques = await ref.watch(mosqueSlugsProvider(query.mosque).future);
    if (_mosques case []) return Paged.first(emptyPage<MediaItem>());
    return Paged.first(await _page(0));
  }

  Future<Fetched<PageResult<MediaItem>>> _page(int page) => ref
      .read(contentRepositoryProvider)
      .media(mosques: _mosques, query: query.text, category: query.category, page: page);

  Future<void> loadMore() async {
    final current = state.value;
    if (current == null || !current.hasMore || current.loadingMore) return;
    state = AsyncData(current.withLoadingMore(loading: true));
    try {
      final next = await _page(current.nextPage);
      if (ref.mounted) state = AsyncData(current.append(next));
    } on AppFailure {
      if (ref.mounted) state = AsyncData(current.withLoadingMore(loading: false, failed: true));
    }
  }
}

/// Recent toegevoegde video's (startpagina, moskeeprofiel).
final latestMediaProvider = FutureProvider.autoDispose.family<Fetched<PageResult<MediaItem>>, MosqueFilter>((
  ref,
  filter,
) async {
  ref.watch(currentUserIdProvider);
  final mosques = await ref.watch(mosqueSlugsProvider(filter).future);
  if (mosques case []) return emptyPage<MediaItem>();
  return ref.watch(contentRepositoryProvider).media(mosques: mosques, size: 4);
});

final mediaItemProvider = FutureProvider.autoDispose.family<Fetched<MediaItem>, String>((ref, id) {
  ref.watch(currentUserIdProvider);
  return ref.watch(contentRepositoryProvider).mediaItem(id);
});

final relatedMediaProvider = FutureProvider.autoDispose.family<Fetched<List<MediaItem>>, String>((ref, id) {
  ref.watch(currentUserIdProvider);
  return ref.watch(contentRepositoryProvider).relatedMedia(id);
});
