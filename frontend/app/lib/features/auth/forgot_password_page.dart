import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api/api_exception.dart';
import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../core/widgets/content_width.dart';
import 'auth_form.dart';

/// Wachtwoord vergeten: eerst een code per e-mail aanvragen, dan met die code een nieuw wachtwoord kiezen. Daarna
/// is de gebruiker ingelogd.
class ForgotPasswordPage extends ConsumerStatefulWidget {
  const ForgotPasswordPage({super.key, this.initialEmail});

  /// Het adres dat al op het inlogscherm stond.
  final String? initialEmail;

  @override
  ConsumerState<ForgotPasswordPage> createState() => _ForgotPasswordPageState();
}

class _ForgotPasswordPageState extends ConsumerState<ForgotPasswordPage> {
  final _requestForm = GlobalKey<FormState>();
  final _confirmForm = GlobalKey<FormState>();
  late final _email = TextEditingController(text: widget.initialEmail);
  final _code = TextEditingController();
  final _password = TextEditingController();
  bool _codeSent = false;
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _email.dispose();
    _code.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _request() async {
    if (_busy || !(_requestForm.currentState?.validate() ?? false)) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await ref.read(authServiceProvider).requestPasswordReset(_email.text);
      if (mounted) setState(() => _codeSent = true);
    } on AppFailure catch (e) {
      setState(
        () => _error = AuthForm.message(
          e,
          codes: const {
            'PASSWORD_RESET_UNAVAILABLE':
                'Wachtwoord herstellen via e-mail is bij de moskee nog niet ingesteld. Neem contact op met de '
                'moskee.',
            'VALIDATION_FAILED': 'Vul een geldig e-mailadres in.',
          },
          fallback: 'De code kon niet worden verstuurd. Probeer het later opnieuw.',
        ),
      );
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _confirm() async {
    if (_busy || !(_confirmForm.currentState?.validate() ?? false)) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await ref
          .read(authControllerProvider.notifier)
          .confirmPasswordReset(email: _email.text, code: _code.text, newPassword: _password.text);
      if (!mounted) return;
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('Je wachtwoord is gewijzigd en je bent ingelogd.')));
      context.go('/profiel');
    } on AppFailure catch (e) {
      setState(
        () => _error = AuthForm.message(
          e,
          codes: const {
            'INVALID_RESET_CODE': 'Deze code klopt niet of is verlopen. Controleer de code of vraag een nieuwe aan.',
            'VALIDATION_FAILED': 'Controleer de code en je nieuwe wachtwoord.',
          },
          fallback: 'Je wachtwoord kon niet worden gewijzigd. Probeer het later opnieuw.',
        ),
      );
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _startOver() => setState(() {
    _codeSent = false;
    _error = null;
    _code.clear();
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Wachtwoord vergeten')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(TokSpace.s6),
          children: [
            ContentWidth(
              maxWidth: TokComponent.modalMaxWidth,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (_error != null) ...[
                    StatusMessage(kind: StatusKind.error, message: _error!),
                    const SizedBox(height: TokSpace.s4),
                  ],
                  _codeSent ? _confirmStep(context) : _requestStep(context),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _requestStep(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Form(
      key: _requestForm,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'Vul het e-mailadres van je account in. Je krijgt een code waarmee je een nieuw wachtwoord kiest.',
            style: text.bodyLarge,
          ),
          const SizedBox(height: TokSpace.s6),
          TextFormField(
            controller: _email,
            decoration: const InputDecoration(labelText: 'E-mailadres'),
            keyboardType: TextInputType.emailAddress,
            textInputAction: TextInputAction.done,
            autofillHints: const [AutofillHints.email],
            autocorrect: false,
            onFieldSubmitted: (_) => _request(),
            validator: AuthForm.email,
          ),
          const SizedBox(height: TokSpace.s6),
          BusyButton(label: 'Code versturen', busy: _busy, onPressed: _request),
        ],
      ),
    );
  }

  Widget _confirmStep(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Form(
      key: _confirmForm,
      child: AutofillGroup(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            StatusMessage(
              kind: StatusKind.info,
              message:
                  'Als er een account is met ${_email.text.trim()}, staat er nu een code in je mail. De code is 30 '
                  'minuten geldig. Kijk ook in je map met ongewenste mail.',
            ),
            const SizedBox(height: TokSpace.s6),
            TextFormField(
              controller: _code,
              decoration: const InputDecoration(labelText: 'Code uit de e-mail', hintText: 'ABCD-EFGH'),
              textCapitalization: TextCapitalization.characters,
              textInputAction: TextInputAction.next,
              autofillHints: const [AutofillHints.oneTimeCode],
              autocorrect: false,
              enableSuggestions: false,
              validator: (value) {
                final code = (value ?? '').replaceAll(RegExp(r'[\s-]'), '');
                if (code.isEmpty) return 'Vul de code uit de e-mail in.';
                if (code.length != 8) return 'De code heeft 8 tekens, bijvoorbeeld ABCD-EFGH.';
                return null;
              },
            ),
            const SizedBox(height: TokSpace.s4),
            PasswordField(
              controller: _password,
              label: 'Nieuw wachtwoord',
              isNew: true,
              helperText: AuthForm.passwordHint,
              validator: AuthForm.newPassword,
              onSubmitted: (_) => _confirm(),
            ),
            const SizedBox(height: TokSpace.s6),
            BusyButton(label: 'Wachtwoord opslaan', busy: _busy, onPressed: _confirm),
            const SizedBox(height: TokSpace.s4),
            Text('Geen code gekregen?', style: text.bodyMedium, textAlign: TextAlign.center),
            TextButton(onPressed: _busy ? null : _startOver, child: const Text('Nieuwe code aanvragen')),
          ],
        ),
      ),
    );
  }
}
