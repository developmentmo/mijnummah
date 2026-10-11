import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../data/models.dart';
import '../../features/agenda/agenda_page.dart';
import '../../features/agenda/event_detail_page.dart';
import '../../features/announcements/announcement_detail_page.dart';
import '../../features/auth/forgot_password_page.dart';
import '../../features/auth/login_page.dart';
import '../../features/auth/register_page.dart';
import '../../features/design_preview/design_preview_page.dart';
import '../../features/discover/discover_page.dart';
import '../../features/home/home_page.dart';
import '../../features/lectures/lectures_page.dart';
import '../../features/lectures/media_detail_page.dart';
import '../../features/profile/account_page.dart';
import '../../features/profile/profile_page.dart';
import '../providers.dart';
import 'app_shell.dart';

/// Navigatie: vijf tabbladen met elk hun eigen geschiedenis, en detailpagina's die over de tabbladen heen openen.
///
/// | Pad | Scherm |
/// |---|---|
/// | `/`, `/agenda`, `/lezingen`, `/ontdekken`, `/profiel` | De vijf tabbladen; `/agenda?categorie=YOUTH` filtert |
/// | `/activiteit/:id`, `/lezing/:id`, `/aankondiging/:id` | Details |
/// | `/inloggen`, `/account-aanmaken`, `/wachtwoord-vergeten` | Inloggen, registreren, wachtwoord herstellen |
/// | `/account` | Wachtwoord wijzigen en account verwijderen |
/// | `/design` | Designpreview (alleen debug) |
GoRouter buildRouter({String initialLocation = '/'}) {
  final rootKey = GlobalKey<NavigatorState>(debugLabel: 'root');
  return GoRouter(
    navigatorKey: rootKey,
    initialLocation: initialLocation,
    routes: [
      StatefulShellRoute.indexedStack(
        builder: (context, state, shell) => AppShell(shell: shell),
        branches: [
          StatefulShellBranch(
            routes: [GoRoute(path: '/', builder: (context, state) => const HomePage())],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/agenda',
                builder: (context, state) {
                  final category = state.uri.queryParameters['categorie'];
                  return AgendaPage(
                    initialCategory: category == null
                        ? null
                        : EventCategory.values.where((c) => c.apiValue == category).firstOrNull,
                  );
                },
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [GoRoute(path: '/lezingen', builder: (context, state) => const LecturesPage())],
          ),
          StatefulShellBranch(
            routes: [GoRoute(path: '/ontdekken', builder: (context, state) => const DiscoverPage())],
          ),
          StatefulShellBranch(
            routes: [GoRoute(path: '/profiel', builder: (context, state) => const ProfilePage())],
          ),
        ],
      ),
      GoRoute(
        path: '/activiteit/:id',
        parentNavigatorKey: rootKey,
        builder: (context, state) => EventDetailPage(eventId: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/lezing/:id',
        parentNavigatorKey: rootKey,
        builder: (context, state) => MediaDetailPage(mediaId: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/aankondiging/:id',
        parentNavigatorKey: rootKey,
        builder: (context, state) => AnnouncementDetailPage(announcementId: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/inloggen',
        parentNavigatorKey: rootKey,
        pageBuilder: (context, state) => const MaterialPage(fullscreenDialog: true, child: LoginPage()),
      ),
      GoRoute(
        path: '/account-aanmaken',
        parentNavigatorKey: rootKey,
        builder: (context, state) => const RegisterPage(),
      ),
      GoRoute(
        path: '/wachtwoord-vergeten',
        parentNavigatorKey: rootKey,
        builder: (context, state) => ForgotPasswordPage(
          initialEmail: switch (state.extra) {
            final String email => email,
            _ => null,
          },
        ),
      ),
      GoRoute(path: '/account', parentNavigatorKey: rootKey, builder: (context, state) => const AccountPage()),
      if (kDebugMode)
        GoRoute(
          path: '/design',
          parentNavigatorKey: rootKey,
          builder: (context, state) => Consumer(
            builder: (context, ref, _) => DesignPreviewPage(
              themeMode: ref.watch(themeModeProvider),
              onThemeModeChanged: ref.read(themeModeProvider.notifier).set,
            ),
          ),
        ),
    ],
  );
}
