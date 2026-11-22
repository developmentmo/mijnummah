import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../core/widgets/choice_chip_row.dart';
import '../../core/widgets/network_image_box.dart';
import '../../data/models.dart';
import '../profile/follow_providers.dart';
import 'mosque_filter.dart';

/// Logo van een moskee in een cirkel; zonder logo de eerste letter van de naam. Decoratief.
class MosqueAvatar extends ConsumerWidget {
  const MosqueAvatar({super.key, required this.name, this.logoUrl, this.size = 40});

  final String name;
  final String? logoUrl;
  final double size;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.colors;
    final src = resolveImageUrl(logoUrl, ref.watch(appConfigProvider).apiBaseUrl);
    final initial = Center(
      child: Text(
        name.isEmpty ? '?' : name.characters.first.toUpperCase(),
        style: TextStyle(color: c.onPrimaryContainer, fontWeight: FontWeight.w600, fontSize: size * 0.42),
      ),
    );
    return ExcludeSemantics(
      child: Container(
        width: size,
        height: size,
        clipBehavior: Clip.antiAlias,
        decoration: BoxDecoration(color: c.primaryContainer, shape: BoxShape.circle),
        child: src == null ? initial : Image.network(src, fit: BoxFit.cover, errorBuilder: (_, _, _) => initial),
      ),
    );
  }
}

/// Naam van de moskee bij een activiteit, video of bericht, met een klein logo.
class MosqueLabel extends StatelessWidget {
  const MosqueLabel({super.key, required this.mosque});

  final MosqueSummary mosque;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Row(
      children: [
        MosqueAvatar(name: mosque.name, logoUrl: mosque.logoUrl, size: 20),
        const SizedBox(width: TokSpace.s2),
        Flexible(
          child: Text(
            mosque.name,
            style: text.labelMedium?.copyWith(color: context.colors.onSurfaceMuted),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }
}

/// Chips om een lijst te beperken tot alle, de gevolgde of één moskee. Zonder gevolgde moskeeën en zonder gekozen
/// moskee is er niets te kiezen en verschijnt de rij niet.
class MosqueFilterRow extends ConsumerWidget {
  const MosqueFilterRow({super.key, required this.selected, required this.onSelected});

  final MosqueFilter selected;
  final ValueChanged<MosqueFilter> onSelected;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final follows = ref.watch(myFollowsProvider).value ?? const <FollowStatus>[];
    final slug = selected.slug;
    final options = <(MosqueFilter, String)>[
      (MosqueFilter.all, 'Alle moskeeën'),
      if (follows.isNotEmpty || selected.followedOnly) (MosqueFilter.followed, 'Gevolgd'),
      for (final follow in follows) (MosqueFilter.only(follow.mosqueSlug), follow.mosqueName),
      if (slug != null && !follows.any((follow) => follow.mosqueSlug == slug))
        (selected, ref.watch(mosqueProvider(slug)).value?.data.name ?? slug),
    ];
    if (options.length == 1) return const SizedBox.shrink();
    return ChoiceChipRow<MosqueFilter>(
      semanticLabel: 'Filter op moskee',
      options: options,
      selected: selected,
      onSelected: onSelected,
    );
  }
}

/// Op een detailpagina: van welke moskee dit is, met een link naar haar profiel.
class MosqueLinkTile extends StatelessWidget {
  const MosqueLinkTile({super.key, required this.mosque});

  final MosqueSummary mosque;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      child: ListTile(
        leading: MosqueAvatar(name: mosque.name, logoUrl: mosque.logoUrl),
        title: Text(mosque.name),
        subtitle: const Text('Bekijk de moskee'),
        trailing: const Icon(Icons.chevron_right),
        onTap: () => context.push('/moskee/${Uri.encodeComponent(mosque.slug)}'),
      ),
    );
  }
}
