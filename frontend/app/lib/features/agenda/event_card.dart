import 'package:flutter/material.dart';

import '../../core/design/design.dart';
import '../../core/format/dates.dart';
import '../../core/widgets/tag.dart';
import '../../data/models.dart';

/// Activiteit in een lijst: datumblok, titel, tijd en locatie.
class EventCard extends StatelessWidget {
  const EventCard({super.key, required this.event, required this.onTap});

  final Event event;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final text = Theme.of(context).textTheme;
    final (day, month) = Dates.dayAndMonth(event.startsAt);
    final when = '${Dates.shortDate(event.startsAt)} · ${Dates.timeRange(event.startsAt, event.endsAt)}';
    final label = [
      event.title,
      if (event.cancelled) 'geannuleerd',
      Dates.weekdayDate(event.startsAt),
      Dates.timeRange(event.startsAt, event.endsAt),
      if (event.location != null) event.location!,
      event.category.label,
    ].join(', ');

    return Card(
      clipBehavior: Clip.antiAlias,
      child: Semantics(
        button: true,
        label: label,
        excludeSemantics: true,
        child: InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(TokComponent.cardPadding),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 56,
                  padding: const EdgeInsets.symmetric(vertical: TokSpace.s2),
                  decoration: BoxDecoration(
                    color: event.cancelled ? c.surfaceMuted : c.primaryContainer,
                    borderRadius: BorderRadius.circular(TokRadius.md),
                  ),
                  child: Column(
                    children: [
                      Text(day, style: text.headlineSmall?.copyWith(color: c.onPrimaryContainer)),
                      Text(month, style: text.labelMedium?.copyWith(color: c.onPrimaryContainer)),
                    ],
                  ),
                ),
                const SizedBox(width: TokSpace.s4),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Wrap(
                        spacing: TokSpace.s2,
                        runSpacing: TokSpace.s1,
                        children: [
                          Tag(label: event.category.label, tone: TagTone.primary),
                          if (event.cancelled) const Tag(label: 'Geannuleerd', tone: TagTone.error),
                          if (event.registration.registeredByMe == true)
                            const Tag(label: 'Aangemeld', tone: TagTone.accent, icon: Icons.check),
                        ],
                      ),
                      const SizedBox(height: TokSpace.s2),
                      Text(
                        event.title,
                        style: text.titleMedium?.copyWith(
                          decoration: event.cancelled ? TextDecoration.lineThrough : null,
                        ),
                      ),
                      const SizedBox(height: TokSpace.s1),
                      Text(when, style: text.bodySmall),
                      if (event.location != null) Text(event.location!, style: text.bodySmall),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Plaatshouder voor een [EventCard] tijdens het laden.
class EventCardSkeleton extends StatelessWidget {
  const EventCardSkeleton({super.key});

  @override
  Widget build(BuildContext context) {
    return const Card(
      child: Padding(
        padding: EdgeInsets.all(TokComponent.cardPadding),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Skeleton(width: 56, height: 64, radius: TokRadius.md),
            SizedBox(width: TokSpace.s4),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Skeleton(width: 80, height: 20, radius: TokRadius.full),
                  SizedBox(height: TokSpace.s2),
                  Skeleton(height: 20),
                  SizedBox(height: TokSpace.s2),
                  Skeleton(width: 160, height: 14),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
