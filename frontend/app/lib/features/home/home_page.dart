import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api/api_client.dart';
import '../../core/brand/brand.g.dart';
import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../core/widgets/content_width.dart';
import '../../core/widgets/failure_messages.dart';
import '../../core/widgets/offline_banner.dart';
import '../../core/widgets/section_header.dart';
import '../../data/models.dart';
import '../agenda/agenda_providers.dart';
import '../agenda/event_card.dart';
import '../announcements/announcement_card.dart';
import '../announcements/announcement_providers.dart';
import '../lectures/lectures_providers.dart';
import '../lectures/media_card.dart';
import '../profile/follow_card.dart';

/// Startpagina: welkom, uitgelichte aankondigingen, de eerstvolgende activiteiten, lezingen en nieuwe video's.
class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  Future<void> _refresh(WidgetRef ref) async {
    ref
      ..invalidate(mosqueProvider)
      ..invalidate(homeAnnouncementsProvider)
      ..invalidate(upcomingEventsProvider)
      ..invalidate(latestLecturesProvider)
      ..invalidate(latestMediaProvider);
    await ref.read(upcomingEventsProvider.future).then((_) {}, onError: (_) {});
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final announcements = ref.watch(homeAnnouncementsProvider);
    final events = ref.watch(upcomingEventsProvider);
    final lectures = ref.watch(latestLecturesProvider);
    final latest = ref.watch(latestMediaProvider);
    final cachedAt = [
      announcements,
      events,
      lectures,
      latest,
    ].map((value) => value.value?.cachedAt).whereType<DateTime>().firstOrNull;

    final lectureIds = {for (final item in lectures.value?.data.items ?? const <MediaItem>[]) item.id};

    return Scaffold(
      appBar: AppBar(title: const BrandLogo(height: 32), toolbarHeight: 64),
      body: RefreshIndicator(
        onRefresh: () => _refresh(ref),
        child: ListView(
          padding: const EdgeInsets.fromLTRB(TokSpace.s4, TokSpace.s2, TokSpace.s4, TokSpace.s8),
          children: [
            ContentWidth(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const _Welcome(),
                  if (cachedAt != null) ...[
                    const SizedBox(height: TokSpace.s4),
                    OfflineBanner(savedAt: cachedAt, onRetry: () => _refresh(ref)),
                  ],
                  _Section<PageResult<Announcement>>(
                    title: 'Aankondigingen',
                    value: announcements,
                    hideWhenEmpty: true,
                    isEmpty: (page) => page.items.isEmpty,
                    onRetry: () => ref.invalidate(homeAnnouncementsProvider),
                    skeleton: const Card(
                      child: Padding(
                        padding: EdgeInsets.all(TokComponent.cardPadding),
                        child: Column(
                          children: [
                            Skeleton(height: 20),
                            SizedBox(height: TokSpace.s2),
                            Skeleton(),
                          ],
                        ),
                      ),
                    ),
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
                  _Section<PageResult<Event>>(
                    title: 'Binnenkort',
                    actionLabel: 'Hele agenda',
                    onAction: () => context.go('/agenda'),
                    value: events,
                    isEmpty: (page) => page.items.isEmpty,
                    onRetry: () => ref.invalidate(upcomingEventsProvider),
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
                        OutlinedButton.icon(
                          onPressed: () => context.go('/agenda'),
                          icon: const Icon(Icons.calendar_month_outlined),
                          label: const Text('Bekijk de hele agenda'),
                        ),
                      ],
                    ),
                  ),
                  _Section<PageResult<MediaItem>>(
                    title: 'Lezingen',
                    actionLabel: 'Alle lezingen',
                    onAction: () => context.go('/lezingen'),
                    value: lectures,
                    hideWhenEmpty: true,
                    isEmpty: (page) => page.items.isEmpty,
                    onRetry: () => ref.invalidate(latestLecturesProvider),
                    skeleton: const _MediaRail(children: [MediaCardSkeleton(), MediaCardSkeleton()]),
                    builder: (page) => _MediaRail(
                      children: [
                        for (final item in page.items)
                          MediaCard(item: item, onTap: () => context.push('/lezing/${item.id}')),
                      ],
                    ),
                  ),
                  _Section<List<MediaItem>>(
                    title: 'Nieuw toegevoegd',
                    actionLabel: 'Bibliotheek',
                    onAction: () => context.go('/lezingen'),
                    value: latest.whenData(
                      (fetched) => fetched.map(
                        (page) => page.items.where((item) => !lectureIds.contains(item.id)).take(4).toList(),
                      ),
                    ),
                    hideWhenEmpty: true,
                    isEmpty: (items) => items.isEmpty,
                    onRetry: () => ref.invalidate(latestMediaProvider),
                    skeleton: const _MediaRail(children: [MediaCardSkeleton(), MediaCardSkeleton()]),
                    builder: (items) => _MediaRail(
                      children: [
                        for (final item in items)
                          MediaCard(item: item, onTap: () => context.push('/lezing/${item.id}')),
                      ],
                    ),
                  ),
                  const SizedBox(height: TokSpace.s6),
                  const FollowInvitation(),
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
  const _Welcome();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final text = Theme.of(context).textTheme;
    final name = ref.watch(authControllerProvider).value?.displayName;
    final mosqueName = ref.watch(mosqueProvider).value?.data.name ?? Brand.name;
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
            'Welkom bij $mosqueName. Hier vind je wat er in onze moskee gebeurt: activiteiten, lezingen en nieuws.',
            style: text.bodyLarge?.copyWith(color: context.colors.onSurfaceMuted),
          ),
        ],
      ),
    );
  }
}

/// Sectie op de startpagina met eigen laad-, fout- en lege toestand, zodat één mislukte lijst de rest niet blokkeert.
class _Section<T> extends StatelessWidget {
  const _Section({
    required this.title,
    required this.value,
    required this.isEmpty,
    required this.onRetry,
    required this.skeleton,
    required this.builder,
    this.actionLabel,
    this.onAction,
    this.empty,
    this.hideWhenEmpty = false,
  });

  final String title;
  final String? actionLabel;
  final VoidCallback? onAction;
  final AsyncValue<Fetched<T>> value;
  final bool Function(T data) isEmpty;
  final VoidCallback onRetry;
  final Widget skeleton;
  final Widget Function(T data) builder;
  final Widget? empty;
  final bool hideWhenEmpty;

  @override
  Widget build(BuildContext context) {
    final Widget content;
    if (value.hasError && !value.isLoading) {
      final failure = describeFailure(value.error!);
      content = StatusMessage(
        kind: StatusKind.error,
        title: failure.title,
        message: failure.message,
        actionLabel: 'Opnieuw proberen',
        onAction: onRetry,
      );
    } else if (value.hasValue) {
      final data = value.requireValue.data;
      if (isEmpty(data)) {
        if (hideWhenEmpty || empty == null) return const SizedBox.shrink();
        content = empty!;
      } else {
        content = builder(data);
      }
    } else {
      content = Semantics(label: '$title worden geladen', child: skeleton);
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(title: title, actionLabel: actionLabel, onAction: onAction),
        content,
      ],
    );
  }
}

/// Horizontaal scrollende rij kaarten. De hoogte volgt de inhoud, zodat grotere tekst niet wordt afgekapt.
class _MediaRail extends StatelessWidget {
  const _MediaRail({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      clipBehavior: Clip.none,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (final child in children)
            Padding(
              padding: const EdgeInsets.only(right: TokSpace.s3),
              child: SizedBox(width: 260, child: child),
            ),
        ],
      ),
    );
  }
}
