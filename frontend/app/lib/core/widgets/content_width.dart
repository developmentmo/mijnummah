import 'package:flutter/material.dart';

import '../design/design.dart';

/// Beperkt de breedte van de inhoud op tablets en grote schermen, zodat regels leesbaar blijven.
class ContentWidth extends StatelessWidget {
  const ContentWidth({super.key, required this.child, this.maxWidth = 840});

  final Widget child;
  final double maxWidth;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.topCenter,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: maxWidth.clamp(0, TokSize.contentMaxWidth)),
        child: child,
      ),
    );
  }
}
