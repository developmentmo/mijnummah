import '../core/api/api_client.dart';
import 'json.dart';

/// Een lijst die per pagina wordt aangevuld ("meer laden").
class Paged<T> {
  const Paged({
    required this.items,
    required this.hasMore,
    required this.nextPage,
    this.totalItems = 0,
    this.cachedAt,
    this.loadingMore = false,
    this.loadMoreFailed = false,
  });

  factory Paged.first(Fetched<PageResult<T>> result) => Paged(
    items: result.data.items,
    hasMore: result.data.hasMore,
    nextPage: result.data.page + 1,
    totalItems: result.data.totalItems,
    cachedAt: result.cachedAt,
  );

  final List<T> items;
  final bool hasMore;
  final int nextPage;
  final int totalItems;

  /// Gezet als de eerste pagina uit de offline-cache komt.
  final DateTime? cachedAt;
  final bool loadingMore;
  final bool loadMoreFailed;

  Paged<T> append(Fetched<PageResult<T>> result) => Paged(
    items: [...items, ...result.data.items],
    hasMore: result.data.hasMore,
    nextPage: result.data.page + 1,
    totalItems: result.data.totalItems,
    cachedAt: cachedAt ?? result.cachedAt,
  );

  Paged<T> withLoadingMore({required bool loading, bool failed = false}) => Paged(
    items: items,
    hasMore: hasMore,
    nextPage: nextPage,
    totalItems: totalItems,
    cachedAt: cachedAt,
    loadingMore: loading,
    loadMoreFailed: failed,
  );
}
