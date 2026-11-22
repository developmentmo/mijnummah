import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api/api_client.dart';
import '../../core/brand/brand.g.dart';
import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../core/widgets/content_width.dart';
import '../../core/widgets/offline_banner.dart';
import '../../core/widgets/preview_section.dart';
import '../../data/models.dart';
import '../agenda/agenda_providers.dart';
import '../agenda/event_card.dart';
import '../announcements/announcement_card.dart';
import '../announcements/announcement_providers.dart';
import '../discover/discover_providers.dart';
import '../lectures/lectures_providers.dart';
import '../lectures/media_card.dart';
import '../mosques/mosque_filter.dart';
import '../mosques/mosque_widgets.dart';
import '../profile/follow_providers.dart';

const _allMosques = (text: '', city: null);

/// Startpagina: welkom, je eigen (of uitgelichte) moskeeën, en de activiteiten, video's en het nieuws daarvan.
/// Wie moskeeën volgt, ziet hier wat die moskeeën doen; anders wat er bij alle moskeeën gebeurt.
class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  Future<void> _refresh(WidgetRef ref, MosqueFilter scope) async {
    ref
      ..invalidate(myFollowsProvider)
      ..invalidate(mosqueListProvider(_allMosques))
      ..invalidate(upcomingEventsProvider(scope))
      ..invalidate(latestMediaProvider(scope))
      ..invalidate(latestNewsProvider(scope));
    await ref.read(upcomingEventsProvider(scope).future).then((_) {}, onError: (_) {});
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final follows = ref.watch(myFollowsProvider);
    final following = follows.value?.isNotEmpty ?? false;
    final scope = following ? MosqueFilter.followed : MosqueFilter.all;
    // Tot bekend is of de gebruiker moskeeën volgt, staan de secties op "laden" (geen dubbele verzoeken).
    final ready = follows.hasValue || follows.hasError;
    const pending = AsyncLoading<Never>();
    final events = ready ? ref.watch(upcomingEventsProvider(scope)) : pending;
    final media = ready ? ref.watch(latestMediaProvider(scope)) : pending;
    final news = ready ? ref.watch(latestNewsProvider(scope)) : pending;
    final cachedAt = [events, media, news].map((value) => value.value?.cachedAt).whereType<DateTime>().firstOrNull;
    final scopeQuery = following ? '?moskee=gevolgd' : '';

    return Scaffold(
      appBar: AppBar(title: const BrandLogo(height: 32), toolbarHeight: 64),
      body: RefreshIndicator(
        onRefresh: () => _refresh(ref, scope),
        child: ListView(
          padding: const EdgeInsets.fromLTRB(TokSpace.s4, TokSpace.s2, TokSpace.s4, TokSpace.s8),
          children: [
            ContentWidth(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _Welcome(following: following),
                  if (cachedAt != null) ...[
                    const SizedBox(height: TokSpace.s4),
                    OfflineBanner(savedAt: cachedAt, onRetry: () => _refresh(ref, scope)),
                  ],
                  if (following) _MyMosques(follows: follows.requireValue) else const _FeaturedMosques(),
                  PreviewSection<PageResult<Event>>(
                    title: 'Binnenkort',
                    actionLabel: 'Hele agenda',
                    onAction: () => context.go('/agenda$scopeQuery'),
                    value: events,
                    isEmpty: (page) => page.items.isEmpty,
                    onRetry: () => ref.invalidate(upcomingEventsProvider(scope)),
                    skeleton: const EventCardSkeleton(),
                    empty: Text(
                      'Er staan nu geen activiteiten gepland. Kijk later nog eens in de agenda.',
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                    builder: (page) => Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        for (final event in page.items)
                          Padding(
                            padding: const EdgeInsets.only(bottom: TokSpace.s3),
                            child: EventCard(event: event, onTap: () => context.push('/activiteit/${event.id}')),
                          ),
                      ],
                    ),
                  ),
                  PreviewSection<PageResult<MediaItem>>(
                    title: 'Nieuwe video\'s',
                    actionLabel: 'Bibliotheek',
                    onAction: () => context.go('/lezingen$scopeQuery'),
                    value: media,
                    hideWhenEmpty: true,
                    isEmpty: (page) => page.items.isEmpty,
                    onRetry: () => ref.invalidate(latestMediaProvider(scope)),
                    skeleton: const CardRail(children: [MediaCardSkeleton(), MediaCardSkeleton()]),
                    builder: (page) => CardRail(
                      children: [
                        for (final item in page.items)
                          MediaCard(item: item, onTap: () => context.push('/lezing/${item.id}')),
                      ],
                    ),
                  ),
                  PreviewSection<PageResult<Announcement>>(
                    title: 'Nieuws',
                    actionLabel: 'Al het nieuws',
                    onAction: () => context.push('/nieuws$scopeQuery'),
                    value: news,
                    hideWhenEmpty: true,
                    isEmpty: (page) => page.items.isEmpty,
                    onRetry: () => ref.invalidate(latestNewsProvider(scope)),
                    skeleton: const Skeleton(height: 96),
                    builder: (page) => Column(
                      children: [
                        for (final announcement in page.items)
                          Padding(
                            padding: const EdgeInsets.only(bottom: TokSpace.s3),
                            child: AnnouncementCard(
                              announcement: announcement,
                              onTap: () => context.push('/aankondiging/${announcement.id}'),
                            ),
                          ),
                      ],
                    ),
                  ),
                  if (ref.watch(currentUserIdProvider) == null) ...[
                    const SizedBox(height: TokSpace.s6),
                    const _SignInInvitation(),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Welcome extends ConsumerWidget {
  const _Welcome({required this.following});

  final bool following;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final text = Theme.of(context).textTheme;
    final name = ref.watch(authControllerProvider).value?.displayName;
    return Padding(
      padding: const EdgeInsets.only(top: TokSpace.s2),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Semantics(
            header: true,
            child: Text(name == null ? 'Assalamoe alaikoem' : 'Assalamoe alaikoem, $name', style: text.headlineMedium),
          ),
          const SizedBox(height: TokSpace.s2),
          Text(
            following
                ? 'Welkom terug bij ${Brand.name}. Hier zie je wat er gebeurt bij de moskeeën die je volgt.'
                : 'Welkom bij ${Brand.name}. Ontdek activiteiten, lezingen en nieuws van moskeeën bij jou in de buurt.',
            style: text.bodyLarge?.copyWith(color: context.colors.onSurfaceMuted),
          ),
        ],
      ),
    );
  }
}

/// De gevolgde moskeeën, favorieten eerst.
class _MyMosques extends StatelessWidget {
  const _MyMosques({required this.follows});

  final List<FollowStatus> follows;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        PreviewSection<List<FollowStatus>>(
          title: 'Jouw moskeeën',
          actionLabel: 'Meer ontdekken',
          onAction: () => context.go('/ontdekken'),
          value: AsyncData(Fetched(follows)),
          isEmpty: (list) => list.isEmpty,
          onRetry: () {},
          skeleton: const SizedBox.shrink(),
          builder: (list) => CardRail(
            width: 160,
            children: [for (final follow in list) _MosqueCard(mosque: follow.mosque, favorite: follow.favorite)],
          ),
        ),
      ],
    );
  }
}

/// Zonder gevolgde moskeeën: een greep uit de aangesloten moskeeën.
class _FeaturedMosques extends ConsumerWidget {
  const _FeaturedMosques();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final mosques = ref.watch(mosqueListProvider(_allMosques));
    return PreviewSection<List<Mosque>>(
      title: 'Moskeeën ontdekken',
      actionLabel: 'Alle moskeeën',
      onAction: () => context.go('/ontdekken'),
      value: mosques.whenData((paged) => Fetched(paged.items.take(8).toList(), cachedAt: paged.cachedAt)),
      hideWhenEmpty: true,
      isEmpty: (list) => list.isEmpty,
      onRetry: () => ref.invalidate(mosqueListProvider(_allMosques)),
      skeleton: const CardRail(width: 160, children: [Skeleton(height: 120), Skeleton(height: 120)]),
      builder: (list) => CardRail(
        width: 160,
        children: [for (final mosque in list) _MosqueCard(mosque: mosque.summary, city: mosque.city)],
      ),
    );
  }
}

class _MosqueCard extends StatelessWidget {
  const _MosqueCard({required this.mosque, this.city, this.favorite = false});

  final MosqueSummary mosque;
  final String? city;
  final bool favorite;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Semantics(
        button: true,
        label: [mosque.name, ?city, if (favorite) 'favoriet'].join(', '),
        excludeSemantics: true,
        child: InkWell(
          onTap: () => context.push('/moskee/${Uri.encodeComponent(mosque.slug)}'),
          child: Padding(
            padding: const EdgeInsets.all(TokSpace.s3),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    MosqueAvatar(name: mosque.name, logoUrl: mosque.logoUrl, size: 48),
                    const Spacer(),
                    if (favorite) Icon(Icons.star, color: context.colors.accentText, size: TokIconSize.md),
                  ],
                ),
                const SizedBox(height: TokSpace.s2),
                Text(mosque.name, style: text.titleSmall, maxLines: 2, overflow: TextOverflow.ellipsis),
                if (city != null) Text(city!, style: text.bodySmall),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _SignInInvitation extends StatelessWidget {
  const _SignInInvitation();

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final text = Theme.of(context).textTheme;
    return Card(
      color: c.primaryContainer,
      child: Padding(
        padding: const EdgeInsets.all(TokComponent.cardPadding),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.favorite_outline, color: c.onPrimaryContainer),
                const SizedBox(width: TokSpace.s2),
                Expanded(
                  child: Text('Volg je moskeeën', style: text.titleMedium?.copyWith(color: c.onPrimaryContainer)),
                ),
              ],
            ),
            const SizedBox(height: TokSpace.s2),
            Text(
              'Met een account volg je moskeeën, krijg je meldingen van hun nieuws en bewaar je video\'s. Volgen is '
              'gratis en is geen lidmaatschap van een moskee.',
              style: text.bodyMedium?.copyWith(color: c.onPrimaryContainer),
            ),
            const SizedBox(height: TokSpace.s3),
            FilledButton(onPressed: () => context.push('/inloggen'), child: const Text('Inloggen of account maken')),
          ],
        ),
      ),
    );
  }
}
