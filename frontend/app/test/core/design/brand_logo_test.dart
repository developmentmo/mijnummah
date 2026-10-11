import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/core/design/design.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('BrandLogo', () {
    for (final variant in BrandLogoVariant.values) {
      for (final brightness in Brightness.values) {
        final asset = BrandLogo.assetFor(variant, brightness);

        test('$asset is een geldige SVG die flutter_svg kan renderen', () async {
          final source = await rootBundle.loadString(asset);
          expect(source, contains('<svg'));
          expect(source, isNot(contains('<text')), reason: 'Woordmark moet uit paden bestaan, niet uit tekst');
          expect(source, isNot(contains('href=')), reason: 'Geen externe verwijzingen');
          final picture = await vg.loadPicture(SvgStringLoader(source), null);
          expect(picture.size.width, greaterThan(0));
          expect(picture.size.height, greaterThan(0));
          picture.picture.dispose();
        });
      }
    }

    testWidgets('kiest de lichte of donkere variant op basis van het thema', (tester) async {
      Future<String?> assetIn(ThemeData theme) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Theme(
              data: theme,
              child: const Scaffold(body: BrandLogo()),
            ),
          ),
        );
        final picture = tester.widget<SvgPicture>(find.byType(SvgPicture));
        return (picture.bytesLoader as SvgAssetLoader).assetName;
      }

      expect(await assetIn(AppTheme.light), 'assets/branding/logo-horizontal-light.svg');
      expect(await assetIn(AppTheme.dark), 'assets/branding/logo-horizontal-dark.svg');
    });

    testWidgets('heeft een label voor screenreaders', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.light,
          home: const Scaffold(body: BrandLogo()),
        ),
      );
      final picture = tester.widget<SvgPicture>(find.byType(SvgPicture));
      expect(picture.semanticsLabel, 'Noeroel Islam');
    });
  });
}
