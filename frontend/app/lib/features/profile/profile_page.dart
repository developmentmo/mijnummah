import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api/api_exception.dart';
import '../../core/design/design.dart';
import '../../core/format/dates.dart';
import '../../core/providers.dart';
import '../../core/widgets/content_width.dart';
import '../../core/widgets/section_header.dart';
import '../../data/models.dart';
import '../agenda/agenda_providers.dart';
import 'follow_card.dart';
import 'follow_providers.dart';

/// Profiel: account, volgen met meldingsvoorkeuren, eigen aanmeldingen en de weergave-instelling.
class ProfilePage extends ConsumerWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    final user = auth.value;
    return Scaffold(
      appBar: AppBar(title: const Text('Profiel')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(TokSpace.s4, TokSpace.s2, TokSpace.s4, TokSpace.s8),
        children: [
          ContentWidth(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                if (auth.isLoading && !auth.hasValue)
                  const Skeleton(height: 120)
                else if (user == null)
                  const _Visitor()
                else ...[
                  _Account(user: user),
                  const SectionHeader(title: 'Volgen'),
                  const _FollowSection(),
                  const SectionHeader(title: 'Mijn aanmeldingen'),
                  const _MyRegistrations(),
                ],
                const SectionHeader(title: 'Volgen is geen lidmaatschap'),
                _Statuses(signedIn: user != null),
                const SectionHeader(title: 'Weergave'),
                const _ThemeSetting(),
                if (kDebugMode) ...[
                  const SectionHeader(title: 'Ontwikkeling'),
                  ListTile(
                    leading: const Icon(Icons.palette_outlined),
                    title: const Text('Designpreview'),
                    subtitle: const Text('Alleen zichtbaar in debug-builds'),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () => context.push('/design'),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Visitor extends StatelessWidget {
  const _Visitor();

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(TokComponent.cardPadding),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Semantics(header: true, child: Text('Je bent bezoeker', style: text.titleLarge)),
            const SizedBox(height: TokSpace.s2),
            Text(
              'Openbare activiteiten, lezingen en aankondigingen kun je zonder account bekijken. Met een account kun '
              'je je aanmelden voor activiteiten en de moskee volgen.',
              style: text.bodyMedium,
            ),
            const SizedBox(height: TokSpace.s4),
            FilledButton(onPressed: () => context.push('/inloggen'), child: const Text('Inloggen')),
            const SizedBox(height: TokSpace.s3),
            Text(
              'Zelf een account aanmaken kan nog niet in deze versie van de app. Heb je nog geen account, vraag er '
              'dan een aan bij de moskee.',
              style: text.bodySmall,
            ),
          ],
        ),
      ),
    );
  }
}

class _Account extends ConsumerWidget {
  const _Account({required this.user});

  final UserProfile user;

  Future<void> _logout(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Uitloggen?'),
        content: const Text('Je kunt daarna nog steeds alle openbare content bekijken.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuleren')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Uitloggen')),
        ],
      ),
    );
    if (confirmed == true) await ref.read(authControllerProvider.notifier).logout();
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.colors;
    final text = Theme.of(context).textTheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(TokComponent.cardPadding),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                CircleAvatar(
                  backgroundColor: c.primaryContainer,
                  foregroundColor: c.onPrimaryContainer,
                  child: Text(user.displayName.characters.first.toUpperCase()),
                ),
                const SizedBox(width: TokSpace.s3),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(user.displayName, style: text.titleMedium),
                      Text(user.email, style: text.bodySmall),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: TokSpace.s3),
            OutlinedButton.icon(
              onPressed: () => _logout(context, ref),
              icon: const Icon(Icons.logout),
              label: const Text('Uitloggen'),
            ),
          ],
        ),
      ),
    );
  }
}

class _FollowSection extends ConsumerStatefulWidget {
  const _FollowSection();

  @override
  ConsumerState<_FollowSection> createState() => _FollowSectionState();
}

class _FollowSectionState extends ConsumerState<_FollowSection> {
  NotificationTopic? _saving;
  bool _unfollowing = false;

  Future<void> _toggle(FollowStatus status, NotificationTopic topic, bool enabled) async {
    setState(() => _saving = topic);
    try {
      await ref.read(meRepositoryProvider).updateNotification(status.mosqueId, topic, enabled);
      ref.invalidate(followStatusProvider);
      await ref.read(followStatusProvider.future);
    } on AppFailure {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Je voorkeur kon niet worden opgeslagen. Probeer het opnieuw.')));
      }
    } finally {
      if (mounted) setState(() => _saving = null);
    }
  }

  Future<void> _unfollow(FollowStatus status, String mosqueName) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('$mosqueName ontvolgen?'),
        content: const Text('Je meldingsvoorkeuren worden dan ook verwijderd. Je kunt later altijd opnieuw volgen.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuleren')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Ontvolgen')),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    setState(() => _unfollowing = true);
    try {
      await ref.read(meRepositoryProvider).unfollow(status.mosqueId);
      ref.invalidate(followStatusProvider);
    } on AppFailure {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Ontvolgen is niet gelukt. Probeer het opnieuw.')));
      }
    } finally {
      if (mounted) setState(() => _unfollowing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final status = ref.watch(followStatusProvider);
    final mosqueName = ref.watch(mosqueProvider).value?.data.name ?? 'de moskee';
    if (status.hasError && !status.isLoading) {
      return StatusMessage(
        kind: StatusKind.error,
        message: 'Je volgstatus kon niet worden geladen.',
        actionLabel: 'Opnieuw proberen',
        onAction: () => ref.invalidate(followStatusProvider),
      );
    }
    if (!status.hasValue) return const Skeleton(height: 96);
    final follow = status.value;
    if (follow == null) return const FollowInvitation();

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(TokComponent.cardPadding),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Je volgt $mosqueName sinds ${Dates.date(follow.followedSince)}.', style: text.bodyMedium),
            const SizedBox(height: TokSpace.s3),
            Text('Waarover wil je meldingen ontvangen?', style: text.titleSmall),
            for (final topic in NotificationTopic.values)
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(topic.label),
                value: follow.notifications[topic] ?? false,
                onChanged: _saving != null ? null : (enabled) => _toggle(follow, topic, enabled),
              ),
            const StatusMessage(
              kind: StatusKind.info,
              message:
                  'Je voorkeuren worden bewaard. Pushmeldingen worden in deze versie van de app nog niet verstuurd.',
            ),
            const SizedBox(height: TokSpace.s3),
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton(
                onPressed: _unfollowing ? null : () => _unfollow(follow, mosqueName),
                child: const Text('Ontvolgen'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _MyRegistrations extends ConsumerWidget {
  const _MyRegistrations();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final value = ref.watch(myRegistrationsProvider);
    final slug = ref.watch(appConfigProvider).mosqueSlug;
    if (value.hasError && !value.isLoading) {
      return StatusMessage(
        kind: StatusKind.error,
        message: 'Je aanmeldingen konden niet worden geladen.',
        actionLabel: 'Opnieuw proberen',
        onAction: () => ref.invalidate(myRegistrationsProvider),
      );
    }
    if (!value.hasValue) return const Skeleton(height: 72);
    final now = DateTime.now();
    final upcoming = value.requireValue.where((r) => r.mosqueSlug == slug && r.eventStartsAt.isAfter(now)).toList()
      ..sort((a, b) => a.eventStartsAt.compareTo(b.eventStartsAt));
    if (upcoming.isEmpty) {
      return Text(
        'Je bent nog niet aangemeld voor een komende activiteit. In de agenda zie je bij elke activiteit of je je '
        'kunt aanmelden.',
        style: Theme.of(context).textTheme.bodyMedium,
      );
    }
    return Card(
      child: Column(
        children: [
          for (final (index, registration) in upcoming.indexed) ...[
            if (index > 0) const Divider(height: 1),
            ListTile(
              leading: const Icon(Icons.event_available_outlined),
              title: Text(registration.eventTitle),
              subtitle: Text(Dates.compact(registration.eventStartsAt)),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => context.push('/activiteit/${registration.eventId}'),
            ),
          ],
        ],
      ),
    );
  }
}

/// Legt uit wat bezoeker, account, volger en lid betekenen, en welke status de gebruiker nu heeft.
class _Statuses extends ConsumerWidget {
  const _Statuses({required this.signedIn});

  final bool signedIn;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final following = ref.watch(followStatusProvider).value != null;
    final current = !signedIn ? 0 : (following ? 2 : 1);
    const items = [
      (Icons.visibility_outlined, 'Bezoeker', 'Bekijkt openbare content, zonder account.'),
      (Icons.person_outline, 'Account', 'Kan zich aanmelden voor activiteiten en de moskee volgen.'),
      (Icons.favorite_outline, 'Volger', 'Blijft op de hoogte van nieuws en activiteiten. Volgen is gratis.'),
      (
        Icons.card_membership_outlined,
        'Lid',
        'Officieel lid van de moskee. Lidmaatschap regel je rechtstreeks bij de moskee, niet via de app.',
      ),
    ];
    return Card(
      child: Column(
        children: [
          for (final (index, (icon, title, description)) in items.indexed) ...[
            if (index > 0) const Divider(height: 1),
            ListTile(
              leading: Icon(icon),
              title: Text(title),
              subtitle: Text(description),
              trailing: index == current
                  ? Semantics(label: 'Jouw status', child: const Icon(Icons.check_circle))
                  : null,
              selected: index == current,
            ),
          ],
        ],
      ),
    );
  }
}

class _ThemeSetting extends ConsumerWidget {
  const _ThemeSetting();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final mode = ref.watch(themeModeProvider);
    const options = [(ThemeMode.system, 'Zoals het toestel'), (ThemeMode.light, 'Licht'), (ThemeMode.dark, 'Donker')];
    return Wrap(
      spacing: TokSpace.s2,
      runSpacing: TokSpace.s2,
      children: [
        for (final (value, label) in options)
          ChoiceChip(
            label: Text(label),
            selected: mode == value,
            onSelected: (_) => ref.read(themeModeProvider.notifier).set(value),
          ),
      ],
    );
  }
}
