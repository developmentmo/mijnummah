import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/design/design.dart';
import '../../core/format/dates.dart';
import '../../core/providers.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/content_width.dart';
import '../../core/widgets/detail_app_bar.dart';
import '../mosques/mosque_widgets.dart';
import '../profile/follow_providers.dart';

/// Meldingen van de gevolgde moskeeën van de laatste 30 dagen, ook als een pushmelding dit toestel niet bereikte.
class NotificationsPage extends ConsumerWidget {
  const NotificationsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final signedIn = ref.watch(currentUserIdProvider) != null;
    return Scaffold(
      appBar: const DetailAppBar(title: 'Meldingen'),
      body: !signedIn
          ? ListView(
              children: [
                StatePlaceholder(
                  icon: Icons.notifications_none,
                  title: 'Log in voor meldingen',
                  message: 'Volg moskeeën met een account, dan zie je hier hun nieuws, activiteiten en video\'s.',
                  actionLabel: 'Inloggen',
                  onAction: () => context.push('/inloggen'),
                ),
              ],
            )
          : RefreshIndicator(
              onRefresh: () => ref.refresh(inboxProvider.future),
              child: AsyncView(
                value: ref.watch(inboxProvider),
                onRetry: () => ref.invalidate(inboxProvider),
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
                data: (page) => page.items.isEmpty
                    ? ListView(
                        children: [
                          StatePlaceholder(
                            icon: Icons.notifications_none,
                            title: 'Geen meldingen',
                            message: 'Meldingen van de moskeeën die je volgt verschijnen hier.',
                            actionLabel: 'Moskeeën ontdekken',
                            onAction: () => context.go('/ontdekken'),
                          ),
                        ],
                      )
                    : ContentWidth(
                        child: ListView.separated(
                          padding: const EdgeInsets.symmetric(vertical: TokSpace.s2),
                          itemCount: page.items.length,
                          separatorBuilder: (_, _) => const Divider(height: 1),
                          itemBuilder: (context, index) {
                            final item = page.items[index];
                            final location = item.location;
                            final mosque = item.mosque;
                            return ListTile(
                              leading: mosque == null
                                  ? const Icon(Icons.notifications_outlined)
                                  : MosqueAvatar(name: mosque.name, logoUrl: mosque.logoUrl),
                              title: Text(item.title),
                              subtitle: Text(
                                [
                                  if (item.body.isNotEmpty) item.body,
                                  [if (mosque != null) mosque.name, Dates.compact(item.sentAt)].join(' · '),
                                ].join('\n'),
                              ),
                              isThreeLine: item.body.isNotEmpty,
                              trailing: location == null ? null : const Icon(Icons.chevron_right),
                              onTap: location == null ? null : () => context.push(location),
                            );
                          },
                        ),
                      ),
              ),
            ),
    );
  }
}
