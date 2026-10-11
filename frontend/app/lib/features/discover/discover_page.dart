import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/content_width.dart';
import '../../core/widgets/external_links.dart';
import '../../core/widgets/info_row.dart';
import '../../core/widgets/offline_banner.dart';
import '../../core/widgets/section_header.dart';
import '../../data/models.dart';

/// Kennismaken met de moskee: wie ze is, waar ze zit, hoe je contact opneemt en welke activiteiten er zijn.
class DiscoverPage extends ConsumerWidget {
  const DiscoverPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(title: const Text('Ontdekken')),
      body: AsyncView(
        value: ref.watch(mosqueProvider),
        onRetry: () => ref.invalidate(mosqueProvider),
        loading: ListView(
          padding: const EdgeInsets.all(TokSpace.s4),
          children: const [
            Skeleton(height: 32),
            SizedBox(height: TokSpace.s3),
            Skeleton(height: 72),
            SizedBox(height: TokSpace.s6),
            Skeleton(height: 120),
          ],
        ),
        data: (fetched) => RefreshIndicator(
          onRefresh: () => ref.refresh(mosqueProvider.future),
          child: _Discover(
            mosque: fetched.data,
            cachedAt: fetched.cachedAt,
            onRetry: () => ref.invalidate(mosqueProvider),
          ),
        ),
      ),
    );
  }
}

class _Discover extends StatelessWidget {
  const _Discover({required this.mosque, required this.cachedAt, required this.onRetry});

  final Mosque mosque;
  final DateTime? cachedAt;
  final VoidCallback onRetry;

  static const _activities = <(EventCategory, IconData, String)>[
    (EventCategory.lecture, Icons.record_voice_over_outlined, 'Lezingen door sprekers en imams'),
    (EventCategory.fridayProgram, Icons.mosque_outlined, 'Het vrijdaggebed en het programma eromheen'),
    (EventCategory.classes, Icons.menu_book_outlined, 'Lessen voor kinderen en volwassenen'),
    (EventCategory.youth, Icons.groups_outlined, 'Activiteiten voor jongeren'),
    (EventCategory.community, Icons.diversity_3_outlined, 'Ontmoetingen voor de hele gemeenschap'),
  ];

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final address = mosque.addressLine;
    final hasContact = mosque.email != null || mosque.phone != null || mosque.websiteUrl != null;
    final website = mosque.websiteUrl == null ? null : Uri.tryParse(mosque.websiteUrl!);

    return ListView(
      padding: const EdgeInsets.fromLTRB(TokSpace.s4, TokSpace.s2, TokSpace.s4, TokSpace.s8),
      children: [
        ContentWidth(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (cachedAt != null) ...[
                OfflineBanner(savedAt: cachedAt!, onRetry: onRetry),
                const SizedBox(height: TokSpace.s4),
              ],
              const Align(
                alignment: Alignment.centerLeft,
                child: BrandLogo(variant: BrandLogoVariant.compact, height: 56),
              ),
              const SizedBox(height: TokSpace.s3),
              Semantics(header: true, child: Text(mosque.name, style: text.headlineLarge)),
              if (mosque.description != null) ...[
                const SizedBox(height: TokSpace.s2),
                Text(mosque.description!, style: text.bodyLarge),
              ],
              const SectionHeader(title: 'Kennismaken'),
              Text(
                'Wil je de moskee leren kennen? Kom naar een activiteit die voor iedereen toegankelijk is, bekijk een '
                'lezing terug of neem contact op.',
                style: text.bodyMedium,
              ),
              const SizedBox(height: TokSpace.s3),
              Wrap(
                spacing: TokSpace.s2,
                runSpacing: TokSpace.s2,
                children: [
                  FilledButton.icon(
                    onPressed: () => context.go('/agenda'),
                    icon: const Icon(Icons.calendar_month_outlined),
                    label: const Text('Bekijk de agenda'),
                  ),
                  OutlinedButton.icon(
                    onPressed: () => context.go('/lezingen'),
                    icon: const Icon(Icons.play_circle_outline),
                    label: const Text('Lezingen terugkijken'),
                  ),
                ],
              ),
              if (address != null || hasContact) const SectionHeader(title: 'Adres en contact'),
              if (address != null)
                InfoRow(
                  icon: Icons.place_outlined,
                  label: 'Adres, opent de kaart',
                  value: address,
                  onTap: () => openExternal(context, mapsUri('$address, ${mosque.name}')),
                ),
              if (mosque.phone != null)
                InfoRow(
                  icon: Icons.phone_outlined,
                  label: 'Bellen',
                  value: mosque.phone!,
                  onTap: () => openExternal(context, Uri(scheme: 'tel', path: mosque.phone!.replaceAll(' ', ''))),
                ),
              if (mosque.email != null)
                InfoRow(
                  icon: Icons.mail_outline,
                  label: 'E-mailen',
                  value: mosque.email!,
                  onTap: () => openExternal(context, Uri(scheme: 'mailto', path: mosque.email!)),
                ),
              if (website != null && website.hasScheme)
                InfoRow(
                  icon: Icons.language_outlined,
                  label: 'Website',
                  value: website.host.isEmpty ? mosque.websiteUrl! : website.host,
                  onTap: () => openExternal(context, website),
                ),
              const SectionHeader(title: 'Activiteiten'),
              Card(
                child: Column(
                  children: [
                    for (final (index, (category, icon, description)) in _activities.indexed) ...[
                      if (index > 0) const Divider(height: 1),
                      ListTile(
                        leading: Icon(icon),
                        title: Text(category.label),
                        subtitle: Text(description),
                        trailing: const Icon(Icons.chevron_right),
                        onTap: () => context.go('/agenda?categorie=${category.apiValue}'),
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
