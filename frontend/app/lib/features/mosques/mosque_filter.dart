import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../data/models.dart';
import '../profile/follow_providers.dart';

/// Van welke moskeeën een lijst content toont: alle, de gevolgde, of één moskee.
@immutable
class MosqueFilter {
  const MosqueFilter._(this.slug, this.followedOnly);

  const MosqueFilter.only(String slug) : this._(slug, false);

  static const all = MosqueFilter._(null, false);
  static const followed = MosqueFilter._(null, true);

  /// Leest de waarde van `?moskee=` (een slug, of `gevolgd`).
  factory MosqueFilter.parse(String? value) => switch (value?.trim()) {
    null || '' => all,
    'gevolgd' => followed,
    final slug => MosqueFilter.only(slug),
  };

  /// De slug bij één moskee; anders null.
  final String? slug;
  final bool followedOnly;

  bool get isAll => slug == null && !followedOnly;

  /// Waarde voor `?moskee=`, of null zonder filter.
  String? get queryValue => followedOnly ? 'gevolgd' : slug;

  @override
  bool operator ==(Object other) => other is MosqueFilter && other.slug == slug && other.followedOnly == followedOnly;

  @override
  int get hashCode => Object.hash(slug, followedOnly);

  @override
  String toString() => 'MosqueFilter(${queryValue ?? 'alle'})';
}

/// De slugs voor een filter: null betekent alle moskeeën, een lege lijst betekent geen enkele (de gebruiker volgt
/// niets, of is niet ingelogd).
final mosqueSlugsProvider = FutureProvider.autoDispose.family<List<String>?, MosqueFilter>((ref, filter) async {
  if (filter.slug != null) return [filter.slug!];
  if (!filter.followedOnly) return null;
  return [for (final follow in await ref.watch(myFollowsProvider.future)) follow.mosqueSlug];
});

/// Een lege eerste pagina, voor een filter zonder moskeeën: dan is er niets op te halen.
Fetched<PageResult<T>> emptyPage<T>() => Fetched(PageResult<T>(items: const [], page: 0, totalItems: 0, totalPages: 0));
