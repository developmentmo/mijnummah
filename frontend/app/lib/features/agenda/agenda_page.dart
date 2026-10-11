import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/design/design.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/choice_chip_row.dart';
import '../../core/widgets/content_width.dart';
import '../../core/widgets/offline_banner.dart';
import '../../data/models.dart';
import '../../data/paged.dart';
import 'agenda_providers.dart';
import 'event_card.dart';

/// Overzicht van aankomende activiteiten, te filteren op categorie.
class AgendaPage extends ConsumerStatefulWidget {
  const AgendaPage({super.key, this.initialCategory});

  final EventCategory? initialCategory;

  @override
  ConsumerState<AgendaPage> createState() => _AgendaPageState();
}

class _AgendaPageState extends ConsumerState<AgendaPage> {
  late EventCategory? _category = widget.initialCategory;

  @override
  void didUpdateWidget(AgendaPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.initialCategory != oldWidget.initialCategory) _category = widget.initialCategory;
  }

  void _select(EventCategory? category) {
    setState(() => _category = category);
    context.go(category == null ? '/agenda' : '/agenda?categorie=${category.apiValue}');
  }

  @override
  Widget build(BuildContext context) {
    final provider = agendaProvider(_category);
    final value = ref.watch(provider);
    return Scaffold(
      appBar: AppBar(title: const Text('Agenda')),
      body: Column(
        children: [
          _CategoryFilter(selected: _category, onSelected: _select),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () => ref.refresh(provider.future),
              child: AsyncView(
                value: value,
                onRetry: () => ref.invalidate(provider),
                loading: ListView(
                  padding: const EdgeInsets.all(TokSpace.s4),
                  children: [
                    for (var i = 0; i < 4; i++)
                      const Padding(
                        padding: EdgeInsets.only(bottom: TokSpace.s3),
                        child: EventCardSkeleton(),
                      ),
                  ],
                ),
                data: (paged) => _EventList(
                  paged: paged,
                  category: _category,
                  onShowAll: () => _select(null),
                  onLoadMore: () => ref.read(provider.notifier).loadMore(),
                  onRetry: () => ref.invalidate(provider),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _CategoryFilter extends StatelessWidget {
  const _CategoryFilter({required this.selected, required this.onSelected});

  final EventCategory? selected;
  final ValueChanged<EventCategory?> onSelected;

  @override
  Widget build(BuildContext context) {
    return ChoiceChipRow<EventCategory?>(
      semanticLabel: 'Filter op categorie',
      options: [(null, 'Alles'), for (final category in EventCategory.values) (category, category.label)],
      selected: selected,
      onSelected: onSelected,
    );
  }
}

class _EventList extends StatelessWidget {
  const _EventList({
    required this.paged,
    required this.category,
    required this.onShowAll,
    required this.onLoadMore,
    required this.onRetry,
  });

  final Paged<Event> paged;
  final EventCategory? category;
  final VoidCallback onShowAll;
  final VoidCallback onLoadMore;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    if (paged.items.isEmpty) {
      return ListView(
        children: [
          if (paged.cachedAt != null)
            Padding(
              padding: const EdgeInsets.all(TokSpace.s4),
              child: OfflineBanner(savedAt: paged.cachedAt!, onRetry: onRetry),
            ),
          category == null
              ? const StatePlaceholder(
                  icon: Icons.event_available_outlined,
                  title: 'Nog geen activiteiten gepland',
                  message: 'Zodra de moskee nieuwe activiteiten plant, vind je ze hier.',
                )
              : StatePlaceholder(
                  icon: Icons.filter_alt_off_outlined,
                  title: 'Geen activiteiten in deze categorie',
                  message: 'Er staan nu geen activiteiten gepland in "${category!.label}".',
                  actionLabel: 'Alle activiteiten tonen',
                  onAction: onShowAll,
                ),
        ],
      );
    }
    return ContentWidth(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(TokSpace.s4, TokSpace.s2, TokSpace.s4, TokSpace.s8),
        children: [
          if (paged.cachedAt != null)
            Padding(
              padding: const EdgeInsets.only(bottom: TokSpace.s3),
              child: OfflineBanner(savedAt: paged.cachedAt!, onRetry: onRetry),
            ),
          for (final event in paged.items)
            Padding(
              padding: const EdgeInsets.only(bottom: TokSpace.s3),
              child: EventCard(event: event, onTap: () => context.push('/activiteit/${event.id}')),
            ),
          if (paged.loadMoreFailed)
            const Padding(
              padding: EdgeInsets.only(bottom: TokSpace.s3),
              child: StatusMessage(kind: StatusKind.error, message: 'Meer activiteiten laden is niet gelukt.'),
            ),
          if (paged.hasMore)
            Center(
              child: paged.loadingMore
                  ? const Padding(padding: EdgeInsets.all(TokSpace.s3), child: CircularProgressIndicator())
                  : OutlinedButton(onPressed: onLoadMore, child: const Text('Meer activiteiten laden')),
            ),
        ],
      ),
    );
  }
}
