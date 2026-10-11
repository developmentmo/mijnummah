import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// Kop van een detailpagina. Is de pagina direct geopend (via een link, zonder pagina eronder), dan staat er een
/// knop naar Home in plaats van een terugknop.
class DetailAppBar extends StatelessWidget implements PreferredSizeWidget {
  const DetailAppBar({super.key, required this.title});

  final String title;

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    return AppBar(
      title: Text(title),
      leading: context.canPop()
          ? null
          : IconButton(tooltip: 'Naar Home', icon: const Icon(Icons.home_outlined), onPressed: () => context.go('/')),
    );
  }
}
