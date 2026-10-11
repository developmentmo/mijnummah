import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api/api_exception.dart';
import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../core/widgets/content_width.dart';
import 'auth_form.dart';

/// Inloggen met e-mailadres en wachtwoord. Na het inloggen gaat de gebruiker terug naar waar hij was.
class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _form = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
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
      await ref.read(authControllerProvider.notifier).login(_email.text, _password.text);
      if (!mounted) return;
      final name = ref.read(authControllerProvider).value?.displayName;
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(name == null ? 'Je bent ingelogd.' : 'Welkom, $name.')));
      context.canPop() ? context.pop() : context.go('/profiel');
    } on AppFailure catch (e) {
      setState(
        () => _error = AuthForm.message(
          e,
          codes: const {
            'INVALID_CREDENTIALS': 'Het e-mailadres of wachtwoord klopt niet.',
            'VALIDATION_FAILED': 'Het e-mailadres of wachtwoord klopt niet.',
            'ACCOUNT_BLOCKED': 'Dit account is geblokkeerd. Neem contact op met de moskee.',
          },
          fallback: 'Inloggen is niet gelukt. Probeer het later opnieuw.',
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
      appBar: AppBar(title: const Text('Inloggen')),
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
                      const Center(child: BrandLogo(variant: BrandLogoVariant.stacked, height: 96)),
                      const SizedBox(height: TokSpace.s6),
                      Text(
                        'Log in om je aan te melden voor activiteiten en de moskee te volgen.',
                        style: text.bodyLarge,
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: TokSpace.s6),
                      if (_error != null) ...[
                        StatusMessage(kind: StatusKind.error, message: _error!),
                        const SizedBox(height: TokSpace.s4),
                      ],
                      TextFormField(
                        controller: _email,
                        decoration: const InputDecoration(labelText: 'E-mailadres'),
                        keyboardType: TextInputType.emailAddress,
                        textInputAction: TextInputAction.next,
                        autofillHints: const [AutofillHints.email, AutofillHints.username],
                        autocorrect: false,
                        validator: AuthForm.email,
                      ),
                      const SizedBox(height: TokSpace.s4),
                      PasswordField(controller: _password, onSubmitted: (_) => _submit()),
                      Align(
                        alignment: Alignment.centerRight,
                        child: TextButton(
                          onPressed: () => context.push('/wachtwoord-vergeten', extra: _email.text.trim()),
                          child: const Text('Wachtwoord vergeten?'),
                        ),
                      ),
                      const SizedBox(height: TokSpace.s2),
                      BusyButton(label: 'Inloggen', busy: _busy, onPressed: _submit),
                      const SizedBox(height: TokSpace.s6),
                      Text('Nog geen account?', style: text.bodyMedium, textAlign: TextAlign.center),
                      const SizedBox(height: TokSpace.s2),
                      OutlinedButton(
                        onPressed: () => context.push('/account-aanmaken'),
                        child: const Text('Account aanmaken'),
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
