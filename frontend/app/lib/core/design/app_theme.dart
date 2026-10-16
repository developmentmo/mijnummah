import 'package:flutter/material.dart';

import 'tokens.g.dart';

/// Bouwt het Material 3-thema uit de design tokens.
///
/// Widgets gebruiken kleuren en tekststijlen uit het thema (`Theme.of(context)`, `context.colors`),
/// nooit losse waarden. Nieuwe waarden horen in `design/tokens/tokens.json`.
abstract final class AppTheme {
  static ThemeData get light => _build(Brightness.light, AppColors.light);

  static ThemeData get dark => _build(Brightness.dark, AppColors.dark);

  static TextTheme textTheme(AppColors c) {
    TextStyle s(TextStyle base, Color color) => base.copyWith(color: color);
    return TextTheme(
      displayLarge: s(TokType.displayLarge, c.onBackground),
      displayMedium: s(TokType.displayMedium, c.onBackground),
      displaySmall: s(TokType.headlineLarge, c.onBackground),
      headlineLarge: s(TokType.headlineLarge, c.onBackground),
      headlineMedium: s(TokType.headlineMedium, c.onBackground),
      headlineSmall: s(TokType.headlineSmall, c.onBackground),
      titleLarge: s(TokType.titleLarge, c.onSurface),
      titleMedium: s(TokType.titleMedium, c.onSurface),
      titleSmall: s(TokType.titleSmall, c.onSurface),
      bodyLarge: s(TokType.bodyLarge, c.onSurface),
      bodyMedium: s(TokType.bodyMedium, c.onSurface),
      bodySmall: s(TokType.bodySmall, c.onSurfaceMuted),
      labelLarge: s(TokType.labelLarge, c.onSurface),
      labelMedium: s(TokType.labelMedium, c.onSurface),
      labelSmall: s(TokType.labelSmall, c.onSurfaceMuted),
    );
  }

  static ThemeData _build(Brightness brightness, AppColors c) {
    final scheme = ColorScheme(
      brightness: brightness,
      primary: c.primary,
      onPrimary: c.onPrimary,
      primaryContainer: c.primaryContainer,
      onPrimaryContainer: c.onPrimaryContainer,
      secondary: c.secondary,
      onSecondary: c.onSecondary,
      secondaryContainer: c.primaryContainer,
      onSecondaryContainer: c.onPrimaryContainer,
      tertiary: c.accent,
      onTertiary: c.onPrimary,
      tertiaryContainer: c.accentContainer,
      onTertiaryContainer: c.onAccentContainer,
      error: c.error,
      onError: brightness == Brightness.light ? TokPalette.neutral0 : TokPalette.red900,
      errorContainer: c.errorContainer,
      onErrorContainer: c.onErrorContainer,
      surface: c.surface,
      onSurface: c.onSurface,
      onSurfaceVariant: c.onSurfaceMuted,
      surfaceContainerLowest: c.surface,
      surfaceContainerLow: c.background,
      surfaceContainer: c.surfaceMuted,
      surfaceContainerHigh: c.surfaceMuted,
      surfaceContainerHighest: c.surfaceMuted,
      outline: c.borderStrong,
      outlineVariant: c.border,
      inverseSurface: c.surfaceInverse,
      onInverseSurface: c.onSurfaceInverse,
      inversePrimary: brightness == Brightness.light ? TokPalette.primary200 : TokPalette.primary800,
      shadow: TokPalette.primary900,
      scrim: c.scrim,
    );
    final text = textTheme(c);
    const buttonShape = RoundedRectangleBorder(
      borderRadius: BorderRadius.all(Radius.circular(TokComponent.buttonRadius)),
    );
    const buttonSize = Size(TokSize.touchTarget, TokComponent.buttonHeightMd);
    const buttonPadding = EdgeInsets.symmetric(horizontal: TokComponent.buttonPaddingX);
    final inputRadius = BorderRadius.circular(TokComponent.inputRadius);
    OutlineInputBorder inputBorder(Color color, [double width = TokComponent.inputBorderWidth]) => OutlineInputBorder(
      borderRadius: inputRadius,
      borderSide: BorderSide(color: color, width: width),
    );

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: scheme,
      extensions: [c],
      scaffoldBackgroundColor: c.background,
      canvasColor: c.background,
      fontFamily: TokFont.body,
      textTheme: text,
      dividerTheme: DividerThemeData(color: c.divider, thickness: 1, space: TokSpace.s4),
      focusColor: c.focus.withValues(alpha: 0.16),
      appBarTheme: AppBarTheme(
        backgroundColor: c.background,
        foregroundColor: c.onBackground,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 1,
        centerTitle: false,
        titleTextStyle: text.titleLarge,
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: c.primary,
          foregroundColor: c.onPrimary,
          disabledBackgroundColor: c.disabled,
          disabledForegroundColor: c.onDisabled,
          minimumSize: buttonSize,
          padding: buttonPadding,
          shape: buttonShape,
          textStyle: TokType.labelLarge,
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: c.surfaceRaised,
          foregroundColor: c.primary,
          minimumSize: buttonSize,
          padding: buttonPadding,
          shape: buttonShape,
          textStyle: TokType.labelLarge,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: c.primary,
          disabledForegroundColor: c.onDisabled,
          side: BorderSide(color: c.borderStrong),
          minimumSize: buttonSize,
          padding: buttonPadding,
          shape: buttonShape,
          textStyle: TokType.labelLarge,
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: c.link,
          minimumSize: buttonSize,
          padding: const EdgeInsets.symmetric(horizontal: TokSpace.s3),
          shape: buttonShape,
          textStyle: TokType.labelLarge,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: c.surface,
        contentPadding: const EdgeInsets.symmetric(horizontal: TokComponent.inputPaddingX, vertical: TokSpace.s3),
        constraints: const BoxConstraints(minHeight: TokComponent.inputHeight),
        labelStyle: text.bodyLarge?.copyWith(color: c.onSurfaceMuted),
        floatingLabelStyle: text.bodyMedium?.copyWith(color: c.primary),
        hintStyle: text.bodyLarge?.copyWith(color: c.onSurfaceMuted),
        helperStyle: text.bodySmall,
        helperMaxLines: 3,
        errorMaxLines: 3,
        errorStyle: text.bodySmall?.copyWith(color: c.error),
        border: inputBorder(c.borderStrong),
        enabledBorder: inputBorder(c.borderStrong),
        focusedBorder: inputBorder(c.focus, TokComponent.inputFocusBorderWidth),
        errorBorder: inputBorder(c.error),
        focusedErrorBorder: inputBorder(c.error, TokComponent.inputFocusBorderWidth),
        disabledBorder: inputBorder(c.border),
      ),
      cardTheme: CardThemeData(
        color: c.surface,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(TokComponent.cardRadius),
          side: BorderSide(color: c.border),
        ),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: c.surface,
        selectedColor: c.primaryContainer,
        labelStyle: text.labelMedium,
        side: BorderSide(color: c.border),
        shape: const StadiumBorder(),
        padding: const EdgeInsets.symmetric(horizontal: TokSpace.s2),
      ),
      navigationBarTheme: NavigationBarThemeData(
        height: TokComponent.navigationBarHeight,
        backgroundColor: c.surface,
        surfaceTintColor: Colors.transparent,
        indicatorColor: c.primaryContainer,
        indicatorShape: const StadiumBorder(),
        labelTextStyle: WidgetStateProperty.resolveWith(
          (states) => TokType.labelMedium.copyWith(
            color: states.contains(WidgetState.selected) ? c.primary : c.onSurfaceMuted,
            fontWeight: states.contains(WidgetState.selected) ? FontWeight.w600 : FontWeight.w500,
          ),
        ),
        iconTheme: WidgetStateProperty.resolveWith(
          (states) => IconThemeData(
            size: TokIconSize.lg,
            color: states.contains(WidgetState.selected) ? c.primary : c.onSurfaceMuted,
          ),
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: c.surfaceRaised,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(TokComponent.modalRadius)),
        titleTextStyle: text.headlineSmall,
        contentTextStyle: text.bodyMedium,
      ),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: c.surfaceRaised,
        surfaceTintColor: Colors.transparent,
        showDragHandle: true,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(TokComponent.modalRadius)),
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        backgroundColor: c.surfaceInverse,
        contentTextStyle: text.bodyMedium?.copyWith(color: c.onSurfaceInverse),
        actionTextColor: brightness == Brightness.light ? TokPalette.accent300 : TokPalette.primary700,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(TokComponent.notificationRadius)),
      ),
      progressIndicatorTheme: ProgressIndicatorThemeData(color: c.secondary, linearTrackColor: c.surfaceMuted),
      iconTheme: IconThemeData(color: c.onSurface, size: TokIconSize.lg),
      listTileTheme: ListTileThemeData(
        iconColor: c.onSurfaceMuted,
        titleTextStyle: text.titleSmall,
        subtitleTextStyle: text.bodySmall,
        minVerticalPadding: TokSpace.s3,
      ),
    );
  }
}

/// Korte toegang tot de semantische kleuren: `context.colors.primary`.
extension AppThemeContext on BuildContext {
  AppColors get colors => Theme.of(this).extension<AppColors>()!;
}
