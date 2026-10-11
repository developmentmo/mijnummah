import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../design/design.dart';
import 'failure_messages.dart';

/// Toont de drie toestanden van asynchrone gegevens: laden, fout met opnieuw proberen, en de gegevens zelf.
/// Tijdens het opnieuw laden blijven de vorige gegevens staan.
class AsyncView<T> extends StatelessWidget {
  const AsyncView({super.key, required this.value, required this.data, required this.loading, required this.onRetry});

  final AsyncValue<T> value;
  final Widget Function(T data) data;
  final Widget loading;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    if (value.hasError && !value.isLoading) {
      final failure = describeFailure(value.error!);
      return StatePlaceholder.error(title: failure.title, message: failure.message, onRetry: onRetry);
    }
    if (value.hasValue) return data(value.requireValue);
    return Semantics(label: 'Bezig met laden', liveRegion: true, child: loading);
  }
}
