import 'package:flutter/material.dart';

import '../design/design.dart';

/// Horizontaal scrollende rij keuzechips, zoals een categoriefilter. De gekozen chip schuift vanzelf in beeld,
/// ook als de keuze van buitenaf komt (bijvoorbeeld via een link).
class ChoiceChipRow<T> extends StatelessWidget {
  const ChoiceChipRow({
    super.key,
    required this.semanticLabel,
    required this.options,
    required this.selected,
    required this.onSelected,
  });

  final String semanticLabel;
  final List<(T value, String label)> options;
  final T selected;
  final ValueChanged<T> onSelected;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      container: true,
      label: semanticLabel,
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.fromLTRB(TokSpace.s4, TokSpace.s2, TokSpace.s4, TokSpace.s2),
        child: Row(
          children: [
            for (final (value, label) in options)
              Padding(
                padding: const EdgeInsets.only(right: TokSpace.s2),
                child: _KeepVisible(
                  active: value == selected,
                  child: ChoiceChip(
                    label: Text(label),
                    selected: value == selected,
                    onSelected: (_) => onSelected(value),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _KeepVisible extends StatefulWidget {
  const _KeepVisible({required this.active, required this.child});

  final bool active;
  final Widget child;

  @override
  State<_KeepVisible> createState() => _KeepVisibleState();
}

class _KeepVisibleState extends State<_KeepVisible> {
  @override
  void initState() {
    super.initState();
    if (widget.active) _reveal();
  }

  @override
  void didUpdateWidget(_KeepVisible oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.active && !oldWidget.active) _reveal();
  }

  void _reveal() => WidgetsBinding.instance.addPostFrameCallback((_) {
    if (!mounted) return;
    Scrollable.ensureVisible(context, alignment: 0.5, duration: TokMotion.normal, curve: TokMotion.standard);
  });

  @override
  Widget build(BuildContext context) => widget.child;
}
