import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../design/design.dart';

/// De vijf hoofdonderdelen. Onderaan op telefoons, als zijbalk op tablets en grote schermen.
class AppShell extends StatelessWidget {
  const AppShell({super.key, required this.shell});

  final StatefulNavigationShell shell;

  static const _destinations = [
    (Icons.home_outlined, Icons.home, 'Home'),
    (Icons.calendar_month_outlined, Icons.calendar_month, 'Agenda'),
    (Icons.play_circle_outline, Icons.play_circle, 'Lezingen'),
    (Icons.explore_outlined, Icons.explore, 'Ontdekken'),
    (Icons.person_outline, Icons.person, 'Profiel'),
  ];

  /// Opnieuw op het actieve tabblad tikken brengt je terug naar het begin daarvan.
  void _select(int index) => shell.goBranch(index, initialLocation: index == shell.currentIndex);

  @override
  Widget build(BuildContext context) {
    final wide = MediaQuery.sizeOf(context).width >= 840;
    if (!wide) {
      return Scaffold(
        body: shell,
        bottomNavigationBar: NavigationBar(
          selectedIndex: shell.currentIndex,
          onDestinationSelected: _select,
          destinations: [
            for (final (icon, selectedIcon, label) in _destinations)
              NavigationDestination(icon: Icon(icon), selectedIcon: Icon(selectedIcon), label: label),
          ],
        ),
      );
    }
    final c = context.colors;
    return Scaffold(
      body: Row(
        children: [
          NavigationRail(
            selectedIndex: shell.currentIndex,
            onDestinationSelected: _select,
            labelType: NavigationRailLabelType.all,
            backgroundColor: c.surface,
            indicatorColor: c.primaryContainer,
            leading: const Padding(
              padding: EdgeInsets.symmetric(vertical: TokSpace.s4),
              child: BrandLogo(variant: BrandLogoVariant.compact, height: 40),
            ),
            destinations: [
              for (final (icon, selectedIcon, label) in _destinations)
                NavigationRailDestination(icon: Icon(icon), selectedIcon: Icon(selectedIcon), label: Text(label)),
            ],
          ),
          VerticalDivider(width: 1, color: c.divider),
          Expanded(child: shell),
        ],
      ),
    );
  }
}
