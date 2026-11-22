import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api/api_exception.dart';
import '../../core/providers.dart';
import '../../data/models.dart';
import 'follow_providers.dart';
import 'push_permission_notice.dart';

/// Knop om een moskee te volgen of te ontvolgen. Een bezoeker zonder account gaat naar inloggen. Volgen is geen
/// lidmaatschap; de meldingsvoorkeuren staan bij Profiel.
class FollowButton extends ConsumerStatefulWidget {
  const FollowButton({super.key, required this.mosque, this.compact = false});

  final MosqueSummary mosque;

  /// Kleine knop, voor in een lijst.
  final bool compact;

  @override
  ConsumerState<FollowButton> createState() => _FollowButtonState();
}

class _FollowButtonState extends ConsumerState<FollowButton> {
  bool _busy = false;

  Future<void> _toggle(bool following) async {
    setState(() => _busy = true);
    final messenger = ScaffoldMessenger.of(context);
    final container = ProviderScope.containerOf(context, listen: false);
    final name = widget.mosque.name;
    try {
      if (following) {
        await ref.unfollow(widget.mosque.id);
        messenger.showSnackBar(SnackBar(content: Text('Je volgt $name niet meer.')));
      } else {
        await ref.follow(widget.mosque.id);
        messenger.showSnackBar(SnackBar(content: Text('Je volgt nu $name.')));
        await enablePush(container);
      }
    } on AppFailure {
      messenger.showSnackBar(
        SnackBar(
          content: Text(following ? 'Ontvolgen is niet gelukt.' : 'Volgen is niet gelukt. Probeer het later opnieuw.'),
        ),
      );
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final signedIn = ref.watch(currentUserIdProvider) != null;
    final status = ref.watch(followOfProvider(widget.mosque.id));
    final following = status.value != null;
    final busy = _busy || (signedIn && status.isLoading && !status.hasValue);
    final onPressed = !signedIn
        ? () => context.push('/inloggen')
        : busy
        ? null
        : () => _toggle(following);
    final label = Text(following ? 'Volgend' : 'Volgen');
    if (following) {
      return widget.compact
          ? OutlinedButton(onPressed: onPressed, child: label)
          : OutlinedButton.icon(onPressed: onPressed, icon: const Icon(Icons.check), label: label);
    }
    return widget.compact
        ? FilledButton.tonal(onPressed: onPressed, child: label)
        : FilledButton.icon(onPressed: onPressed, icon: const Icon(Icons.add), label: label);
  }
}
