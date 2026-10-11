import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/design/design.dart';
import '../../core/format/dates.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/content_width.dart';
import '../../core/widgets/detail_app_bar.dart';
import '../../core/widgets/info_row.dart';
import '../../core/widgets/network_image_box.dart';
import '../../core/widgets/offline_banner.dart';
import '../../core/widgets/tag.dart';
import '../../data/models.dart';
import 'agenda_providers.dart';
import 'registration_panel.dart';

/// Alle details van één activiteit, met aanmelden als de moskee dat heeft aangezet.
class EventDetailPage extends ConsumerWidget {
  const EventDetailPage({super.key, required this.eventId});

  final String eventId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final provider = eventProvider(eventId);
    return Scaffold(
      appBar: const DetailAppBar(title: 'Activiteit'),
      body: AsyncView(
        value: ref.watch(provider),
        onRetry: () => ref.invalidate(provider),
        loading: const _DetailSkeleton(),
        data: (fetched) => RefreshIndicator(
          onRefresh: () => ref.refresh(provider.future),
          child: _EventDetail(event: fetched.data, cachedAt: fetched.cachedAt, onRetry: () => ref.invalidate(provider)),
        ),
      ),
    );
  }
}

class _EventDetail extends StatelessWidget {
  const _EventDetail({required this.event, required this.cachedAt, required this.onRetry});

  final Event event;
  final DateTime? cachedAt;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return ListView(
      padding: const EdgeInsets.only(bottom: TokSpace.s8),
      children: [
        ContentWidth(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (event.imageUrl != null) NetworkImageBox(url: event.imageUrl, fallbackIcon: Icons.event_outlined),
              Padding(
                padding: const EdgeInsets.all(TokSpace.s4),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (cachedAt != null) ...[
                      OfflineBanner(savedAt: cachedAt!, onRetry: onRetry),
                      const SizedBox(height: TokSpace.s4),
                    ],
                    if (event.cancelled) ...[
                      const StatusMessage(
                        kind: StatusKind.error,
                        title: 'Geannuleerd',
                        message: 'Deze activiteit gaat niet door.',
                      ),
                      const SizedBox(height: TokSpace.s4),
                    ],
                    Wrap(
                      spacing: TokSpace.s2,
                      runSpacing: TokSpace.s2,
                      children: [
                        Tag(label: event.category.label, tone: TagTone.primary),
                        event.visibility == ContentVisibility.public
                            ? const Tag(label: 'Voor iedereen', icon: Icons.public)
                            : const Tag(label: 'Alleen voor ingelogde gebruikers', icon: Icons.lock_outline),
                      ],
                    ),
                    const SizedBox(height: TokSpace.s3),
                    Semantics(header: true, child: Text(event.title, style: text.headlineMedium)),
                    const SizedBox(height: TokSpace.s4),
                    InfoRow(
                      icon: Icons.calendar_today_outlined,
                      label: 'Datum',
                      value: Dates.weekdayDate(event.startsAt),
                    ),
                    InfoRow(
                      icon: Icons.schedule_outlined,
                      label: 'Tijd',
                      value: Dates.timeRange(event.startsAt, event.endsAt),
                    ),
                    if (event.location != null)
                      InfoRow(icon: Icons.place_outlined, label: 'Locatie', value: event.location!),
                    if (event.description != null) ...[
                      const SizedBox(height: TokSpace.s4),
                      Text(event.description!, style: text.bodyLarge),
                    ],
                    if (!event.cancelled && event.registration.enabled) ...[
                      const SizedBox(height: TokSpace.s6),
                      RegistrationPanel(event: event),
                    ],
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

class _DetailSkeleton extends StatelessWidget {
  const _DetailSkeleton();

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(TokSpace.s4),
      children: const [
        Skeleton(width: 120, height: 24, radius: TokRadius.full),
        SizedBox(height: TokSpace.s4),
        Skeleton(height: 32),
        SizedBox(height: TokSpace.s4),
        Skeleton(width: 220),
        SizedBox(height: TokSpace.s2),
        Skeleton(width: 160),
        SizedBox(height: TokSpace.s6),
        Skeleton(height: 96),
      ],
    );
  }
}
