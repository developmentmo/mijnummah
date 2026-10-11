import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api/api_exception.dart';
import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../core/widgets/content_width.dart';
import '../../core/widgets/section_header.dart';
import '../auth/auth_form.dart';

/// Accountinstellingen: wachtwoord wijzigen en het account verwijderen.
class AccountPage extends ConsumerWidget {
  const AccountPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authControllerProvider).value;
    return Scaffold(
      appBar: AppBar(title: const Text('Account')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(TokSpace.s4, TokSpace.s2, TokSpace.s4, TokSpace.s8),
          children: [
            ContentWidth(
              child: user == null
                  ? Padding(
                      padding: const EdgeInsets.only(top: TokSpace.s6),
                      child: StatusMessage(
                        kind: StatusKind.info,
                        message: 'Je bent niet ingelogd.',
                        actionLabel: 'Inloggen',
                        onAction: () => context.push('/inloggen'),
                      ),
                    )
                  : Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Text('Ingelogd als ${user.email}', style: Theme.of(context).textTheme.bodyMedium),
                        const SectionHeader(title: 'Wachtwoord wijzigen'),
                        const _ChangePassword(),
                        const SectionHeader(title: 'Account verwijderen'),
                        const _DeleteAccount(),
                      ],
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ChangePassword extends ConsumerStatefulWidget {
  const _ChangePassword();

  @override
  ConsumerState<_ChangePassword> createState() => _ChangePasswordState();
}

class _ChangePasswordState extends ConsumerState<_ChangePassword> {
  final _form = GlobalKey<FormState>();
  final _current = TextEditingController();
  final _new = TextEditingController();
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _current.dispose();
    _new.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_busy || !(_form.currentState?.validate() ?? false)) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await ref.read(authServiceProvider).changePassword(currentPassword: _current.text, newPassword: _new.text);
      if (!mounted) return;
      _form.currentState?.reset();
      _current.clear();
      _new.clear();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Je wachtwoord is gewijzigd. Op andere toestellen ben je uitgelogd.')),
      );
    } on AppFailure catch (e) {
      setState(
        () => _error = AuthForm.message(
          e,
          codes: const {'CURRENT_PASSWORD_INCORRECT': 'Je huidige wachtwoord klopt niet.'},
          fallback: 'Je wachtwoord kon niet worden gewijzigd. Probeer het later opnieuw.',
        ),
      );
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(TokComponent.cardPadding),
      child: Form(
        key: _form,
        child: AutofillGroup(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (_error != null) ...[
                StatusMessage(kind: StatusKind.error, message: _error!),
                const SizedBox(height: TokSpace.s4),
              ],
              PasswordField(controller: _current, label: 'Huidig wachtwoord', textInputAction: TextInputAction.next),
              const SizedBox(height: TokSpace.s4),
              PasswordField(
                controller: _new,
                label: 'Nieuw wachtwoord',
                isNew: true,
                helperText: AuthForm.passwordHint,
                validator: AuthForm.newPassword,
                onSubmitted: (_) => _submit(),
              ),
              const SizedBox(height: TokSpace.s4),
              BusyButton(label: 'Wachtwoord wijzigen', busy: _busy, onPressed: _submit),
            ],
          ),
        ),
      ),
    ),
  );
}

class _DeleteAccount extends ConsumerWidget {
  const _DeleteAccount();

  Future<void> _delete(BuildContext context, WidgetRef ref) async {
    final deleted = await showDialog<bool>(context: context, builder: (context) => const _DeleteDialog());
    if (deleted != true || !context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Je account is verwijderd.')));
    context.go('/profiel');
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.colors;
    final text = Theme.of(context).textTheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(TokComponent.cardPadding),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Je account wordt definitief verwijderd, met je aanmeldingen voor activiteiten, het volgen van de moskee, '
              'je meldingsvoorkeuren en de koppeling met je toestellen. Dit kan niet ongedaan worden gemaakt.',
              style: text.bodyMedium,
            ),
            const SizedBox(height: TokSpace.s4),
            OutlinedButton.icon(
              style: OutlinedButton.styleFrom(
                foregroundColor: c.error,
                side: BorderSide(color: c.error),
              ),
              onPressed: () => _delete(context, ref),
              icon: const Icon(Icons.delete_outline),
              label: const Text('Account verwijderen'),
            ),
          ],
        ),
      ),
    );
  }
}

/// Vraagt het wachtwoord ter bevestiging en verwijdert het account. Sluit met true als dat is gelukt.
class _DeleteDialog extends ConsumerStatefulWidget {
  const _DeleteDialog();

  @override
  ConsumerState<_DeleteDialog> createState() => _DeleteDialogState();
}

class _DeleteDialogState extends ConsumerState<_DeleteDialog> {
  final _form = GlobalKey<FormState>();
  final _password = TextEditingController();
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
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
      await ref.read(authControllerProvider.notifier).deleteAccount(_password.text);
      if (mounted) Navigator.pop(context, true);
    } on AppFailure catch (e) {
      setState(
        () => _error = AuthForm.message(
          e,
          codes: const {
            'CURRENT_PASSWORD_INCORRECT': 'Je wachtwoord klopt niet.',
            'ACCOUNT_DELETION_BLOCKED':
                'Je beheert een moskee of hebt content geplaatst. Zo\'n account kan alleen de beheerder van het '
                'platform verwijderen. Neem contact op met de moskee.',
          },
          fallback: 'Je account kon niet worden verwijderd. Probeer het later opnieuw.',
        ),
      );
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return AlertDialog(
      title: const Text('Account definitief verwijderen?'),
      content: Form(
        key: _form,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (_error != null) ...[
              StatusMessage(kind: StatusKind.error, message: _error!),
              const SizedBox(height: TokSpace.s4),
            ],
            const Text('Vul ter bevestiging je wachtwoord in.'),
            const SizedBox(height: TokSpace.s4),
            PasswordField(controller: _password, onSubmitted: (_) => _submit()),
          ],
        ),
      ),
      scrollable: true,
      actions: [
        TextButton(onPressed: _busy ? null : () => Navigator.pop(context, false), child: const Text('Annuleren')),
        FilledButton(
          style: FilledButton.styleFrom(backgroundColor: scheme.error, foregroundColor: scheme.onError),
          onPressed: _busy ? null : _submit,
          child: const Text('Verwijderen'),
        ),
      ],
    );
  }
}
