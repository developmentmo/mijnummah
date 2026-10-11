import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/design/design.dart';
import '../../core/format/dates.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/content_width.dart';
import '../../core/widgets/detail_app_bar.dart';
import '../../core/widgets/offline_banner.dart';
import '../../core/widgets/tag.dart';
import 'announcement_providers.dart';

class AnnouncementDetailPage extends ConsumerWidget {
  const AnnouncementDetailPage({super.key, required this.announcementId});

  final String announcementId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final provider = announcementProvider(announcementId);
    final text = Theme.of(context).textTheme;
    return Scaffold(
      appBar: const DetailAppBar(title: 'Aankondiging'),
      body: AsyncView(
        value: ref.watch(provider),
        onRetry: () => ref.invalidate(provider),
        loading: const Padding(
          padding: EdgeInsets.all(TokSpace.s4),
          child: Column(
            children: [
              Skeleton(height: 28),
              SizedBox(height: TokSpace.s4),
              Skeleton(height: 120),
            ],
          ),
        ),
        data: (fetched) {
          final announcement = fetched.data;
          return ListView(
            padding: const EdgeInsets.all(TokSpace.s4),
            children: [
              ContentWidth(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (fetched.cachedAt != null) ...[
                      OfflineBanner(savedAt: fetched.cachedAt!, onRetry: () => ref.invalidate(provider)),
                      const SizedBox(height: TokSpace.s4),
                    ],
                    if (announcement.featured) ...[
                      const Tag(label: 'Uitgelicht', tone: TagTone.accent, icon: Icons.star_outline),
                      const SizedBox(height: TokSpace.s3),
                    ],
                    Semantics(header: true, child: Text(announcement.title, style: text.headlineMedium)),
                    const SizedBox(height: TokSpace.s2),
                    Text('Geplaatst op ${Dates.date(announcement.publishedAt)}', style: text.bodySmall),
                    if (announcement.expiresAt != null)
                      Text('Geldig tot ${Dates.weekdayDate(announcement.expiresAt!)}', style: text.bodySmall),
                    const SizedBox(height: TokSpace.s4),
                    SelectableText(announcement.body, style: text.bodyLarge),
                  ],
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
