import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/app.dart';
import 'package:mijnummah_app/core/design/design.dart';

/// Scrollt de hele preview door, zodat ook onderaan gebouwde widgets op overflow worden gecontroleerd.
Future<void> scrollThrough(WidgetTester tester) async {
  final list = find.byType(Scrollable).first;
  for (var i = 0; i < 40; i++) {
    await tester.drag(list, const Offset(0, -600));
    await tester.pump(const Duration(milliseconds: 50));
  }
}

void main() {
  const phone = Size(360, 780);
  const tablet = Size(1024, 1366);

  Future<void> pumpApp(WidgetTester tester, {Size size = phone, double textScale = 1}) async {
    tester.view.physicalSize = size;
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = textScale;
    addTearDown(tester.view.reset);
    addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
    await tester.pumpWidget(const NoeroelIslamApp());
    await tester.pump();
  }

  testWidgets('toont de designpreview met logo en secties', (tester) async {
    await pumpApp(tester);
    expect(find.text('Designpreview'), findsOneWidget);
    expect(find.byType(BrandLogo), findsWidgets);
    expect(find.text('Kleuren'), findsOneWidget);
  });

  for (final (name, size, scale) in [
    ('telefoon', phone, 1.0),
    ('telefoon met 200% tekst', phone, 2.0),
    ('tablet', tablet, 1.0),
  ]) {
    testWidgets('rendert zonder overflow op $name', (tester) async {
      await pumpApp(tester, size: size, textScale: scale);
      await scrollThrough(tester);
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets('wisselt tussen licht en donker thema', (tester) async {
    await pumpApp(tester);
    BuildContext context() => tester.element(find.text('Designpreview'));

    await tester.tap(find.byTooltip('Donker'));
    await tester.pumpAndSettle();
    expect(Theme.of(context()).brightness, Brightness.dark);
    expect(context().colors, AppColors.dark);

    await tester.tap(find.byTooltip('Licht'));
    await tester.pumpAndSettle();
    expect(Theme.of(context()).brightness, Brightness.light);
  });

  testWidgets('voorbeeldknoppen zeggen eerlijk dat het een voorbeeld is', (tester) async {
    await pumpApp(tester, size: tablet);
    await tester.scrollUntilVisible(find.text('Aanmelden'), 300, scrollable: find.byType(Scrollable).first);
    await tester.tap(find.text('Aanmelden'));
    await tester.pump();
    expect(find.text('Dit is een voorbeeldknop in de designpreview.'), findsOneWidget);
  });

  testWidgets('meldingen hebben een voorleesbaar label naast de kleur', (tester) async {
    final handle = tester.ensureSemantics();
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light,
        home: const Scaffold(
          body: StatusMessage(kind: StatusKind.error, message: 'Video niet beschikbaar.'),
        ),
      ),
    );
    expect(find.bySemanticsLabel(RegExp('Fout')), findsOneWidget);
    handle.dispose();
  });

  testWidgets('skeleton staat stil als animaties zijn uitgeschakeld', (tester) async {
    await tester.pumpWidget(
      MediaQuery(
        data: const MediaQueryData(disableAnimations: true),
        child: MaterialApp(
          theme: AppTheme.light,
          home: const Scaffold(body: Skeleton()),
        ),
      ),
    );
    // pumpAndSettle zou bij een oneindige animatie een time-out geven.
    await tester.pumpAndSettle();
    expect(find.byType(Skeleton), findsOneWidget);
  });
}
