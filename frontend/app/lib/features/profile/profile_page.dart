import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api/api_exception.dart';
import '../../core/brand/brand.g.dart';
import '../../core/design/design.dart';
import '../../core/format/dates.dart';
import '../../core/providers.dart';
import '../../core/widgets/content_width.dart';
import '../../core/widgets/external_links.dart';
import '../../core/widgets/section_header.dart';
import '../../data/models.dart';
import '../agenda/agenda_providers.dart';
import '../mosques/mosque_widgets.dart';
import 'follow_providers.dart';
import 'push_permission_notice.dart';

/// Profiel: account, meldingen en favorieten, gevolgde moskeeën met meldingsvoorkeuren, eigen aanmeldingen en de weergave-instelling.
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
                  const SizedBox(height: TokSpace.s3),
                  Card(
                    child: Column(
                      children: [
                        ListTile(
                          leading: const Icon(Icons.notifications_outlined),
                          title: const Text('Meldingen'),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () => context.push('/meldingen'),
                        ),
                        const Divider(height: 1),
                        ListTile(
                          leading: const Icon(Icons.favorite_outline),
                          title: const Text('Favorieten'),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () => context.push('/favorieten'),
                        ),
                      ],
                    ),
                  ),
                  const SectionHeader(title: 'Moskeeën die je volgt'),
                  const _FollowSection(),
                  const SectionHeader(title: 'Mijn aanmeldingen'),
                  const _MyRegistrations(),
                ],
                const SectionHeader(title: 'Volgen is geen lidmaatschap'),
                _Statuses(signedIn: user != null),
                const SectionHeader(title: 'Weergave'),
                const _ThemeSetting(),
                if (Brand.privacyPolicyUrl case final url?) ...[
                  const SectionHeader(title: 'Privacy'),
                  PrivacyPolicyTile(url: Uri.parse(url)),
                ],
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

/// Link naar de privacyverklaring van het platform (uit de merkconfiguratie, `links.privacyPolicyUrl`).
class PrivacyPolicyTile extends StatelessWidget {
  const PrivacyPolicyTile({super.key, required this.url});

  final Uri url;

  @override
  Widget build(BuildContext context) => ListTile(
    leading: const Icon(Icons.privacy_tip_outlined),
    title: const Text('Privacyverklaring'),
    subtitle: Text(url.host),
    trailing: const Icon(Icons.open_in_new),
    onTap: () => openExternal(context, url),
  );
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
              'je je aanmelden voor activiteiten, moskeeën volgen en video\'s bewaren.',
              style: text.bodyMedium,
            ),
            const SizedBox(height: TokSpace.s4),
            FilledButton(onPressed: () => context.push('/inloggen'), child: const Text('Inloggen')),
            const SizedBox(height: TokSpace.s3),
            OutlinedButton(onPressed: () => context.push('/account-aanmaken'), child: const Text('Account aanmaken')),
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
              onPressed: () => context.push('/account'),
              icon: const Icon(Icons.manage_accounts_outlined),
              label: const Text('Accountinstellingen'),
            ),
            const SizedBox(height: TokSpace.s2),
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

/// De gevolgde moskeeën, elk met favoriet, meldingsvoorkeuren en ontvolgen.
class _FollowSection extends ConsumerWidget {
  const _FollowSection();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final follows = ref.watch(myFollowsProvider);
    if (follows.hasError && !follows.isLoading) {
      return StatusMessage(
        kind: StatusKind.error,
        message: 'De moskeeën die je volgt konden niet worden geladen.',
        actionLabel: 'Opnieuw proberen',
        onAction: () => ref.invalidate(myFollowsProvider),
      );
    }
    if (!follows.hasValue) return const Skeleton(height: 96);
    final list = follows.requireValue;
    if (list.isEmpty) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'Je volgt nog geen moskeeën. Volg een moskee om haar activiteiten en nieuws op Home te zien en meldingen '
            'te krijgen.',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: TokSpace.s3),
          FilledButton.icon(
            onPressed: () => context.go('/ontdekken'),
            icon: const Icon(Icons.explore_outlined),
            label: const Text('Moskeeën ontdekken'),
          ),
        ],
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Card(
          child: Column(
            children: [
              for (final (index, follow) in list.indexed) ...[
                if (index > 0) const Divider(height: 1),
                _FollowTile(follow: follow),
              ],
            ],
          ),
        ),
        const SizedBox(height: TokSpace.s2),
        const PushPermissionNotice(),
      ],
    );
  }
}

class _FollowTile extends ConsumerStatefulWidget {
  const _FollowTile({required this.follow});

  final FollowStatus follow;

  @override
  ConsumerState<_FollowTile> createState() => _FollowTileState();
}

class _FollowTileState extends ConsumerState<_FollowTile> {
  bool _busy = false;

  Future<void> _run(Future<void> Function() action, String failure) async {
    setState(() => _busy = true);
    try {
      await action();
    } on AppFailure {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(failure)));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _toggle(NotificationTopic topic, bool enabled) {
    final container = ProviderScope.containerOf(context, listen: false);
    return _run(() async {
      await ref.updateNotification(widget.follow.mosqueId, topic, enabled);
      if (enabled) await enablePush(container);
    }, 'Je voorkeur kon niet worden opgeslagen. Probeer het opnieuw.');
  }

  Future<void> _unfollow() async {
    final name = widget.follow.mosqueName;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('$name ontvolgen?'),
        content: const Text('Je meldingsvoorkeuren worden dan ook verwijderd. Je kunt later altijd opnieuw volgen.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuleren')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Ontvolgen')),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    await _run(() => ref.unfollow(widget.follow.mosqueId), 'Ontvolgen is niet gelukt. Probeer het opnieuw.');
  }

  @override
  Widget build(BuildContext context) {
    final follow = widget.follow;
    final text = Theme.of(context).textTheme;
    return ExpansionTile(
      leading: MosqueAvatar(name: follow.mosqueName, logoUrl: follow.mosqueLogoUrl),
      title: Text(follow.mosqueName),
      subtitle: Text(
        follow.favorite
            ? 'Favoriet · volgt sinds ${Dates.date(follow.followedSince)}'
            : 'Volgt sinds ${Dates.date(follow.followedSince)}',
      ),
      shape: const Border(),
      childrenPadding: const EdgeInsets.fromLTRB(TokSpace.s4, 0, TokSpace.s4, TokSpace.s3),
      expandedCrossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SwitchListTile(
          contentPadding: EdgeInsets.zero,
          secondary: Icon(follow.favorite ? Icons.star : Icons.star_outline),
          title: const Text('Favoriet'),
          subtitle: const Text('Bovenaan op Home en bij Favorieten'),
          value: follow.favorite,
          onChanged: _busy
              ? null
              : (favorite) => _run(
                  () => ref.setFavoriteMosque(follow.mosqueId, favorite),
                  'Opslaan als favoriet is niet gelukt.',
                ),
        ),
        Text('Waarover wil je meldingen ontvangen?', style: text.titleSmall),
        for (final topic in NotificationTopic.values)
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(topic.label),
            value: follow.notifications[topic] ?? false,
            onChanged: _busy ? null : (enabled) => _toggle(topic, enabled),
          ),
        Wrap(
          spacing: TokSpace.s2,
          children: [
            TextButton(
              onPressed: () => context.push('/moskee/${Uri.encodeComponent(follow.mosqueSlug)}'),
              child: const Text('Bekijk de moskee'),
            ),
            TextButton(onPressed: _busy ? null : _unfollow, child: const Text('Ontvolgen')),
          ],
        ),
      ],
    );
  }
}

class _MyRegistrations extends ConsumerWidget {
  const _MyRegistrations();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final value = ref.watch(myRegistrationsProvider);
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
    final upcoming = value.requireValue.where((r) => r.eventStartsAt.isAfter(now)).toList()
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
    final following = ref.watch(myFollowsProvider).value?.isNotEmpty ?? false;
    final current = !signedIn ? 0 : (following ? 2 : 1);
    const items = [
      (Icons.visibility_outlined, 'Bezoeker', 'Bekijkt openbare content, zonder account.'),
      (Icons.person_outline, 'Account', 'Kan zich aanmelden voor activiteiten en moskeeën volgen.'),
      (Icons.favorite_outline, 'Volger', 'Blijft op de hoogte van nieuws en activiteiten. Volgen is gratis.'),
      (
        Icons.card_membership_outlined,
        'Lid',
        'Officieel lid van een moskee. Lidmaatschap regel je rechtstreeks bij de moskee, niet via de app.',
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
