import 'dart:convert';

import 'package:flutter/material.dart';

import '../../core/api/api_exception.dart';
import '../../core/design/design.dart';

/// Bouwstenen voor de formulieren rond het account: inloggen, registreren, wachtwoord herstellen en wijzigen.
abstract final class AuthForm {
  /// Gelijk aan de regels van de backend (`PasswordPolicy`): minstens 12 tekens en hoogstens 72 bytes.
  static const minPasswordLength = 12;
  static const maxPasswordBytes = 72;

  static String? email(String? value) {
    final email = value?.trim() ?? '';
    if (email.isEmpty) return 'Vul je e-mailadres in.';
    if (!email.contains('@') || email.startsWith('@') || email.endsWith('@') || email.contains(' ')) {
      return 'Vul een geldig e-mailadres in.';
    }
    return null;
  }

  static String? newPassword(String? value) {
    final password = value ?? '';
    if (password.trim().isEmpty) return 'Kies een wachtwoord.';
    if (password.runes.length < minPasswordLength) return 'Gebruik minstens $minPasswordLength tekens.';
    if (utf8.encode(password).length > maxPasswordBytes) return 'Dit wachtwoord is te lang.';
    return null;
  }

  static const passwordHint = 'Minstens $minPasswordLength tekens. Een zin van een paar woorden werkt goed.';

  /// Meldingen voor fouten die bij elk formulier kunnen voorkomen. [codes] gaat voor.
  static String message(AppFailure failure, {Map<String, String> codes = const {}, required String fallback}) {
    if (failure is NetworkException) {
      return 'Geen verbinding met de moskee. Controleer je internet en probeer het opnieuw.';
    }
    final e = failure as ApiException;
    final specific = codes[e.code];
    if (specific != null) return specific;
    return switch (e.code) {
      'INVALID_PASSWORD' => 'Kies een wachtwoord van minstens $minPasswordLength tekens.',
      'TOO_MANY_LOGIN_ATTEMPTS' || 'RATE_LIMITED' => tooMany(e.retryAfter),
      _ => fallback,
    };
  }

  static String tooMany(Duration? retryAfter) {
    final minutes = retryAfter == null ? null : (retryAfter.inSeconds / 60).ceil();
    return minutes == null || minutes <= 1
        ? 'Te veel pogingen. Wacht even en probeer het dan opnieuw.'
        : 'Te veel pogingen. Probeer het over $minutes minuten opnieuw.';
  }
}

/// Wachtwoordveld met een knop om het wachtwoord te tonen.
class PasswordField extends StatefulWidget {
  const PasswordField({
    super.key,
    required this.controller,
    this.label = 'Wachtwoord',
    this.helperText,
    this.isNew = false,
    this.validator,
    this.textInputAction = TextInputAction.done,
    this.onSubmitted,
  });

  final TextEditingController controller;
  final String label;
  final String? helperText;

  /// Een nieuw wachtwoord: de wachtwoordmanager mag er een voorstellen.
  final bool isNew;
  final FormFieldValidator<String>? validator;
  final TextInputAction textInputAction;
  final ValueChanged<String>? onSubmitted;

  @override
  State<PasswordField> createState() => _PasswordFieldState();
}

class _PasswordFieldState extends State<PasswordField> {
  bool _obscure = true;

  @override
  Widget build(BuildContext context) => TextFormField(
    controller: widget.controller,
    decoration: InputDecoration(
      labelText: widget.label,
      helperText: widget.helperText,
      helperMaxLines: 3,
      suffixIcon: IconButton(
        tooltip: _obscure ? 'Wachtwoord tonen' : 'Wachtwoord verbergen',
        icon: Icon(_obscure ? Icons.visibility_outlined : Icons.visibility_off_outlined),
        onPressed: () => setState(() => _obscure = !_obscure),
      ),
    ),
    obscureText: _obscure,
    autocorrect: false,
    enableSuggestions: false,
    textInputAction: widget.textInputAction,
    autofillHints: [widget.isNew ? AutofillHints.newPassword : AutofillHints.password],
    onFieldSubmitted: widget.onSubmitted,
    validator: widget.validator ?? (value) => (value ?? '').isEmpty ? 'Vul je wachtwoord in.' : null,
  );
}

/// Primaire knop die tijdens het versturen een laadindicator toont.
class BusyButton extends StatelessWidget {
  const BusyButton({super.key, required this.label, required this.busy, required this.onPressed});

  final String label;
  final bool busy;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) => FilledButton(
    onPressed: busy ? null : onPressed,
    child: busy
        ? Semantics(
            label: 'Bezig',
            child: const SizedBox.square(dimension: TokIconSize.md, child: CircularProgressIndicator(strokeWidth: 2)),
          )
        : Text(label),
  );
}
