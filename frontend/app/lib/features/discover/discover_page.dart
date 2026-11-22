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
import '../mosques/mosque_widgets.dart';
import '../profile/follow_card.dart';
import 'discover_providers.dart';

/// Moskeeën ontdekken: zoeken op naam, filteren op plaats, een profiel openen en volgen.
class DiscoverPage extends ConsumerStatefulWidget {
  const DiscoverPage({super.key});

  @override
  ConsumerState<DiscoverPage> createState() => _DiscoverPageState();
}

class _DiscoverPageState extends ConsumerState<DiscoverPage> {
  final _search = TextEditingController();
  Timer? _debounce;
  MosqueQuery _query = (text: '', city: null);

  @override
  void dispose() {
    _debounce?.cancel();
    _search.dispose();
    super.dispose();
  }

  void _setText(String value) => setState(() => _query = (text: value.trim(), city: _query.city));

  void _onSearchChanged(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), () {
      if (mounted) _setText(value);
    });
    setState(() {}); // toont of verbergt de wisknop
  }

  void _clear() {
    _debounce?.cancel();
    _search.clear();
    setState(() => _query = (text: '', city: null));
  }

  @override
  Widget build(BuildContext context) {
    final provider = mosqueListProvider(_query);
    final cities = ref.watch(citiesProvider).value?.data ?? const <String>[];
    return Scaffold(
      appBar: AppBar(title: const Text('Moskeeën ontdekken')),
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
                  _setText(value);
                },
                textInputAction: TextInputAction.search,
                decoration: InputDecoration(
                  labelText: 'Zoek een moskee op naam',
                  prefixIcon: const Icon(Icons.search),
                  suffixIcon: _search.text.isEmpty
                      ? null
                      : IconButton(
                          tooltip: 'Zoekopdracht wissen',
                          icon: const Icon(Icons.close),
                          onPressed: () {
                            _debounce?.cancel();
                            _search.clear();
                            _setText('');
                          },
                        ),
                ),
              ),
            ),
          ),
          if (cities.length > 1)
            ChoiceChipRow<String?>(
              semanticLabel: 'Filter op plaats',
              options: [(null, 'Alle plaatsen'), for (final city in cities) (city, city)],
              selected: _query.city,
              onSelected: (city) => setState(() => _query = (text: _query.text, city: city)),
            ),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () => ref.refresh(provider.future),
              child: AsyncView(
                value: ref.watch(provider),
                onRetry: () => ref.invalidate(provider),
                loading: ListView(
                  padding: const EdgeInsets.all(TokSpace.s4),
                  children: [
                    for (var i = 0; i < 4; i++)
                      const Padding(
                        padding: EdgeInsets.only(bottom: TokSpace.s3),
                        child: Skeleton(height: 72),
                      ),
                  ],
                ),
                data: (paged) => _MosqueList(
                  paged: paged,
                  filtered: _query.text.isNotEmpty || _query.city != null,
                  onClear: _clear,
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

class _MosqueList extends StatelessWidget {
  const _MosqueList({
    required this.paged,
    required this.filtered,
    required this.onClear,
    required this.onLoadMore,
    required this.onRetry,
  });

  final Paged<Mosque> paged;
  final bool filtered;
  final VoidCallback onClear;
  final VoidCallback onLoadMore;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return ContentWidth(
      child: ListView(
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
                    title: 'Geen moskee gevonden',
                    message: 'Probeer een andere naam of plaats.',
                    actionLabel: 'Alle moskeeën tonen',
                    onAction: onClear,
                  )
                : const StatePlaceholder(
                    icon: Icons.mosque_outlined,
                    title: 'Nog geen moskeeën',
                    message: 'Zodra er moskeeën zijn aangesloten, vind je ze hier.',
                  )
          else ...[
            for (final mosque in paged.items)
              Padding(
                padding: const EdgeInsets.only(bottom: TokSpace.s3),
                child: MosqueTile(mosque: mosque),
              ),
            if (paged.loadMoreFailed)
              const Padding(
                padding: EdgeInsets.only(bottom: TokSpace.s3),
                child: StatusMessage(kind: StatusKind.error, message: 'Meer moskeeën laden is niet gelukt.'),
              ),
            if (paged.hasMore)
              Center(
                child: paged.loadingMore
                    ? const Padding(padding: EdgeInsets.all(TokSpace.s3), child: CircularProgressIndicator())
                    : OutlinedButton(onPressed: onLoadMore, child: const Text('Meer moskeeën laden')),
              ),
          ],
        ],
      ),
    );
  }
}

/// Een moskee in een lijst: logo, naam en plaats, met een knop om te volgen. Tikken opent het profiel.
class MosqueTile extends StatelessWidget {
  const MosqueTile({super.key, required this.mosque});

  final Mosque mosque;

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: TokSpace.s4, vertical: TokSpace.s2),
        leading: MosqueAvatar(name: mosque.name, logoUrl: mosque.logoUrl, size: 48),
        title: Text(mosque.name),
        subtitle: mosque.city == null ? null : Text(mosque.city!),
        trailing: FollowButton(mosque: mosque.summary, compact: true),
        onTap: () => context.push('/moskee/${Uri.encodeComponent(mosque.slug)}'),
      ),
    );
  }
}
