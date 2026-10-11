import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api/api_exception.dart';
import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../core/widgets/content_width.dart';

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
  bool _obscure = true;
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
    } on ApiException catch (e) {
      setState(() => _error = _message(e));
    } on NetworkException {
      setState(() => _error = 'Geen verbinding met de moskee. Controleer je internet en probeer het opnieuw.');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  static String _message(ApiException e) => switch (e.code) {
    'INVALID_CREDENTIALS' || 'VALIDATION_FAILED' => 'Het e-mailadres of wachtwoord klopt niet.',
    'ACCOUNT_BLOCKED' => 'Dit account is geblokkeerd. Neem contact op met de moskee.',
    'TOO_MANY_LOGIN_ATTEMPTS' || 'RATE_LIMITED' => _tooMany(e.retryAfter),
    _ => 'Inloggen is niet gelukt. Probeer het later opnieuw.',
  };

  static String _tooMany(Duration? retryAfter) {
    final minutes = retryAfter == null ? null : (retryAfter.inSeconds / 60).ceil();
    return minutes == null || minutes <= 1
        ? 'Te veel pogingen. Wacht even en probeer het dan opnieuw.'
        : 'Te veel pogingen. Probeer het over $minutes minuten opnieuw.';
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
                        validator: (value) {
                          final email = value?.trim() ?? '';
                          if (email.isEmpty) return 'Vul je e-mailadres in.';
                          if (!email.contains('@') || email.startsWith('@') || email.endsWith('@')) {
                            return 'Vul een geldig e-mailadres in.';
                          }
                          return null;
                        },
                      ),
                      const SizedBox(height: TokSpace.s4),
                      TextFormField(
                        controller: _password,
                        decoration: InputDecoration(
                          labelText: 'Wachtwoord',
                          suffixIcon: IconButton(
                            tooltip: _obscure ? 'Wachtwoord tonen' : 'Wachtwoord verbergen',
                            icon: Icon(_obscure ? Icons.visibility_outlined : Icons.visibility_off_outlined),
                            onPressed: () => setState(() => _obscure = !_obscure),
                          ),
                        ),
                        obscureText: _obscure,
                        textInputAction: TextInputAction.done,
                        autofillHints: const [AutofillHints.password],
                        onFieldSubmitted: (_) => _submit(),
                        validator: (value) => (value ?? '').isEmpty ? 'Vul je wachtwoord in.' : null,
                      ),
                      const SizedBox(height: TokSpace.s6),
                      FilledButton(
                        onPressed: _busy ? null : _submit,
                        child: _busy
                            ? const SizedBox.square(
                                dimension: TokIconSize.md,
                                child: CircularProgressIndicator(strokeWidth: 2),
                              )
                            : const Text('Inloggen'),
                      ),
                      const SizedBox(height: TokSpace.s4),
                      Text(
                        'Nog geen account? Zelf een account aanmaken kan nog niet in deze versie van de app; vraag er '
                        'een aan bij de moskee.',
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
