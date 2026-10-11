import 'package:flutter/material.dart';

import '../app_theme.dart';
import '../tokens.g.dart';

enum StatusKind { info, success, warning, error }

/// Melding binnen een scherm (geen pop-up), met optioneel een herstelactie.
///
/// De soort wordt naast de kleur ook met een icoon en een voorleesbaar label aangegeven,
/// zodat de betekenis niet alleen van kleur afhangt.
class StatusMessage extends StatelessWidget {
  const StatusMessage({
    super.key,
    required this.kind,
    required this.message,
    this.title,
    this.actionLabel,
    this.onAction,
  }) : assert((actionLabel == null) == (onAction == null), 'actionLabel en onAction horen bij elkaar');

  final StatusKind kind;
  final String? title;
  final String message;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final text = Theme.of(context).textTheme;
    final (Color background, Color foreground, IconData icon, String label) = switch (kind) {
      StatusKind.info => (c.infoContainer, c.onInfoContainer, Icons.info_outline, 'Informatie'),
      StatusKind.success => (c.successContainer, c.onSuccessContainer, Icons.check_circle_outline, 'Gelukt'),
      StatusKind.warning => (c.warningContainer, c.onWarningContainer, Icons.warning_amber_outlined, 'Let op'),
      StatusKind.error => (c.errorContainer, c.onErrorContainer, Icons.error_outline, 'Fout'),
    };
    return Semantics(
      container: true,
      liveRegion: kind == StatusKind.error,
      label: label,
      child: Container(
        padding: const EdgeInsets.all(TokComponent.notificationPadding),
        decoration: BoxDecoration(
          color: background,
          borderRadius: BorderRadius.circular(TokComponent.notificationRadius),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: foreground, size: TokIconSize.lg),
            const SizedBox(width: TokSpace.s3),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (title != null) Text(title!, style: text.titleSmall?.copyWith(color: foreground)),
                  Text(message, style: text.bodyMedium?.copyWith(color: foreground)),
                  if (actionLabel != null)
                    Padding(
                      padding: const EdgeInsets.only(top: TokSpace.s2),
                      child: TextButton(
                        style: TextButton.styleFrom(
                          foregroundColor: foreground,
                          padding: EdgeInsets.zero,
                          textStyle: TokType.labelLarge.copyWith(decoration: TextDecoration.underline),
                        ),
                        onPressed: onAction,
                        child: Text(actionLabel!),
                      ),
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
