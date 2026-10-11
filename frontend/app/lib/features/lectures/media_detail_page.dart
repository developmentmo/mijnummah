import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/design/design.dart';
import '../../core/format/dates.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/content_width.dart';
import '../../core/widgets/detail_app_bar.dart';
import '../../core/widgets/info_row.dart';
import '../../core/widgets/offline_banner.dart';
import '../../core/widgets/section_header.dart';
import '../../core/widgets/tag.dart';
import '../../data/models.dart';
import 'lectures_providers.dart';
import 'media_card.dart';
import 'video_player.dart';

/// Een lezing of opgenomen activiteit: de video, de gegevens en gerelateerde content.
class MediaDetailPage extends ConsumerWidget {
  const MediaDetailPage({super.key, required this.mediaId});

  final String mediaId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final provider = mediaItemProvider(mediaId);
    return Scaffold(
      appBar: const DetailAppBar(title: 'Lezing'),
      body: AsyncView(
        value: ref.watch(provider),
        onRetry: () => ref.invalidate(provider),
        loading: ListView(
          children: const [
            AspectRatio(aspectRatio: 16 / 9, child: Skeleton(radius: 0)),
            Padding(
              padding: EdgeInsets.all(TokSpace.s4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Skeleton(height: 28),
                  SizedBox(height: TokSpace.s3),
                  Skeleton(width: 180),
                  SizedBox(height: TokSpace.s6),
                  Skeleton(height: 80),
                ],
              ),
            ),
          ],
        ),
        data: (fetched) =>
            _MediaDetail(item: fetched.data, cachedAt: fetched.cachedAt, onRetry: () => ref.invalidate(provider)),
      ),
    );
  }
}

class _MediaDetail extends StatelessWidget {
  const _MediaDetail({required this.item, required this.cachedAt, required this.onRetry});

  final MediaItem item;
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
              MediaVideo(item: item),
              Padding(
                padding: const EdgeInsets.all(TokSpace.s4),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (cachedAt != null) ...[
                      OfflineBanner(savedAt: cachedAt!, onRetry: onRetry),
                      const SizedBox(height: TokSpace.s4),
                    ],
                    Align(
                      alignment: Alignment.centerLeft,
                      child: Tag(label: item.category.label, tone: TagTone.primary),
                    ),
                    const SizedBox(height: TokSpace.s3),
                    Semantics(header: true, child: Text(item.title, style: text.headlineMedium)),
                    const SizedBox(height: TokSpace.s3),
                    if (item.speaker != null)
                      InfoRow(icon: Icons.person_outline, label: 'Spreker', value: item.speaker!),
                    InfoRow(
                      icon: Icons.calendar_today_outlined,
                      label: 'Gepubliceerd',
                      value: 'Gepubliceerd op ${Dates.date(item.publishedAt)}',
                    ),
                    if (item.recordedOn != null)
                      InfoRow(
                        icon: Icons.mic_none_outlined,
                        label: 'Opgenomen',
                        value: 'Opgenomen op ${Dates.calendarDate(item.recordedOn!)}',
                      ),
                    if (item.duration != null)
                      InfoRow(icon: Icons.schedule_outlined, label: 'Duur', value: Dates.duration(item.duration!)),
                    if (item.description != null) ...[
                      const SizedBox(height: TokSpace.s4),
                      Text(item.description!, style: text.bodyLarge),
                    ],
                    if (item.relatedEvent != null) ...[
                      const SectionHeader(title: 'Bij deze activiteit'),
                      Card(
                        child: ListTile(
                          leading: const Icon(Icons.event_outlined),
                          title: Text(item.relatedEvent!.title),
                          subtitle: Text(Dates.compact(item.relatedEvent!.startsAt)),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () => context.push('/activiteit/${item.relatedEvent!.id}'),
                        ),
                      ),
                    ],
                    _RelatedMedia(mediaId: item.id),
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

/// "Meer zoals dit": items met dezelfde categorie of spreker. Een fout hier verbergt de sectie; de video zelf
/// blijft bruikbaar.
class _RelatedMedia extends ConsumerWidget {
  const _RelatedMedia({required this.mediaId});

  final String mediaId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final related = ref.watch(relatedMediaProvider(mediaId)).value?.data ?? const <MediaItem>[];
    if (related.isEmpty) return const SizedBox.shrink();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SectionHeader(title: 'Meer zoals dit'),
        ResponsiveCardGrid(
          minItemWidth: 260,
          children: [
            for (final item in related) MediaCard(item: item, onTap: () => context.push('/lezing/${item.id}')),
          ],
        ),
      ],
    );
  }
}
