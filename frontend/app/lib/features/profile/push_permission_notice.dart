import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../core/push/push_messaging.dart';

/// Vraagt toestemming voor meldingen en meldt het toestel aan. Wordt aangeroepen als de gebruiker de moskee gaat
/// volgen of een meldingsvoorkeur aanzet, zodat de systeemvraag een duidelijke aanleiding heeft.
///
/// Werkt via de container (`ProviderScope.containerOf`), omdat de aanroepende widget kan verdwijnen terwijl de
/// systeemvraag openstaat: de uitnodiging om te volgen verdwijnt zodra de gebruiker volgt.
Future<void> enablePush(ProviderContainer container) async {
  if (!container.read(pushMessagingProvider).supported) return;
  await container.read(pushRegistrationProvider).enable();
  container.invalidate(pushPermissionProvider);
}

/// Laat zien of meldingen dit toestel kunnen bereiken. Niets als alles in orde is.
class PushPermissionNotice extends ConsumerWidget {
  const PushPermissionNotice({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final permission = ref.watch(pushPermissionProvider);
    if (!permission.hasValue) return const SizedBox.shrink();
    return switch (permission.value) {
      null => const StatusMessage(
        kind: StatusKind.info,
        message:
            'Pushmeldingen zijn in deze versie van de app niet ingesteld. Je voorkeuren worden wel bewaard en gelden '
            'zodra meldingen beschikbaar zijn.',
      ),
      PushPermission.notDetermined => StatusMessage(
        kind: StatusKind.info,
        message: 'Sta meldingen toe om ze op dit toestel te ontvangen.',
        actionLabel: 'Meldingen toestaan',
        onAction: () => enablePush(ProviderScope.containerOf(context, listen: false)),
      ),
      PushPermission.denied => const StatusMessage(
        kind: StatusKind.warning,
        message: 'Meldingen staan uit voor deze app. Zet ze aan in de instellingen van je toestel om ze te ontvangen.',
      ),
      PushPermission.granted => const SizedBox.shrink(),
    };
  }
}
