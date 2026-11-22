import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/api_client.dart';
import '../design/design.dart';
import 'failure_messages.dart';
import 'section_header.dart';

/// Sectie met een voorproefje van een lijst (startpagina, moskeeprofiel), met eigen laad-, fout- en lege toestand, zodat één mislukte lijst de rest niet blokkeert.
class PreviewSection<T> extends StatelessWidget {
  const PreviewSection({
    super.key,
    required this.title,
    required this.value,
    required this.isEmpty,
    required this.onRetry,
    required this.skeleton,
    required this.builder,
    this.actionLabel,
    this.onAction,
    this.empty,
    this.hideWhenEmpty = false,
  });

  final String title;
  final String? actionLabel;
  final VoidCallback? onAction;
  final AsyncValue<Fetched<T>> value;
  final bool Function(T data) isEmpty;
  final VoidCallback onRetry;
  final Widget skeleton;
  final Widget Function(T data) builder;
  final Widget? empty;
  final bool hideWhenEmpty;

  @override
  Widget build(BuildContext context) {
    final Widget content;
    if (value.hasError && !value.isLoading) {
      final failure = describeFailure(value.error!);
      content = StatusMessage(
        kind: StatusKind.error,
        title: failure.title,
        message: failure.message,
        actionLabel: 'Opnieuw proberen',
        onAction: onRetry,
      );
    } else if (value.hasValue) {
      final data = value.requireValue.data;
      if (isEmpty(data)) {
        if (hideWhenEmpty || empty == null) return const SizedBox.shrink();
        content = empty!;
      } else {
        content = builder(data);
      }
    } else {
      content = Semantics(label: '$title worden geladen', child: skeleton);
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(title: title, actionLabel: actionLabel, onAction: onAction),
        content,
      ],
    );
  }
}

/// Horizontaal scrollende rij kaarten. De hoogte volgt de inhoud, zodat grotere tekst niet wordt afgekapt.
class CardRail extends StatelessWidget {
  const CardRail({super.key, required this.children, this.width = 260});

  final List<Widget> children;

  /// Breedte van elke kaart.
  final double width;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      clipBehavior: Clip.none,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (final child in children)
            Padding(
              padding: const EdgeInsets.only(right: TokSpace.s3),
              child: SizedBox(width: width, child: child),
            ),
        ],
      ),
    );
  }
}
