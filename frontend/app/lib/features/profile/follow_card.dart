import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api/api_exception.dart';
import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../data/models.dart';
import 'follow_providers.dart';

/// Uitnodiging om de moskee te volgen. Verdwijnt zodra de gebruiker volgt; de voorkeuren staan bij Profiel.
class FollowInvitation extends ConsumerStatefulWidget {
  const FollowInvitation({super.key});

  @override
  ConsumerState<FollowInvitation> createState() => _FollowInvitationState();
}

class _FollowInvitationState extends ConsumerState<FollowInvitation> {
  bool _busy = false;

  Future<void> _follow(Mosque mosque) async {
    setState(() => _busy = true);
    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(meRepositoryProvider).follow(mosque.id);
      ref.invalidate(followStatusProvider);
      messenger.showSnackBar(SnackBar(content: Text('Je volgt nu ${mosque.name}.')));
    } on AppFailure {
      messenger.showSnackBar(const SnackBar(content: Text('Volgen is niet gelukt. Probeer het later opnieuw.')));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final mosque = ref.watch(mosqueProvider).value?.data;
    final signedIn = ref.watch(currentUserIdProvider) != null;
    final status = ref.watch(followStatusProvider);
    if (mosque == null || status.isLoading || status.hasError || status.value != null) return const SizedBox.shrink();

    final c = context.colors;
    final text = Theme.of(context).textTheme;
    return Card(
      color: c.primaryContainer,
      child: Padding(
        padding: const EdgeInsets.all(TokComponent.cardPadding),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.favorite_outline, color: c.onPrimaryContainer),
                const SizedBox(width: TokSpace.s2),
                Expanded(
                  child: Text('Volg ${mosque.name}', style: text.titleMedium?.copyWith(color: c.onPrimaryContainer)),
                ),
              ],
            ),
            const SizedBox(height: TokSpace.s2),
            Text(
              'Blijf op de hoogte van activiteiten, lezingen en nieuws. Volgen is gratis en is geen lidmaatschap '
              'van de moskee.',
              style: text.bodyMedium?.copyWith(color: c.onPrimaryContainer),
            ),
            const SizedBox(height: TokSpace.s3),
            signedIn
                ? FilledButton.icon(
                    onPressed: _busy ? null : () => _follow(mosque),
                    icon: const Icon(Icons.add),
                    label: const Text('Volgen'),
                  )
                : FilledButton(onPressed: () => context.push('/inloggen'), child: const Text('Inloggen om te volgen')),
          ],
        ),
      ),
    );
  }
}
