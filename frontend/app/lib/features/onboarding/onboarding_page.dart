import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/brand/brand.g.dart';
import '../../core/design/design.dart';
import '../../core/providers.dart';
import '../../core/widgets/content_width.dart';

/// Korte introductie bij de eerste start: wat de app doet, dat volgen geen lidmaatschap is, en dat alles ook
/// zonder account te bekijken is. Overslaan kan altijd.
class OnboardingPage extends ConsumerStatefulWidget {
  const OnboardingPage({super.key});

  @override
  ConsumerState<OnboardingPage> createState() => _OnboardingPageState();
}

class _OnboardingPageState extends ConsumerState<OnboardingPage> {
  final _pages = PageController();
  int _index = 0;

  static const _steps = [
    (
      Icons.mosque_outlined,
      'Moskeeën bij jou in de buurt',
      'Ontdek aangesloten moskeeën, zoek op naam of plaats en bekijk hun profiel, gebedstijden en contactgegevens.',
    ),
    (
      Icons.calendar_month_outlined,
      'Activiteiten, lezingen en nieuws',
      'Zie wat er gebeurt: de agenda, opgenomen lezingen en het laatste nieuws. Openbare content bekijk je zonder '
          'account.',
    ),
    (
      Icons.favorite_outline,
      'Volg je moskeeën',
      'Met een account volg je moskeeën en krijg je meldingen van hun nieuws. Volgen is gratis en is geen '
          'lidmaatschap van een moskee.',
    ),
  ];

  @override
  void dispose() {
    _pages.dispose();
    super.dispose();
  }

  Future<void> _finish({String to = '/'}) async {
    await ref.read(onboardingProvider.notifier).finish();
    if (!mounted) return;
    context.go('/');
    if (to != '/') context.push(to);
  }

  void _next() => _pages.nextPage(duration: TokMotion.normal, curve: TokMotion.standard);

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final c = context.colors;
    final last = _index == _steps.length - 1;
    return Scaffold(
      appBar: AppBar(
        title: const BrandLogo(height: 28),
        actions: [if (!last) TextButton(onPressed: _finish, child: const Text('Overslaan'))],
      ),
      body: SafeArea(
        child: ContentWidth(
          child: Column(
            children: [
              Expanded(
                child: PageView(
                  controller: _pages,
                  onPageChanged: (index) => setState(() => _index = index),
                  children: [
                    for (final (icon, title, body) in _steps)
                      SingleChildScrollView(
                        padding: const EdgeInsets.all(TokSpace.s6),
                        child: Column(
                          children: [
                            Icon(icon, size: 72, color: c.primary),
                            const SizedBox(height: TokSpace.s6),
                            Semantics(
                              header: true,
                              child: Text(title, style: text.headlineSmall, textAlign: TextAlign.center),
                            ),
                            const SizedBox(height: TokSpace.s3),
                            Text(body, style: text.bodyLarge, textAlign: TextAlign.center),
                          ],
                        ),
                      ),
                  ],
                ),
              ),
              Semantics(
                label: 'Stap ${_index + 1} van ${_steps.length}',
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    for (var i = 0; i < _steps.length; i++)
                      AnimatedContainer(
                        duration: TokMotion.normal,
                        margin: const EdgeInsets.symmetric(horizontal: TokSpace.s1),
                        width: i == _index ? 24 : 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: i == _index ? c.primary : c.divider,
                          borderRadius: BorderRadius.circular(TokRadius.full),
                        ),
                      ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(TokSpace.s4),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (last) ...[
                      FilledButton(onPressed: _finish, child: Text('Aan de slag met ${Brand.name}')),
                      const SizedBox(height: TokSpace.s2),
                      OutlinedButton(
                        onPressed: () => _finish(to: '/inloggen'),
                        child: const Text('Inloggen of account maken'),
                      ),
                    ] else
                      FilledButton(onPressed: _next, child: const Text('Volgende')),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
