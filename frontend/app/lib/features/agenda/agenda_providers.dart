import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/providers.dart';
import '../../data/models.dart';
import '../../data/paged.dart';
import '../mosques/mosque_filter.dart';

/// Periode in de agenda, gerekend vanaf nu in de tijdzone van het toestel.
enum AgendaPeriod {
  all('alles', 'Alles'),
  today('vandaag', 'Vandaag'),
  week('week', 'Deze week'),
  month('maand', 'Deze maand');

  const AgendaPeriod(this.queryValue, this.label);

  final String queryValue;
  final String label;

  static AgendaPeriod parse(String? value) =>
      values.firstWhere((period) => period.queryValue == value, orElse: () => AgendaPeriod.all);

  /// Het einde van de periode (exclusief), of null voor alles wat nog komt.
  DateTime? end(DateTime now) {
    final today = DateTime(now.year, now.month, now.day);
    return switch (this) {
      AgendaPeriod.all => null,
      AgendaPeriod.today => today.add(const Duration(days: 1)),
      AgendaPeriod.week => today.add(Duration(days: 8 - today.weekday)),
      AgendaPeriod.month => DateTime(now.year, now.month + 1),
    };
  }
}

/// Filters van de agenda.
typedef AgendaQuery = ({MosqueFilter mosque, EventCategory? category, AgendaPeriod period});

/// De agenda van alle (of de gekozen) moskeeën, met "meer laden".
final agendaProvider = AsyncNotifierProvider.autoDispose.family<AgendaController, Paged<Event>, AgendaQuery>(
  AgendaController.new,
);

class AgendaController extends AsyncNotifier<Paged<Event>> {
  AgendaController(this.query);

  final AgendaQuery query;
  List<String>? _mosques;
  DateTime? _to;

  @override
  Future<Paged<Event>> build() async {
    ref.watch(currentUserIdProvider);
    _mosques = await ref.watch(mosqueSlugsProvider(query.mosque).future);
    if (_mosques case []) return Paged.first(emptyPage<Event>());
    _to = query.period.end(DateTime.now());
    return Paged.first(await _page(0));
  }

  Future<Fetched<PageResult<Event>>> _page(int page) =>
      ref.read(contentRepositoryProvider).events(mosques: _mosques, category: query.category, to: _to, page: page);

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

/// De eerstvolgende activiteiten (startpagina, moskeeprofiel).
final upcomingEventsProvider = FutureProvider.autoDispose.family<Fetched<PageResult<Event>>, MosqueFilter>((
  ref,
  filter,
) async {
  ref.watch(currentUserIdProvider);
  final mosques = await ref.watch(mosqueSlugsProvider(filter).future);
  if (mosques case []) return emptyPage<Event>();
  return ref.watch(contentRepositoryProvider).events(mosques: mosques, size: 3);
});

/// Eén activiteit, met `registeredByMe` voor een ingelogde gebruiker.
final eventProvider = FutureProvider.autoDispose.family<Fetched<Event>, String>((ref, id) {
  ref.watch(currentUserIdProvider);
  return ref.watch(contentRepositoryProvider).event(id);
});

/// Eigen aanmeldingen (alleen voor een ingelogde gebruiker; anders leeg).
final myRegistrationsProvider = FutureProvider.autoDispose<List<MyRegistration>>((ref) async {
  if (ref.watch(currentUserIdProvider) == null) return const [];
  return ref.watch(meRepositoryProvider).registrations();
});

/// Haalt alles opnieuw op waarin de aanmeldstatus van een activiteit zichtbaar is.
void refreshRegistrationViews(WidgetRef ref, String eventId) {
  ref.invalidate(eventProvider(eventId));
  ref.invalidate(agendaProvider);
  ref.invalidate(upcomingEventsProvider);
  ref.invalidate(myRegistrationsProvider);
}
