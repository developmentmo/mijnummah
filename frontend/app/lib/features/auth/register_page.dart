import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api/api_exception.dart';
import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../core/widgets/content_width.dart';
import 'auth_form.dart';

/// Zelf een account aanmaken. Daarna is de gebruiker direct ingelogd.
class RegisterPage extends ConsumerStatefulWidget {
  const RegisterPage({super.key});

  @override
  ConsumerState<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends ConsumerState<RegisterPage> {
  final _form = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_busy || !(_form.currentState?.validate() ?? false)) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await ref
          .read(authControllerProvider.notifier)
          .register(email: _email.text, displayName: _name.text, password: _password.text);
      if (!mounted) return;
      final name = ref.read(authControllerProvider).value?.displayName ?? _name.text.trim();
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Welkom, $name. Je account is aangemaakt.')));
      context.go('/profiel');
    } on AppFailure catch (e) {
      setState(
        () => _error = AuthForm.message(
          e,
          codes: const {
            'EMAIL_ALREADY_REGISTERED':
                'Er is al een account met dit e-mailadres. Log in, of herstel je wachtwoord als je het kwijt bent.',
            'VALIDATION_FAILED': 'Controleer je naam en e-mailadres.',
          },
          fallback: 'Je account kon niet worden aangemaakt. Probeer het later opnieuw.',
        ),
      );
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Account aanmaken')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(TokSpace.s6),
          children: [
            ContentWidth(
              maxWidth: TokComponent.modalMaxWidth,
              child: Form(
                key: _form,
                child: AutofillGroup(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(
                        'Met een account kun je je aanmelden voor activiteiten en de moskee volgen. Een account is '
                        'gratis en is geen lidmaatschap van de moskee.',
                        style: text.bodyLarge,
                      ),
                      const SizedBox(height: TokSpace.s6),
                      if (_error != null) ...[
                        StatusMessage(kind: StatusKind.error, message: _error!),
                        const SizedBox(height: TokSpace.s4),
                      ],
                      TextFormField(
                        controller: _name,
                        decoration: const InputDecoration(
                          labelText: 'Naam',
                          helperText: 'Zo zie je jezelf in de app. De moskee ziet deze naam bij je aanmeldingen.',
                          helperMaxLines: 2,
                        ),
                        textCapitalization: TextCapitalization.words,
                        textInputAction: TextInputAction.next,
                        autofillHints: const [AutofillHints.name],
                        maxLength: 100,
                        validator: (value) => (value ?? '').trim().isEmpty ? 'Vul je naam in.' : null,
                      ),
                      const SizedBox(height: TokSpace.s2),
                      TextFormField(
                        controller: _email,
                        decoration: const InputDecoration(labelText: 'E-mailadres'),
                        keyboardType: TextInputType.emailAddress,
                        textInputAction: TextInputAction.next,
                        autofillHints: const [AutofillHints.email],
                        autocorrect: false,
                        validator: AuthForm.email,
                      ),
                      const SizedBox(height: TokSpace.s4),
                      PasswordField(
                        controller: _password,
                        isNew: true,
                        helperText: AuthForm.passwordHint,
                        validator: AuthForm.newPassword,
                        onSubmitted: (_) => _submit(),
                      ),
                      const SizedBox(height: TokSpace.s6),
                      BusyButton(label: 'Account aanmaken', busy: _busy, onPressed: _submit),
                      const SizedBox(height: TokSpace.s4),
                      Text(
                        'Je kunt je account later altijd zelf verwijderen via Profiel.',
                        style: text.bodySmall,
                        textAlign: TextAlign.center,
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
