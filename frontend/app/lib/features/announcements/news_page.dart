import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/design/design.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/content_width.dart';
import '../../core/widgets/detail_app_bar.dart';
import '../../core/widgets/offline_banner.dart';
import '../mosques/mosque_filter.dart';
import '../mosques/mosque_widgets.dart';
import 'announcement_card.dart';
import 'announcement_providers.dart';

/// Het nieuws van alle moskeeën, of van de gevolgde of één moskee.
class NewsPage extends ConsumerStatefulWidget {
  const NewsPage({super.key, this.initialMosque = MosqueFilter.all});

  final MosqueFilter initialMosque;

  @override
  ConsumerState<NewsPage> createState() => _NewsPageState();
}

class _NewsPageState extends ConsumerState<NewsPage> {
  late MosqueFilter _mosque = widget.initialMosque;

  @override
  Widget build(BuildContext context) {
    final provider = newsProvider(_mosque);
    return Scaffold(
      appBar: const DetailAppBar(title: 'Nieuws'),
      body: Column(
        children: [
          MosqueFilterRow(selected: _mosque, onSelected: (mosque) => setState(() => _mosque = mosque)),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () => ref.refresh(provider.future),
              child: AsyncView(
                value: ref.watch(provider),
                onRetry: () => ref.invalidate(provider),
                loading: ListView(
                  padding: const EdgeInsets.all(TokSpace.s4),
                  children: [
                    for (var i = 0; i < 3; i++)
                      const Padding(
                        padding: EdgeInsets.only(bottom: TokSpace.s3),
                        child: Skeleton(height: 112),
                      ),
                  ],
                ),
                data: (paged) => ContentWidth(
                  child: ListView(
                    padding: const EdgeInsets.fromLTRB(TokSpace.s4, TokSpace.s2, TokSpace.s4, TokSpace.s8),
                    children: [
                      if (paged.cachedAt != null)
                        Padding(
                          padding: const EdgeInsets.only(bottom: TokSpace.s3),
                          child: OfflineBanner(savedAt: paged.cachedAt!, onRetry: () => ref.invalidate(provider)),
                        ),
                      if (paged.items.isEmpty)
                        _mosque.isAll
                            ? const StatePlaceholder(
                                icon: Icons.campaign_outlined,
                                title: 'Nog geen nieuws',
                                message: 'Nieuws en aankondigingen van moskeeën verschijnen hier.',
                              )
                            : StatePlaceholder(
                                icon: Icons.filter_alt_off_outlined,
                                title: 'Geen nieuws gevonden',
                                message: 'Er is nu geen nieuws van deze moskee.',
                                actionLabel: 'Nieuws van alle moskeeën',
                                onAction: () => setState(() => _mosque = MosqueFilter.all),
                              ),
                      for (final announcement in paged.items)
                        Padding(
                          padding: const EdgeInsets.only(bottom: TokSpace.s3),
                          child: AnnouncementCard(
                            announcement: announcement,
                            onTap: () => context.push('/aankondiging/${announcement.id}'),
                          ),
                        ),
                      if (paged.loadMoreFailed)
                        const Padding(
                          padding: EdgeInsets.only(bottom: TokSpace.s3),
                          child: StatusMessage(kind: StatusKind.error, message: 'Meer nieuws laden is niet gelukt.'),
                        ),
                      if (paged.hasMore)
                        Center(
                          child: paged.loadingMore
                              ? const Padding(padding: EdgeInsets.all(TokSpace.s3), child: CircularProgressIndicator())
                              : OutlinedButton(
                                  onPressed: () => ref.read(provider.notifier).loadMore(),
                                  child: const Text('Meer nieuws laden'),
                                ),
                        ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
