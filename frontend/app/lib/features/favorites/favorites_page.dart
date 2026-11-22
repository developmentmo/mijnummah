import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../core/widgets/content_width.dart';
import '../../core/widgets/failure_messages.dart';
import '../../core/widgets/section_header.dart';
import '../lectures/media_card.dart';
import '../mosques/mosque_widgets.dart';
import '../profile/follow_providers.dart';

/// Favoriete moskeeën en bewaarde video's.
class FavoritesPage extends ConsumerWidget {
  const FavoritesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final signedIn = ref.watch(currentUserIdProvider) != null;
    if (!signedIn) {
      return Scaffold(
        appBar: AppBar(title: const Text('Favorieten')),
        body: ListView(
          children: [
            StatePlaceholder(
              icon: Icons.favorite_outline,
              title: 'Log in voor favorieten',
              message: 'Met een account bewaar je favoriete moskeeën en video\'s.',
              actionLabel: 'Inloggen',
              onAction: () => context.push('/inloggen'),
            ),
          ],
        ),
      );
    }
    final text = Theme.of(context).textTheme;
    final follows = ref.watch(myFollowsProvider);
    final media = ref.watch(favoriteMediaProvider);
    final favorites = follows.value?.where((follow) => follow.favorite).toList();

    Widget failed(Object error, VoidCallback onRetry) {
      final failure = describeFailure(error);
      return StatusMessage(
        kind: StatusKind.error,
        title: failure.title,
        message: failure.message,
        actionLabel: 'Opnieuw proberen',
        onAction: onRetry,
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Favorieten')),
      body: RefreshIndicator(
        onRefresh: () async {
          ref
            ..invalidate(myFollowsProvider)
            ..invalidate(favoriteMediaProvider);
          await ref.read(favoriteMediaProvider.future);
        },
        child: ListView(
          padding: const EdgeInsets.fromLTRB(TokSpace.s4, 0, TokSpace.s4, TokSpace.s8),
          children: [
            ContentWidth(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SectionHeader(title: 'Favoriete moskeeën'),
                  if (follows.hasError && !follows.isLoading)
                    failed(follows.error!, () => ref.invalidate(myFollowsProvider))
                  else if (favorites == null)
                    const Skeleton(height: 72)
                  else if (favorites.isEmpty)
                    Text(
                      'Tik op de ster bij een moskee die je volgt, dan staat ze hier en bovenaan op Home.',
                      style: text.bodyMedium,
                    )
                  else
                    Card(
                      child: Column(
                        children: [
                          for (final (index, follow) in favorites.indexed) ...[
                            if (index > 0) const Divider(height: 1),
                            ListTile(
                              leading: MosqueAvatar(name: follow.mosqueName, logoUrl: follow.mosqueLogoUrl),
                              title: Text(follow.mosqueName),
                              trailing: const Icon(Icons.chevron_right),
                              onTap: () => context.push('/moskee/${Uri.encodeComponent(follow.mosqueSlug)}'),
                            ),
                          ],
                        ],
                      ),
                    ),
                  const SectionHeader(title: 'Bewaarde video\'s'),
                  if (media.hasError && !media.isLoading)
                    failed(media.error!, () => ref.invalidate(favoriteMediaProvider))
                  else if (!media.hasValue)
                    const ResponsiveCardGrid(children: [MediaCardSkeleton(), MediaCardSkeleton()])
                  else if (media.requireValue.items.isEmpty)
                    Text('Bewaar een video met het hartje, dan vind je hem hier terug.', style: text.bodyMedium)
                  else
                    ResponsiveCardGrid(
                      children: [
                        for (final item in media.requireValue.items)
                          MediaCard(item: item, onTap: () => context.push('/lezing/${item.id}')),
                      ],
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
