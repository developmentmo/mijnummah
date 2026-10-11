import 'package:flutter/material.dart';

import '../design/design.dart';
import '../format/dates.dart';

/// Meldt dat de getoonde gegevens uit de offline-cache komen.
class OfflineBanner extends StatelessWidget {
  const OfflineBanner({super.key, required this.savedAt, this.onRetry});

  final DateTime savedAt;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return StatusMessage(
      kind: StatusKind.warning,
      title: 'Je bent offline',
      message: 'Je ziet de gegevens van ${Dates.savedAt(savedAt)}. Ze kunnen inmiddels zijn gewijzigd.',
      actionLabel: onRetry == null ? null : 'Opnieuw proberen',
      onAction: onRetry,
    );
  }
}
