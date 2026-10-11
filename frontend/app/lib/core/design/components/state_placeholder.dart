import 'package:flutter/material.dart';

import '../app_theme.dart';
import '../tokens.g.dart';

/// Lege toestand of fouttoestand van een scherm: icoon, uitleg en een bruikbare vervolgstap.
class StatePlaceholder extends StatelessWidget {
  const StatePlaceholder({
    super.key,
    required this.icon,
    required this.title,
    required this.message,
    this.actionLabel,
    this.onAction,
  }) : assert((actionLabel == null) == (onAction == null), 'actionLabel en onAction horen bij elkaar');

  /// Fouttoestand met een knop om het opnieuw te proberen.
  const StatePlaceholder.error({
    super.key,
    this.title = 'Er ging iets mis',
    this.message = 'Controleer je internetverbinding en probeer het opnieuw.',
    required VoidCallback onRetry,
  }) : icon = Icons.cloud_off_outlined,
       actionLabel = 'Opnieuw proberen',
       onAction = onRetry;

  final IconData icon;
  final String title;
  final String message;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final text = Theme.of(context).textTheme;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(TokSpace.s6),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(TokSpace.s4),
              decoration: BoxDecoration(color: c.primaryContainer, shape: BoxShape.circle),
              child: Icon(icon, size: TokIconSize.xl, color: c.onPrimaryContainer),
            ),
            const SizedBox(height: TokSpace.s4),
            Text(title, style: text.titleLarge, textAlign: TextAlign.center),
            const SizedBox(height: TokSpace.s2),
            Text(
              message,
              style: text.bodyMedium?.copyWith(color: c.onSurfaceMuted),
              textAlign: TextAlign.center,
            ),
            if (actionLabel != null) ...[
              const SizedBox(height: TokSpace.s5),
              FilledButton(onPressed: onAction, child: Text(actionLabel!)),
            ],
          ],
        ),
      ),
    );
  }
}
