import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../design/design.dart';
import '../providers.dart';

/// Afbeelding van internet met een vaste verhouding. Zonder URL, tijdens het laden of als de afbeelding niet
/// laadt, verschijnt een rustig vlak met een icoon. Decoratief: screenreaders slaan het over. Een pad zonder host
/// (een geüploade afbeelding, `/api/v1/public/images/…`) komt van de eigen API.
class NetworkImageBox extends ConsumerWidget {
  const NetworkImageBox({super.key, required this.url, required this.fallbackIcon, this.aspectRatio = 16 / 9});

  final String? url;
  final IconData fallbackIcon;
  final double aspectRatio;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.colors;
    final src = resolveImageUrl(url, ref.watch(appConfigProvider).apiBaseUrl);
    final fallback = ColoredBox(
      color: c.surfaceMuted,
      child: Center(
        child: Icon(fallbackIcon, size: TokIconSize.xl, color: c.onSurfaceMuted),
      ),
    );
    return ExcludeSemantics(
      child: AspectRatio(
        aspectRatio: aspectRatio,
        child: src == null
            ? fallback
            : Image.network(
                src,
                fit: BoxFit.cover,
                errorBuilder: (_, _, _) => fallback,
                frameBuilder: (_, child, frame, wasSynchronouslyLoaded) =>
                    wasSynchronouslyLoaded || frame != null ? child : fallback,
              ),
      ),
    );
  }
}

/// Maakt een relatief pad van de API absoluut; een volledige link blijft zoals hij is.
String? resolveImageUrl(String? url, String apiBaseUrl) {
  if (url == null || url.isEmpty) return null;
  if (!url.startsWith('/')) return url;
  final base = apiBaseUrl.endsWith('/') ? apiBaseUrl.substring(0, apiBaseUrl.length - 1) : apiBaseUrl;
  return '$base$url';
}
