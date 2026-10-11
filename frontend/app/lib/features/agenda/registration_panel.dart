import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api/api_exception.dart';
import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../data/models.dart';
import 'agenda_providers.dart';

/// Aanmelden voor een activiteit of de aanmelding annuleren.
///
/// Dubbele aanmeldingen worden voorkomen doordat de knop tijdens het versturen uitgeschakeld is en de server een
/// tweede aanmelding weigert (`ALREADY_REGISTERED`); dat laatste wordt als "je was al aangemeld" getoond.
class RegistrationPanel extends ConsumerStatefulWidget {
  const RegistrationPanel({super.key, required this.event, this.now});

  final Event event;

  /// Alleen voor tests.
  final DateTime Function()? now;

  @override
  ConsumerState<RegistrationPanel> createState() => _RegistrationPanelState();
}

class _RegistrationPanelState extends ConsumerState<RegistrationPanel> {
  bool _busy = false;
  String? _error;
  String? _notice;

  Event get _event => widget.event;

  Future<void> _register() async {
    setState(() {
      _busy = true;
      _error = null;
      _notice = null;
    });
    try {
      await ref.read(meRepositoryProvider).register(_event.id);
    } on ApiException catch (e) {
      switch (e.code) {
        case 'ALREADY_REGISTERED':
          _notice = 'Je was al aangemeld voor deze activiteit.';
        case 'EVENT_FULL':
          _error = 'Helaas, deze activiteit is inmiddels vol.';
        case 'REGISTRATION_CLOSED':
          _error = 'Aanmelden is gesloten; de activiteit is al begonnen.';
        case 'EVENT_CANCELLED':
          _error = 'Deze activiteit is geannuleerd.';
        case 'UNAUTHORIZED':
          _error = 'Je sessie is verlopen. Log opnieuw in om je aan te melden.';
        default:
          _error = 'Aanmelden is niet gelukt. Probeer het later opnieuw.';
      }
    } on NetworkException {
      _error = 'Geen verbinding. Controleer je internet en probeer het opnieuw.';
    }
    await _reload();
  }

  Future<void> _cancel() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Aanmelding annuleren?'),
        content: Text('Je plaats voor "${_event.title}" komt dan vrij voor iemand anders.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Terug')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Annuleren')),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    setState(() {
      _busy = true;
      _error = null;
      _notice = null;
    });
    try {
      await ref.read(meRepositoryProvider).cancelRegistration(_event.id);
      _notice = 'Je aanmelding is geannuleerd.';
    } on ApiException catch (e) {
      _error = e.code == 'REGISTRATION_NOT_FOUND' ? null : 'Annuleren is niet gelukt. Probeer het later opnieuw.';
    } on NetworkException {
      _error = 'Geen verbinding. Controleer je internet en probeer het opnieuw.';
    }
    await _reload();
  }

  /// Haalt de activiteit opnieuw op en houdt de knop uitgeschakeld tot de nieuwe status binnen is.
  Future<void> _reload() async {
    if (!mounted) return;
    refreshRegistrationViews(ref, _event.id);
    try {
      await ref.read(eventProvider(_event.id).future);
    } on AppFailure {
      // De pagina toont zelf de fout of de offline-melding.
    }
    if (mounted) setState(() => _busy = false);
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final signedIn = ref.watch(currentUserIdProvider) != null;
    final registration = _event.registration;
    final started = _event.hasStarted((widget.now ?? DateTime.now)());

    final List<Widget> body;
    if (!signedIn) {
      body = [
        Text('Log in om je aan te melden. Zo weet de moskee op hoeveel mensen ze kan rekenen.', style: text.bodyMedium),
        const SizedBox(height: TokSpace.s3),
        FilledButton(onPressed: () => context.push('/inloggen'), child: const Text('Inloggen om je aan te melden')),
      ];
    } else if (registration.registeredByMe == true) {
      body = [
        const StatusMessage(kind: StatusKind.success, message: 'Je bent aangemeld voor deze activiteit.'),
        if (!started) ...[
          const SizedBox(height: TokSpace.s3),
          OutlinedButton(onPressed: _busy ? null : _cancel, child: const Text('Aanmelding annuleren')),
        ],
      ];
    } else if (started) {
      body = [Text('Aanmelden is gesloten; de activiteit is al begonnen.', style: text.bodyMedium)];
    } else if (registration.isFull) {
      body = [const StatusMessage(kind: StatusKind.warning, message: 'Deze activiteit is vol.')];
    } else {
      body = [
        Text(_placesText(registration), style: text.bodyMedium),
        const SizedBox(height: TokSpace.s3),
        FilledButton(
          onPressed: _busy ? null : _register,
          child: _busy
              ? const SizedBox.square(dimension: TokIconSize.md, child: CircularProgressIndicator(strokeWidth: 2))
              : const Text('Aanmelden'),
        ),
      ];
    }

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(TokComponent.cardPadding),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Semantics(header: true, child: Text('Aanmelden', style: text.titleLarge)),
            const SizedBox(height: TokSpace.s2),
            ...body,
            if (_notice != null) ...[
              const SizedBox(height: TokSpace.s3),
              StatusMessage(kind: StatusKind.info, message: _notice!),
            ],
            if (_error != null) ...[
              const SizedBox(height: TokSpace.s3),
              StatusMessage(kind: StatusKind.error, message: _error!),
            ],
          ],
        ),
      ),
    );
  }

  static String _placesText(EventRegistration registration) {
    final places = registration.availablePlaces;
    if (places == null) return 'Er is geen maximum aantal deelnemers.';
    return places == 1 ? 'Nog 1 plaats vrij.' : 'Nog $places plaatsen vrij.';
  }
}
