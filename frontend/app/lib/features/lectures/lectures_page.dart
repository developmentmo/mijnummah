import 'dart:async';

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
import 'lectures_providers.dart';
import 'media_card.dart';

/// De videobibliotheek van alle moskeeën: zoeken en filteren op categorie en moskee.
class LecturesPage extends ConsumerStatefulWidget {
  const LecturesPage({super.key, this.initialMosque = MosqueFilter.all});

  final MosqueFilter initialMosque;

  @override
  ConsumerState<LecturesPage> createState() => _LecturesPageState();
}

class _LecturesPageState extends ConsumerState<LecturesPage> {
  final _search = TextEditingController();
  Timer? _debounce;
  late LibraryQuery _query = (text: '', category: null, mosque: widget.initialMosque);

  @override
  void didUpdateWidget(LecturesPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.initialMosque != oldWidget.initialMosque) {
      _query = (text: _query.text, category: _query.category, mosque: widget.initialMosque);
    }
  }

  void _setText(String text) => setState(() => _query = (text: text, category: _query.category, mosque: _query.mosque));

  void _selectMosque(MosqueFilter mosque) {
    setState(() => _query = (text: _query.text, category: _query.category, mosque: mosque));
    final value = mosque.queryValue;
    context.go(value == null ? '/lezingen' : '/lezingen?moskee=${Uri.encodeQueryComponent(value)}');
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _search.dispose();
    super.dispose();
  }

  void _onSearchChanged(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), () {
      if (mounted) _setText(value.trim());
    });
    setState(() {}); // toont of verbergt de wisknop
  }

  void _clearSearch() {
    _debounce?.cancel();
    _search.clear();
    _setText('');
  }

  void _selectCategory(MediaCategory? category) =>
      setState(() => _query = (text: _query.text, category: category, mosque: _query.mosque));

  @override
  Widget build(BuildContext context) {
    final provider = libraryProvider(_query);
    return Scaffold(
      appBar: AppBar(title: const Text('Lezingen')),
      body: Column(
        children: [
          ContentWidth(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(TokSpace.s4, TokSpace.s2, TokSpace.s4, 0),
              child: TextField(
                controller: _search,
                onChanged: _onSearchChanged,
                onSubmitted: (value) {
                  _debounce?.cancel();
                  _setText(value.trim());
                },
                textInputAction: TextInputAction.search,
                decoration: InputDecoration(
                  labelText: 'Zoek op titel of spreker',
                  prefixIcon: const Icon(Icons.search),
                  suffixIcon: _search.text.isEmpty
                      ? null
                      : IconButton(
                          tooltip: 'Zoekopdracht wissen',
                          icon: const Icon(Icons.close),
                          onPressed: _clearSearch,
                        ),
                ),
              ),
            ),
          ),
          MosqueFilterRow(selected: _query.mosque, onSelected: _selectMosque),
          _CategoryFilter(selected: _query.category, onSelected: _selectCategory),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () => ref.refresh(provider.future),
              child: AsyncView(
                value: ref.watch(provider),
                onRetry: () => ref.invalidate(provider),
                loading: ListView(
                  padding: const EdgeInsets.all(TokSpace.s4),
                  children: const [
                    ResponsiveCardGrid(children: [MediaCardSkeleton(), MediaCardSkeleton(), MediaCardSkeleton()]),
                  ],
                ),
                data: (paged) => _Results(
                  paged: paged,
                  query: _query,
                  onClear: () {
                    _clearSearch();
                    _selectCategory(null);
                    if (!_query.mosque.isAll) _selectMosque(MosqueFilter.all);
                  },
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

  final MediaCategory? selected;
  final ValueChanged<MediaCategory?> onSelected;

  @override
  Widget build(BuildContext context) {
    return ChoiceChipRow<MediaCategory?>(
      semanticLabel: 'Filter op categorie',
      options: [(null, 'Alles'), for (final category in MediaCategory.values) (category, category.label)],
      selected: selected,
      onSelected: onSelected,
    );
  }
}

class _Results extends StatelessWidget {
  const _Results({
    required this.paged,
    required this.query,
    required this.onClear,
    required this.onLoadMore,
    required this.onRetry,
  });

  final Paged<MediaItem> paged;
  final LibraryQuery query;
  final VoidCallback onClear;
  final VoidCallback onLoadMore;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final filtered = query.text.isNotEmpty || query.category != null || !query.mosque.isAll;
    return ListView(
      padding: const EdgeInsets.fromLTRB(TokSpace.s4, TokSpace.s2, TokSpace.s4, TokSpace.s8),
      children: [
        if (paged.cachedAt != null)
          Padding(
            padding: const EdgeInsets.only(bottom: TokSpace.s3),
            child: OfflineBanner(savedAt: paged.cachedAt!, onRetry: onRetry),
          ),
        if (paged.items.isEmpty)
          filtered
              ? StatePlaceholder(
                  icon: Icons.search_off,
                  title: 'Niets gevonden',
                  message: query.text.isEmpty
                      ? 'Er zijn nog geen video\'s die bij je filters passen.'
                      : 'Er zijn geen video\'s die passen bij "${query.text}".',
                  actionLabel: 'Alles tonen',
                  onAction: onClear,
                )
              : const StatePlaceholder(
                  icon: Icons.video_library_outlined,
                  title: 'Nog geen lezingen',
                  message: 'Opgenomen lezingen en activiteiten verschijnen hier zodra moskeeën ze hebben toegevoegd.',
                )
        else ...[
          if (filtered)
            Padding(
              padding: const EdgeInsets.only(bottom: TokSpace.s3),
              child: Semantics(
                liveRegion: true,
                child: Text(
                  paged.totalItems == 1 ? '1 resultaat' : '${paged.totalItems} resultaten',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
            ),
          ResponsiveCardGrid(
            children: [
              for (final item in paged.items) MediaCard(item: item, onTap: () => context.push('/lezing/${item.id}')),
            ],
          ),
          const SizedBox(height: TokSpace.s3),
          if (paged.loadMoreFailed)
            const Padding(
              padding: EdgeInsets.only(bottom: TokSpace.s3),
              child: StatusMessage(kind: StatusKind.error, message: 'Meer video\'s laden is niet gelukt.'),
            ),
          if (paged.hasMore)
            Center(
              child: paged.loadingMore
                  ? const Padding(padding: EdgeInsets.all(TokSpace.s3), child: CircularProgressIndicator())
                  : OutlinedButton(onPressed: onLoadMore, child: const Text('Meer laden')),
            ),
        ],
      ],
    );
  }
}
