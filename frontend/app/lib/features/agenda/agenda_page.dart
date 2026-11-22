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
import '../mosques/mosque_filter.dart';
import '../mosques/mosque_widgets.dart';
import 'agenda_providers.dart';
import 'event_card.dart';

/// Overzicht van aankomende activiteiten van alle moskeeën, te filteren op moskee, periode en categorie.
class AgendaPage extends ConsumerStatefulWidget {
  const AgendaPage({
    super.key,
    this.initialCategory,
    this.initialMosque = MosqueFilter.all,
    this.initialPeriod = AgendaPeriod.all,
  });

  final EventCategory? initialCategory;
  final MosqueFilter initialMosque;
  final AgendaPeriod initialPeriod;

  @override
  ConsumerState<AgendaPage> createState() => _AgendaPageState();
}

class _AgendaPageState extends ConsumerState<AgendaPage> {
  late AgendaQuery _query = _initial;

  AgendaQuery get _initial =>
      (mosque: widget.initialMosque, category: widget.initialCategory, period: widget.initialPeriod);

  @override
  void didUpdateWidget(AgendaPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.initialCategory != oldWidget.initialCategory ||
        widget.initialMosque != oldWidget.initialMosque ||
        widget.initialPeriod != oldWidget.initialPeriod) {
      _query = _initial;
    }
  }

  /// Zet de filters ook in het adres, zodat een link (of de knop "Hele agenda" bij een moskee) dezelfde lijst opent.
  void _apply(AgendaQuery query) {
    setState(() => _query = query);
    final params = {
      'moskee': ?query.mosque.queryValue,
      if (query.period != AgendaPeriod.all) 'periode': query.period.queryValue,
      'categorie': ?query.category?.apiValue,
    };
    context.go(Uri(path: '/agenda', queryParameters: params.isEmpty ? null : params).toString());
  }

  @override
  Widget build(BuildContext context) {
    final provider = agendaProvider(_query);
    final value = ref.watch(provider);
    final filtered = _query.category != null || _query.period != AgendaPeriod.all || !_query.mosque.isAll;
    return Scaffold(
      appBar: AppBar(title: const Text('Agenda')),
      body: Column(
        children: [
          MosqueFilterRow(
            selected: _query.mosque,
            onSelected: (mosque) => _apply((mosque: mosque, category: _query.category, period: _query.period)),
          ),
          ChoiceChipRow<AgendaPeriod>(
            semanticLabel: 'Filter op periode',
            options: [for (final period in AgendaPeriod.values) (period, period.label)],
            selected: _query.period,
            onSelected: (period) => _apply((mosque: _query.mosque, category: _query.category, period: period)),
          ),
          _CategoryFilter(
            selected: _query.category,
            onSelected: (category) => _apply((mosque: _query.mosque, category: category, period: _query.period)),
          ),
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
                  filtered: filtered,
                  onShowAll: () => _apply((mosque: MosqueFilter.all, category: null, period: AgendaPeriod.all)),
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
    required this.filtered,
    required this.onShowAll,
    required this.onLoadMore,
    required this.onRetry,
  });

  final Paged<Event> paged;
  final bool filtered;
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
          filtered
              ? StatePlaceholder(
                  icon: Icons.filter_alt_off_outlined,
                  title: 'Geen activiteiten gevonden',
                  message: 'Er staan geen activiteiten gepland die bij je filters passen.',
                  actionLabel: 'Alle activiteiten tonen',
                  onAction: onShowAll,
                )
              : const StatePlaceholder(
                  icon: Icons.event_available_outlined,
                  title: 'Nog geen activiteiten gepland',
                  message: 'Zodra moskeeën nieuwe activiteiten plannen, vind je ze hier.',
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
