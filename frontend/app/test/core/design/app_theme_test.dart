import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
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

    test('merkkleuren uit de huisstijl staan in het palet', () {
      expect(TokPalette.green800, const Color(0xFF123C32));
      expect(TokPalette.green500, const Color(0xFF2F7965));
      expect(TokPalette.neutral50, const Color(0xFFF7F3E9));
      expect(TokPalette.gold400, const Color(0xFFC6A45A));
      expect(TokPalette.neutral900, const Color(0xFF242A27));
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
