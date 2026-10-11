import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/providers.dart';
import '../../data/models.dart';
import '../../data/paged.dart';

/// De agenda, per categorie (null = alle categorieën), met "meer laden".
final agendaProvider = AsyncNotifierProvider.autoDispose.family<AgendaController, Paged<Event>, EventCategory?>(
  AgendaController.new,
);

class AgendaController extends AsyncNotifier<Paged<Event>> {
  AgendaController(this.category);

  final EventCategory? category;

  @override
  Future<Paged<Event>> build() async {
    ref.watch(currentUserIdProvider);
    return Paged.first(await ref.watch(mosqueRepositoryProvider).events(category: category));
  }

  Future<void> loadMore() async {
    final current = state.value;
    if (current == null || !current.hasMore || current.loadingMore) return;
    state = AsyncData(current.withLoadingMore(loading: true));
    try {
      final next = await ref.read(mosqueRepositoryProvider).events(category: category, page: current.nextPage);
      if (ref.mounted) state = AsyncData(current.append(next));
    } on AppFailure {
      if (ref.mounted) state = AsyncData(current.withLoadingMore(loading: false, failed: true));
    }
  }
}

/// De eerstvolgende activiteiten voor de startpagina.
final upcomingEventsProvider = FutureProvider.autoDispose<Fetched<PageResult<Event>>>((ref) {
  ref.watch(currentUserIdProvider);
  return ref.watch(mosqueRepositoryProvider).events(size: 3);
});

/// Eén activiteit, met `registeredByMe` voor een ingelogde gebruiker.
final eventProvider = FutureProvider.autoDispose.family<Fetched<Event>, String>((ref, id) {
  ref.watch(currentUserIdProvider);
  return ref.watch(mosqueRepositoryProvider).event(id);
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
