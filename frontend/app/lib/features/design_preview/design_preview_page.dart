import 'package:flutter/material.dart';

import '../../core/design/design.dart';

/// Interne pagina die alle tokens en basiscomponenten toont, in licht en donker.
/// Bedoeld voor ontwerp- en ontwikkelcontrole; bevat geen echte content.
class DesignPreviewPage extends StatelessWidget {
  const DesignPreviewPage({super.key, required this.themeMode, required this.onThemeModeChanged});

  final ThemeMode themeMode;
  final ValueChanged<ThemeMode> onThemeModeChanged;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const BrandLogo(height: 32),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: TokSpace.s3),
            child: _ThemeModeMenu(themeMode: themeMode, onChanged: onThemeModeChanged),
          ),
        ],
      ),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: TokSize.contentMaxWidth),
          child: ListView(
            padding: const EdgeInsets.all(TokSpace.s4),
            children: const [
              _Intro(),
              _Section(title: "Logo's", child: _Logos()),
              _Section(title: 'Kleuren', child: _Colors()),
              _Section(title: 'Typografie', child: _Typography()),
              _Section(title: 'Knoppen', child: _Buttons()),
              _Section(title: 'Invoervelden', child: _Inputs()),
              _Section(title: 'Cards', child: _Cards()),
              _Section(title: 'Meldingen', child: _Messages()),
              _Section(title: 'Laden, leeg en fout', child: _States()),
              _Section(title: 'Navigatie', child: _Navigation()),
            ],
          ),
        ),
      ),
    );
  }
}

void _showExample(BuildContext context) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(const SnackBar(content: Text('Dit is een voorbeeldknop in de designpreview.')));
}

class _ThemeModeMenu extends StatelessWidget {
  const _ThemeModeMenu({required this.themeMode, required this.onChanged});

  final ThemeMode themeMode;
  final ValueChanged<ThemeMode> onChanged;

  @override
  Widget build(BuildContext context) {
    return SegmentedButton<ThemeMode>(
      showSelectedIcon: false,
      segments: const [
        ButtonSegment(value: ThemeMode.system, icon: Icon(Icons.brightness_auto_outlined), tooltip: 'Systeem'),
        ButtonSegment(value: ThemeMode.light, icon: Icon(Icons.light_mode_outlined), tooltip: 'Licht'),
        ButtonSegment(value: ThemeMode.dark, icon: Icon(Icons.dark_mode_outlined), tooltip: 'Donker'),
      ],
      selected: {themeMode},
      onSelectionChanged: (selection) => onChanged(selection.first),
    );
  }
}

class _Intro extends StatelessWidget {
  const _Intro();

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(bottom: TokSpace.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Designpreview', style: text.displayMedium),
          const SizedBox(height: TokSpace.s2),
          Text(
            'Interne overzichtspagina van het design system. Alle teksten en voorbeelden op deze pagina zijn '
            'voorbeeldcontent.',
            style: text.bodyLarge?.copyWith(color: context.colors.onSurfaceMuted),
          ),
        ],
      ),
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: TokSpace.s8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Semantics(header: true, child: Text(title, style: Theme.of(context).textTheme.headlineMedium)),
          const SizedBox(height: TokSpace.s4),
          child,
        ],
      ),
    );
  }
}

class _Logos extends StatelessWidget {
  const _Logos();

  @override
  Widget build(BuildContext context) {
    return const Wrap(
      spacing: TokSpace.s6,
      runSpacing: TokSpace.s6,
      crossAxisAlignment: WrapCrossAlignment.end,
      children: [
        BrandLogo(height: 48),
        BrandLogo(variant: BrandLogoVariant.stacked, height: 120),
        BrandLogo(variant: BrandLogoVariant.compact, height: 48),
        BrandLogo(variant: BrandLogoVariant.compact, height: 24),
      ],
    );
  }
}

class _Colors extends StatelessWidget {
  const _Colors();

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final swatches = <(String, Color, Color)>[
      ('Primair', c.primary, c.onPrimary),
      ('Secundair (jade)', c.secondary, c.onSecondary),
      ('Accent (goud)', c.accent, c.onPrimaryContainer),
      ('Achtergrond', c.background, c.onBackground),
      ('Oppervlak', c.surface, c.onSurface),
      ('Gedempt oppervlak', c.surfaceMuted, c.onSurface),
      ('Primaire container', c.primaryContainer, c.onPrimaryContainer),
      ('Succes', c.successContainer, c.onSuccessContainer),
      ('Waarschuwing', c.warningContainer, c.onWarningContainer),
      ('Fout', c.errorContainer, c.onErrorContainer),
      ('Informatie', c.infoContainer, c.onInfoContainer),
      ('Omgekeerd', c.surfaceInverse, c.onSurfaceInverse),
    ];
    return Wrap(
      spacing: TokSpace.s3,
      runSpacing: TokSpace.s3,
      children: [
        for (final (name, color, on) in swatches)
          Container(
            width: 160,
            height: 72,
            padding: const EdgeInsets.all(TokSpace.s3),
            decoration: BoxDecoration(
              color: color,
              borderRadius: BorderRadius.circular(TokRadius.md),
              border: Border.all(color: c.border),
            ),
            alignment: Alignment.bottomLeft,
            child: Text(name, style: TokType.labelMedium.copyWith(color: on)),
          ),
      ],
    );
  }
}

class _Typography extends StatelessWidget {
  const _Typography();

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Welkom bij de moskee', style: t.displayLarge),
        Text('Aankomende evenementen', style: t.headlineLarge),
        Text('Lezing over geduld', style: t.headlineSmall),
        Text('Titel van een card', style: t.titleLarge),
        Text('Ondertitel en sprekers', style: t.titleMedium),
        const SizedBox(height: TokSpace.s2),
        Text(
          'Broodtekst is goed leesbaar, ook voor oudere bezoekers. De regelafstand is ruim en de tekst '
          'schaalt mee met de lettergrootte die iemand op het toestel heeft ingesteld.',
          style: t.bodyLarge,
        ),
        Text('Kleinere tekst voor toelichting en metadata.', style: t.bodySmall),
        const SizedBox(height: TokSpace.s2),
        Text('LABEL · VRIJDAG 18:30', style: t.labelMedium),
        const SizedBox(height: TokSpace.s3),
        Text(
          'بِسْمِ ٱللَّٰهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ',
          style: TokType.arabic.copyWith(color: context.colors.onSurface),
          textDirection: TextDirection.rtl,
        ),
      ],
    );
  }
}

class _Buttons extends StatelessWidget {
  const _Buttons();

  @override
  Widget build(BuildContext context) {
    void onPressed() => _showExample(context);
    return Wrap(
      spacing: TokSpace.s3,
      runSpacing: TokSpace.s3,
      children: [
        FilledButton(onPressed: onPressed, child: const Text('Aanmelden')),
        FilledButton.icon(
          onPressed: onPressed,
          icon: const Icon(Icons.calendar_month_outlined),
          label: const Text('Agenda'),
        ),
        OutlinedButton(onPressed: onPressed, child: const Text('Meer informatie')),
        TextButton(onPressed: onPressed, child: const Text('Alles bekijken')),
        const FilledButton(onPressed: null, child: Text('Uitgeschakeld')),
      ],
    );
  }
}

class _Inputs extends StatelessWidget {
  const _Inputs();

  @override
  Widget build(BuildContext context) {
    return const Column(
      children: [
        TextField(
          decoration: InputDecoration(labelText: 'Naam', hintText: 'Voor- en achternaam'),
        ),
        SizedBox(height: TokSpace.s4),
        TextField(
          decoration: InputDecoration(
            labelText: 'E-mailadres',
            errorText: 'Vul een geldig e-mailadres in, bijvoorbeeld naam@voorbeeld.nl.',
          ),
        ),
        SizedBox(height: TokSpace.s4),
        TextField(
          decoration: InputDecoration(
            labelText: 'Zoeken in lezingen',
            prefixIcon: Icon(Icons.search),
            helperText: 'Zoek op titel of spreker.',
          ),
        ),
      ],
    );
  }
}

class _Cards extends StatelessWidget {
  const _Cards();

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final t = Theme.of(context).textTheme;
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            height: 140,
            color: c.primaryContainer,
            alignment: Alignment.center,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.image_outlined, color: c.onPrimaryContainer, size: TokIconSize.xl),
                Text('Voorbeeldafbeelding', style: t.labelMedium?.copyWith(color: c.onPrimaryContainer)),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(TokComponent.cardPadding),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('VOORBEELD · LEZING', style: t.labelSmall?.copyWith(color: c.accentText)),
                const SizedBox(height: TokSpace.s1),
                Text('Titel van een voorbeeldlezing', style: t.titleLarge),
                const SizedBox(height: TokSpace.s1),
                Text('Vrijdag 18:30 · Grote zaal', style: t.bodySmall),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Messages extends StatelessWidget {
  const _Messages();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const StatusMessage(kind: StatusKind.info, message: 'De agenda wordt elke week bijgewerkt.'),
        const SizedBox(height: TokSpace.s3),
        const StatusMessage(kind: StatusKind.success, title: 'Aangemeld', message: 'Je aanmelding is ontvangen.'),
        const SizedBox(height: TokSpace.s3),
        const StatusMessage(
          kind: StatusKind.warning,
          message: 'Je bekijkt een opgeslagen versie omdat je offline bent.',
        ),
        const SizedBox(height: TokSpace.s3),
        StatusMessage(
          kind: StatusKind.error,
          message: 'De video kon niet worden geladen.',
          actionLabel: 'Opnieuw proberen',
          onAction: () => _showExample(context),
        ),
      ],
    );
  }
}

class _States extends StatelessWidget {
  const _States();

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Skeleton(height: 140, radius: TokRadius.lg),
        const SizedBox(height: TokSpace.s3),
        const Skeleton(width: 220, height: 20),
        const SizedBox(height: TokSpace.s2),
        const Skeleton(width: 140),
        const SizedBox(height: TokSpace.s6),
        const StatePlaceholder(
          icon: Icons.event_busy_outlined,
          title: 'Geen evenementen gepland',
          message: 'Zodra er nieuwe activiteiten zijn, zie je ze hier.',
        ),
        StatePlaceholder.error(onRetry: () => _showExample(context)),
      ],
    );
  }
}

class _Navigation extends StatefulWidget {
  const _Navigation();

  @override
  State<_Navigation> createState() => _NavigationState();
}

class _NavigationState extends State<_Navigation> {
  int _index = 0;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(TokRadius.lg),
      child: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'Home'),
          NavigationDestination(
            icon: Icon(Icons.calendar_month_outlined),
            selectedIcon: Icon(Icons.calendar_month),
            label: 'Agenda',
          ),
          NavigationDestination(
            icon: Icon(Icons.play_circle_outline),
            selectedIcon: Icon(Icons.play_circle),
            label: 'Lezingen',
          ),
          NavigationDestination(
            icon: Icon(Icons.explore_outlined),
            selectedIcon: Icon(Icons.explore),
            label: 'Ontdekken',
          ),
          NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: 'Profiel'),
        ],
      ),
    );
  }
}
