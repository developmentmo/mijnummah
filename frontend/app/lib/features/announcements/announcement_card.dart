import 'package:flutter/material.dart';

import '../../core/design/design.dart';
import '../../core/format/dates.dart';
import '../../core/widgets/tag.dart';
import '../../data/models.dart';

class AnnouncementCard extends StatelessWidget {
  const AnnouncementCard({super.key, required this.announcement, required this.onTap});

  final Announcement announcement;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final text = Theme.of(context).textTheme;
    return Card(
      clipBehavior: Clip.antiAlias,
      color: announcement.featured ? c.accentContainer : null,
      child: Semantics(
        button: true,
        label: [
          if (announcement.featured) 'Uitgelicht',
          announcement.title,
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
