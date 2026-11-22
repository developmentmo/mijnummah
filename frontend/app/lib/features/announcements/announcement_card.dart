import 'package:flutter/material.dart';

import '../../core/design/design.dart';
import '../../core/format/dates.dart';
import '../../core/widgets/tag.dart';
import '../../data/models.dart';
import '../mosques/mosque_widgets.dart';

class AnnouncementCard extends StatelessWidget {
  const AnnouncementCard({super.key, required this.announcement, required this.onTap, this.showMosque = true});

  final Announcement announcement;
  final VoidCallback onTap;

  /// Uit op het profiel van de moskee zelf.
  final bool showMosque;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final text = Theme.of(context).textTheme;
    final mosque = showMosque ? announcement.mosque : null;
    return Card(
      clipBehavior: Clip.antiAlias,
      color: announcement.featured ? c.accentContainer : null,
      child: Semantics(
        button: true,
        label: [
          if (announcement.featured) 'Uitgelicht',
          announcement.title,
          if (mosque != null) mosque.name,
          'geplaatst op ${Dates.date(announcement.publishedAt)}',
        ].join(', '),
        excludeSemantics: true,
        child: InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(TokComponent.cardPadding),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (announcement.featured) ...[
                  const Tag(label: 'Uitgelicht', tone: TagTone.accent, icon: Icons.star_outline),
                  const SizedBox(height: TokSpace.s2),
                ],
                if (mosque != null) ...[MosqueLabel(mosque: mosque), const SizedBox(height: TokSpace.s2)],
                Text(announcement.title, style: text.titleMedium),
                const SizedBox(height: TokSpace.s1),
                Text(announcement.body, style: text.bodyMedium, maxLines: 3, overflow: TextOverflow.ellipsis),
                const SizedBox(height: TokSpace.s2),
                Text('Geplaatst op ${Dates.date(announcement.publishedAt)}', style: text.bodySmall),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
