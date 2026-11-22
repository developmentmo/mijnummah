import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/providers.dart';
import '../../data/models.dart';
import '../../data/paged.dart';
import '../mosques/mosque_filter.dart';

/// Het nieuws van alle (of de gekozen) moskeeën, nieuwste eerst, met "meer laden".
final newsProvider = AsyncNotifierProvider.autoDispose.family<NewsController, Paged<Announcement>, MosqueFilter>(
  NewsController.new,
);

class NewsController extends AsyncNotifier<Paged<Announcement>> {
  NewsController(this.filter);

  final MosqueFilter filter;
  List<String>? _mosques;

  @override
  Future<Paged<Announcement>> build() async {
    _mosques = await ref.watch(mosqueSlugsProvider(filter).future);
    if (_mosques case []) return Paged.first(emptyPage<Announcement>());
    return Paged.first(await _page(0));
  }

  Future<Fetched<PageResult<Announcement>>> _page(int page) =>
      ref.read(contentRepositoryProvider).announcements(mosques: _mosques, page: page);

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

/// Het laatste nieuws (startpagina, moskeeprofiel).
final latestNewsProvider = FutureProvider.autoDispose.family<Fetched<PageResult<Announcement>>, MosqueFilter>((
  ref,
  filter,
) async {
  final mosques = await ref.watch(mosqueSlugsProvider(filter).future);
  if (mosques case []) return emptyPage<Announcement>();
  return ref.watch(contentRepositoryProvider).announcements(mosques: mosques, size: 3);
});

final announcementProvider = FutureProvider.autoDispose.family<Fetched<Announcement>, String>(
  (ref, id) => ref.watch(contentRepositoryProvider).announcement(id),
);
