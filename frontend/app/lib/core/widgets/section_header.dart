import 'package:flutter/material.dart';

import '../design/design.dart';

/// Kop boven een sectie, met optioneel een link naar het volledige overzicht.
class SectionHeader extends StatelessWidget {
  const SectionHeader({super.key, required this.title, this.actionLabel, this.onAction})
    : assert((actionLabel == null) == (onAction == null), 'actionLabel en onAction horen bij elkaar');

  final String title;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: TokSpace.s6, bottom: TokSpace.s2),
      child: Wrap(
        alignment: WrapAlignment.spaceBetween,
        crossAxisAlignment: WrapCrossAlignment.center,
        spacing: TokSpace.s2,
        children: [
          Semantics(header: true, child: Text(title, style: Theme.of(context).textTheme.headlineSmall)),
          if (actionLabel != null) TextButton(onPressed: onAction, child: Text(actionLabel!)),
        ],
      ),
    );
  }
}
