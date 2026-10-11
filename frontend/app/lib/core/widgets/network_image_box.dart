import 'package:flutter/material.dart';

import '../design/design.dart';

/// Afbeelding van internet met een vaste verhouding. Zonder URL, tijdens het laden of als de afbeelding niet
/// laadt, verschijnt een rustig vlak met een icoon. Decoratief: screenreaders slaan het over.
class NetworkImageBox extends StatelessWidget {
  const NetworkImageBox({super.key, required this.url, required this.fallbackIcon, this.aspectRatio = 16 / 9});

  final String? url;
  final IconData fallbackIcon;
  final double aspectRatio;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final fallback = ColoredBox(
      color: c.surfaceMuted,
      child: Center(
        child: Icon(fallbackIcon, size: TokIconSize.xl, color: c.onSurfaceMuted),
      ),
    );
    return ExcludeSemantics(
      child: AspectRatio(
        aspectRatio: aspectRatio,
        child: url == null
            ? fallback
            : Image.network(
                url!,
                fit: BoxFit.cover,
                errorBuilder: (_, _, _) => fallback,
                frameBuilder: (_, child, frame, wasSynchronouslyLoaded) =>
                    wasSynchronouslyLoaded || frame != null ? child : fallback,
              ),
      ),
    );
  }
}
