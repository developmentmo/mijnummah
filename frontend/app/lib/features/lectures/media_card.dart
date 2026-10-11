import 'package:flutter/material.dart';

import '../../core/design/design.dart';
import '../../core/format/dates.dart';
import '../../core/widgets/network_image_box.dart';
import '../../data/models.dart';

/// Lezing of video in een lijst: thumbnail, titel, spreker en datum.
class MediaCard extends StatelessWidget {
  const MediaCard({super.key, required this.item, required this.onTap});

  final MediaItem item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final text = Theme.of(context).textTheme;
    final subtitle = [if (item.speaker != null) item.speaker!, Dates.date(item.publishedAt)].join(' · ');
    final label = [
      item.title,
      if (item.speaker != null) 'door ${item.speaker}',
      item.category.label,
      if (item.duration != null) Dates.duration(item.duration!),
      'gepubliceerd op ${Dates.date(item.publishedAt)}',
    ].join(', ');

    return Card(
      clipBehavior: Clip.antiAlias,
      child: Semantics(
        button: true,
        label: label,
        excludeSemantics: true,
        child: InkWell(
          onTap: onTap,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Stack(
                children: [
                  NetworkImageBox(url: item.thumbnailUrl, fallbackIcon: Icons.play_circle_outline),
                  Positioned.fill(
                    child: Center(
                      child: Container(
                        padding: const EdgeInsets.all(TokSpace.s2),
                        decoration: BoxDecoration(color: c.scrim, shape: BoxShape.circle),
                        child: Icon(Icons.play_arrow_rounded, size: TokIconSize.xl, color: TokPalette.neutral0),
                      ),
                    ),
                  ),
                  if (item.duration != null)
                    Positioned(
                      right: TokSpace.s2,
                      bottom: TokSpace.s2,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: TokSpace.s2, vertical: 2),
                        decoration: BoxDecoration(
                          color: c.surfaceInverse,
                          borderRadius: BorderRadius.circular(TokRadius.xs),
                        ),
                        child: Text(
                          Dates.duration(item.duration!),
                          style: text.labelSmall?.copyWith(color: c.onSurfaceInverse),
                        ),
                      ),
                    ),
                ],
              ),
              Padding(
                padding: const EdgeInsets.all(TokSpace.s3),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(item.category.label.toUpperCase(), style: text.labelSmall?.copyWith(color: c.accentText)),
                    const SizedBox(height: TokSpace.s1),
                    Text(item.title, style: text.titleSmall, maxLines: 3, overflow: TextOverflow.ellipsis),
                    const SizedBox(height: TokSpace.s1),
                    Text(subtitle, style: text.bodySmall),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class MediaCardSkeleton extends StatelessWidget {
  const MediaCardSkeleton({super.key});

  @override
  Widget build(BuildContext context) {
    return const Card(
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          AspectRatio(aspectRatio: 16 / 9, child: Skeleton(radius: 0)),
          Padding(
            padding: EdgeInsets.all(TokSpace.s3),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Skeleton(width: 60, height: 12),
                SizedBox(height: TokSpace.s2),
                Skeleton(height: 18),
                SizedBox(height: TokSpace.s2),
                Skeleton(width: 140, height: 14),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Legt kaarten in één kolom op telefoons en in twee of drie kolommen op bredere schermen.
class ResponsiveCardGrid extends StatelessWidget {
  const ResponsiveCardGrid({super.key, required this.children, this.minItemWidth = 300});

  final List<Widget> children;
  final double minItemWidth;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        const gap = TokSpace.s3;
        final columns = (constraints.maxWidth / minItemWidth).floor().clamp(1, 3);
        final width = (constraints.maxWidth - gap * (columns - 1)) / columns;
        return Wrap(
          spacing: gap,
          runSpacing: gap,
          children: [for (final child in children) SizedBox(width: width, child: child)],
        );
      },
    );
  }
}
