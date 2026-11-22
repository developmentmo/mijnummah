import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api/api_exception.dart';
import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/content_width.dart';
import '../../core/widgets/detail_app_bar.dart';
import '../../core/widgets/external_links.dart';
import '../../core/widgets/info_row.dart';
import '../../core/widgets/network_image_box.dart';
import '../../core/widgets/offline_banner.dart';
import '../../core/widgets/preview_section.dart';
import '../../core/widgets/section_header.dart';
import '../../data/models.dart';
import '../agenda/agenda_providers.dart';
import '../agenda/event_card.dart';
import '../announcements/announcement_card.dart';
import '../announcements/announcement_providers.dart';
import '../lectures/lectures_providers.dart';
import '../lectures/media_card.dart';
import '../profile/follow_card.dart';
import '../profile/follow_providers.dart';
import 'mosque_filter.dart';
import 'mosque_widgets.dart';

/// Het profiel van één moskee, in haar eigen gezicht (omslagfoto en logo): wie ze is, adres en contact,
/// gebedstijden, en haar activiteiten, video's en nieuws.
class MosqueProfilePage extends ConsumerWidget {
  const MosqueProfilePage({super.key, required this.slug});

  final String slug;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final provider = mosqueProvider(slug);
    final name = ref.watch(provider).value?.data.name;
    return Scaffold(
      appBar: DetailAppBar(title: name ?? 'Moskee'),
      body: AsyncView(
        value: ref.watch(provider),
        onRetry: () => ref.invalidate(provider),
        loading: ListView(
          children: const [
            AspectRatio(aspectRatio: 21 / 9, child: Skeleton(radius: 0)),
            Padding(
              padding: EdgeInsets.all(TokSpace.s4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Skeleton(height: 32),
                  SizedBox(height: TokSpace.s3),
                  Skeleton(width: 160),
                  SizedBox(height: TokSpace.s6),
                  Skeleton(height: 96),
                ],
              ),
            ),
          ],
        ),
        data: (fetched) => RefreshIndicator(
          onRefresh: () async {
            final only = MosqueFilter.only(slug);
            ref
              ..invalidate(provider)
              ..invalidate(upcomingEventsProvider(only))
              ..invalidate(latestMediaProvider(only))
              ..invalidate(latestNewsProvider(only));
            await ref.read(provider.future);
          },
          child: _Profile(mosque: fetched.data, cachedAt: fetched.cachedAt, onRetry: () => ref.invalidate(provider)),
        ),
      ),
    );
  }
}

class _Profile extends ConsumerWidget {
  const _Profile({required this.mosque, required this.cachedAt, required this.onRetry});

  final Mosque mosque;
  final DateTime? cachedAt;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final text = Theme.of(context).textTheme;
    final only = MosqueFilter.only(mosque.slug);
    final query = Uri.encodeQueryComponent(mosque.slug);
    final address = mosque.addressLine;
    final website = mosque.websiteUrl == null ? null : Uri.tryParse(mosque.websiteUrl!);
    final prayerTimes = mosque.prayerTimesUrl == null ? null : Uri.tryParse(mosque.prayerTimesUrl!);
    final hasContact = address != null || mosque.email != null || mosque.phone != null || website != null;

    return ListView(
      padding: const EdgeInsets.only(bottom: TokSpace.s8),
      children: [
        ContentWidth(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              NetworkImageBox(url: mosque.coverUrl, fallbackIcon: Icons.mosque_outlined, aspectRatio: 21 / 9),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: TokSpace.s4),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (cachedAt != null) ...[
                      const SizedBox(height: TokSpace.s4),
                      OfflineBanner(savedAt: cachedAt!, onRetry: onRetry),
                    ],
                    const SizedBox(height: TokSpace.s4),
                    Row(
                      children: [
                        MosqueAvatar(name: mosque.name, logoUrl: mosque.logoUrl, size: 64),
                        const SizedBox(width: TokSpace.s3),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Semantics(header: true, child: Text(mosque.name, style: text.headlineMedium)),
                              if (mosque.city != null) Text(mosque.city!, style: text.bodyMedium),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: TokSpace.s4),
                    Wrap(
                      spacing: TokSpace.s2,
                      runSpacing: TokSpace.s2,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        FollowButton(mosque: mosque.summary),
                        _FavoriteMosqueButton(mosque: mosque),
                      ],
                    ),
                    if (mosque.description != null) ...[
                      const SizedBox(height: TokSpace.s4),
                      Text(mosque.description!, style: text.bodyLarge),
                    ],
                    if (prayerTimes != null && prayerTimes.hasScheme) ...[
                      const SizedBox(height: TokSpace.s4),
                      OutlinedButton.icon(
                        onPressed: () => openExternal(context, prayerTimes),
                        icon: const Icon(Icons.schedule_outlined),
                        label: const Text('Gebedstijden'),
                      ),
                    ],
                    PreviewSection<PageResult<Event>>(
                      title: 'Binnenkort',
                      actionLabel: 'Hele agenda',
                      onAction: () => context.go('/agenda?moskee=$query'),
                      value: ref.watch(upcomingEventsProvider(only)),
                      isEmpty: (page) => page.items.isEmpty,
                      onRetry: () => ref.invalidate(upcomingEventsProvider(only)),
                      skeleton: const EventCardSkeleton(),
                      empty: Text('Er staan nu geen activiteiten gepland.', style: text.bodyMedium),
                      builder: (page) => Column(
                        children: [
                          for (final event in page.items)
                            Padding(
                              padding: const EdgeInsets.only(bottom: TokSpace.s3),
                              child: EventCard(
                                event: event,
                                showMosque: false,
                                onTap: () => context.push('/activiteit/${event.id}'),
                              ),
                            ),
                        ],
                      ),
                    ),
                    PreviewSection<PageResult<MediaItem>>(
                      title: 'Video\'s',
                      actionLabel: 'Alle video\'s',
                      onAction: () => context.go('/lezingen?moskee=$query'),
                      value: ref.watch(latestMediaProvider(only)),
                      hideWhenEmpty: true,
                      isEmpty: (page) => page.items.isEmpty,
                      onRetry: () => ref.invalidate(latestMediaProvider(only)),
                      skeleton: const CardRail(children: [MediaCardSkeleton(), MediaCardSkeleton()]),
                      builder: (page) => CardRail(
                        children: [
                          for (final item in page.items)
                            MediaCard(item: item, showMosque: false, onTap: () => context.push('/lezing/${item.id}')),
                        ],
                      ),
                    ),
                    PreviewSection<PageResult<Announcement>>(
                      title: 'Nieuws',
                      actionLabel: 'Al het nieuws',
                      onAction: () => context.push('/nieuws?moskee=$query'),
                      value: ref.watch(latestNewsProvider(only)),
                      hideWhenEmpty: true,
                      isEmpty: (page) => page.items.isEmpty,
                      onRetry: () => ref.invalidate(latestNewsProvider(only)),
                      skeleton: const Skeleton(height: 96),
                      builder: (page) => Column(
                        children: [
                          for (final announcement in page.items)
                            Padding(
                              padding: const EdgeInsets.only(bottom: TokSpace.s3),
                              child: AnnouncementCard(
                                announcement: announcement,
                                showMosque: false,
                                onTap: () => context.push('/aankondiging/${announcement.id}'),
                              ),
                            ),
                        ],
                      ),
                    ),
                    if (hasContact) const SectionHeader(title: 'Adres en contact'),
                    if (address != null)
                      InfoRow(
                        icon: Icons.place_outlined,
                        label: 'Adres, opent de kaart',
                        value: address,
                        onTap: () => openExternal(context, mapsUri('$address, ${mosque.name}')),
                      ),
                    if (mosque.phone != null)
                      InfoRow(
                        icon: Icons.phone_outlined,
                        label: 'Bellen',
                        value: mosque.phone!,
                        onTap: () => openExternal(context, Uri(scheme: 'tel', path: mosque.phone!.replaceAll(' ', ''))),
                      ),
                    if (mosque.email != null)
                      InfoRow(
                        icon: Icons.mail_outline,
                        label: 'E-mailen',
                        value: mosque.email!,
                        onTap: () => openExternal(context, Uri(scheme: 'mailto', path: mosque.email!)),
                      ),
                    if (website != null && website.hasScheme)
                      InfoRow(
                        icon: Icons.language_outlined,
                        label: 'Website',
                        value: website.host.isEmpty ? mosque.websiteUrl! : website.host,
                        onTap: () => openExternal(context, website),
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

/// Ster om een gevolgde moskee als favoriet bovenaan te zetten. Alleen zichtbaar als de gebruiker haar volgt.
class _FavoriteMosqueButton extends ConsumerStatefulWidget {
  const _FavoriteMosqueButton({required this.mosque});

  final Mosque mosque;

  @override
  ConsumerState<_FavoriteMosqueButton> createState() => _FavoriteMosqueButtonState();
}

class _FavoriteMosqueButtonState extends ConsumerState<_FavoriteMosqueButton> {
  bool _busy = false;

  Future<void> _toggle(bool favorite) async {
    setState(() => _busy = true);
    try {
      await ref.setFavoriteMosque(widget.mosque.id, favorite);
    } on AppFailure {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Opslaan als favoriet is niet gelukt.')));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final follow = ref.watch(followOfProvider(widget.mosque.id)).value;
    if (follow == null) return const SizedBox.shrink();
    final favorite = follow.favorite;
    return IconButton(
      tooltip: favorite ? 'Niet meer als favoriet' : 'Favoriete moskee',
      isSelected: favorite,
      icon: const Icon(Icons.star_outline),
      selectedIcon: const Icon(Icons.star),
      onPressed: _busy ? null : () => _toggle(!favorite),
    );
  }
}
