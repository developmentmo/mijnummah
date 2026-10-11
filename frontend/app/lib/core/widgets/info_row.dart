import 'package:flutter/material.dart';

import '../design/design.dart';

/// Regel met icoon en waarde, zoals datum of locatie. Screenreaders lezen "label: waarde".
class InfoRow extends StatelessWidget {
  const InfoRow({super.key, required this.icon, required this.label, required this.value, this.onTap});

  final IconData icon;
  final String label;
  final String value;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final text = Theme.of(context).textTheme;
    final row = Padding(
      padding: const EdgeInsets.symmetric(vertical: TokSpace.s2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: TokIconSize.md, color: c.secondary),
          const SizedBox(width: TokSpace.s3),
          Expanded(
            child: Text(
              value,
              style: text.bodyLarge?.copyWith(
                color: onTap == null ? null : c.link,
                decoration: onTap == null ? null : TextDecoration.underline,
              ),
            ),
          ),
        ],
      ),
    );
    return Semantics(
      label: '$label: $value',
      button: onTap != null,
      excludeSemantics: true,
      child: onTap == null ? row : InkWell(borderRadius: BorderRadius.circular(TokRadius.sm), onTap: onTap, child: row),
    );
  }
}
