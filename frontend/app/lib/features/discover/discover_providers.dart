import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/providers.dart';
import '../../data/models.dart';
import '../../data/paged.dart';

/// Zoeken in de aangesloten moskeeën: op naam en op plaats (null = alle plaatsen).
typedef MosqueQuery = ({String text, String? city});

/// Actieve moskeeën, alfabetisch, met "meer laden".
final mosqueListProvider = AsyncNotifierProvider.autoDispose.family<MosqueListController, Paged<Mosque>, MosqueQuery>(
  MosqueListController.new,
);

class MosqueListController extends AsyncNotifier<Paged<Mosque>> {
  MosqueListController(this.query);

  final MosqueQuery query;

  @override
  Future<Paged<Mosque>> build() async => Paged.first(await _page(0));

  Future<Fetched<PageResult<Mosque>>> _page(int page) =>
      ref.read(contentRepositoryProvider).mosques(query: query.text, city: query.city, page: page);

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

/// Plaatsen met een aangesloten moskee.
final citiesProvider = FutureProvider.autoDispose<Fetched<List<String>>>(
  (ref) => ref.watch(contentRepositoryProvider).cities(),
);
