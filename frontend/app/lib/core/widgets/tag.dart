import 'package:flutter/material.dart';

import '../design/design.dart';

enum TagTone { neutral, primary, accent, warning, error }

/// Klein label, zoals een categorie of "Geannuleerd". De betekenis staat altijd in de tekst, niet alleen in de kleur.
class Tag extends StatelessWidget {
  const Tag({super.key, required this.label, this.tone = TagTone.neutral, this.icon});

  final String label;
  final TagTone tone;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final (Color background, Color foreground) = switch (tone) {
      TagTone.neutral => (c.surfaceMuted, c.onSurface),
      TagTone.primary => (c.primaryContainer, c.onPrimaryContainer),
      TagTone.accent => (c.accentContainer, c.onAccentContainer),
      TagTone.warning => (c.warningContainer, c.onWarningContainer),
      TagTone.error => (c.errorContainer, c.onErrorContainer),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: TokSpace.s2, vertical: TokSpace.s1),
      decoration: BoxDecoration(color: background, borderRadius: BorderRadius.circular(TokRadius.full)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: TokIconSize.sm, color: foreground),
            const SizedBox(width: TokSpace.s1),
          ],
          Flexible(
            child: Text(label, style: Theme.of(context).textTheme.labelMedium?.copyWith(color: foreground)),
          ),
        ],
      ),
    );
  }
}
