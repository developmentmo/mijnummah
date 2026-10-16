import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/core/brand/brand.g.dart';
import 'package:mijnummah_app/core/design/design.dart';

void main() {
  group('AppTheme', () {
    test('licht thema gebruikt de semantische tokens', () {
      final theme = AppTheme.light;
      expect(theme.brightness, Brightness.light);
      expect(theme.colorScheme.primary, TokLight.primary);
      expect(theme.colorScheme.onPrimary, TokLight.onPrimary);
      expect(theme.colorScheme.error, TokLight.error);
      expect(theme.scaffoldBackgroundColor, TokLight.background);
      expect(theme.extension<AppColors>(), AppColors.light);
    });

    test('donker thema gebruikt de semantische tokens', () {
      final theme = AppTheme.dark;
      expect(theme.brightness, Brightness.dark);
      expect(theme.colorScheme.primary, TokDark.primary);
      expect(theme.colorScheme.surface, TokDark.surface);
      expect(theme.scaffoldBackgroundColor, TokDark.background);
      expect(theme.extension<AppColors>(), AppColors.dark);
    });

    test('merkkleuren uit de merkconfiguratie staan in het palet', () {
      // De tokens zijn gegenereerd uit brands/<slug>/brand.json; dit faalt als ze niet bij het toegepaste merk horen.
      final brand =
          jsonDecode(File('../../brands/${Brand.slug}/brand.json').readAsStringSync()) as Map<String, dynamic>;
      final colors = brand['colors'] as Map<String, dynamic>;
      Color hex(String group, String shade) =>
          Color(int.parse('FF${((colors[group] as Map<String, dynamic>)[shade] as String).substring(1)}', radix: 16));
      expect(TokPalette.primary800, hex('primary', '800'));
      expect(TokPalette.primary500, hex('primary', '500'));
      expect(TokPalette.accent400, hex('accent', '400'));
      if (colors.containsKey('neutral')) expect(TokPalette.neutral50, hex('neutral', '50'));
    });

    test('koppen gebruiken Fraunces en lopende tekst Inter', () {
      final text = AppTheme.light.textTheme;
      expect(text.displayLarge?.fontFamily, TokFont.display);
      expect(text.headlineMedium?.fontFamily, TokFont.display);
      expect(text.bodyLarge?.fontFamily, TokFont.body);
      expect(text.labelLarge?.fontFamily, TokFont.body);
      expect(TokFont.display, 'Fraunces');
      expect(TokFont.body, 'Inter');
    });

    test('knoppen en invoervelden halen de minimale aanraakgrootte', () {
      final size = AppTheme.light.filledButtonTheme.style?.minimumSize?.resolve({});
      expect(size, isNotNull);
      expect(size!.height, greaterThanOrEqualTo(44));
      expect(TokComponent.inputHeight, greaterThanOrEqualTo(TokSize.touchTarget));
    });

    test('AppColors.lerp mengt tussen thema\'s', () {
      final halfway = AppColors.light.lerp(AppColors.dark, 0.5);
      expect(halfway.primary, Color.lerp(TokLight.primary, TokDark.primary, 0.5));
      expect(AppColors.light.lerp(null, 0.5), AppColors.light);
    });
  });
}
