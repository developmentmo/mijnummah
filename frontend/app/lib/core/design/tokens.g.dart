// GEGENEREERD BESTAND, NIET HANDMATIG AANPASSEN. Bron: design/tokens/tokens.json
// ignore_for_file: public_member_api_docs

import 'package:flutter/material.dart';

/// Primitieve kleuren. Gebruik in widgets de semantische kleuren via `AppColors`.
abstract final class TokPalette {
  static const green50 = Color(0xFFEEF5F2);
  static const green100 = Color(0xFFD5E7E0);
  static const green200 = Color(0xFFADCFC2);
  static const green300 = Color(0xFF7FB2A0);
  static const green400 = Color(0xFF4F927D);

  /// Jadegroen
  static const green500 = Color(0xFF2F7965);
  static const green600 = Color(0xFF24614F);
  static const green700 = Color(0xFF1A4D3F);

  /// Donkergroen, primaire merkkleur
  static const green800 = Color(0xFF123C32);
  static const green900 = Color(0xFF0B2820);
  static const green950 = Color(0xFF061A14);
  static const gold50 = Color(0xFFFBF6EA);
  static const gold100 = Color(0xFFF4E9CC);
  static const gold200 = Color(0xFFE9D49E);
  static const gold300 = Color(0xFFDCBD77);

  /// Gedempt goud, alleen als accent
  static const gold400 = Color(0xFFC6A45A);
  static const gold500 = Color(0xFFAD8A40);
  static const gold600 = Color(0xFF8C6E31);
  static const gold700 = Color(0xFF6E5627);
  static const gold800 = Color(0xFF54421F);
  static const gold900 = Color(0xFF3B2E17);
  static const neutral0 = Color(0xFFFFFFFF);
  static const neutral25 = Color(0xFFFBF9F4);

  /// Warm ivoor
  static const neutral50 = Color(0xFFF7F3E9);
  static const neutral100 = Color(0xFFEFEAE0);
  static const neutral200 = Color(0xFFE2DCD0);
  static const neutral300 = Color(0xFFCBC5BA);
  static const neutral400 = Color(0xFFA39F96);
  static const neutral500 = Color(0xFF7C7A73);
  static const neutral600 = Color(0xFF5D5E58);
  static const neutral700 = Color(0xFF454945);
  static const neutral800 = Color(0xFF333834);

  /// Donkergrijs
  static const neutral900 = Color(0xFF242A27);
  static const neutral950 = Color(0xFF161A18);
  static const red50 = Color(0xFFFBEAE8);
  static const red100 = Color(0xFFF6D2CE);
  static const red500 = Color(0xFFD64036);
  static const red700 = Color(0xFFB3261E);
  static const red900 = Color(0xFF6E1712);
  static const amber50 = Color(0xFFFDF3DD);
  static const amber100 = Color(0xFFFAE3B0);
  static const amber500 = Color(0xFFD98E04);
  static const amber700 = Color(0xFF8F5B00);
  static const amber900 = Color(0xFF553600);
  static const leaf50 = Color(0xFFEAF4E6);
  static const leaf100 = Color(0xFFCFE6C7);
  static const leaf500 = Color(0xFF4E9A3A);
  static const leaf700 = Color(0xFF2F6B23);
  static const leaf900 = Color(0xFF1B4014);
  static const blue50 = Color(0xFFE7F0F7);
  static const blue100 = Color(0xFFC8DCEC);
  static const blue500 = Color(0xFF3C7DB3);
  static const blue700 = Color(0xFF2B5F8A);
  static const blue900 = Color(0xFF173A57);
}

/// Semantische kleuren voor het lichte thema.
abstract final class TokLight {
  static const background = Color(0xFFF7F3E9);
  static const surface = Color(0xFFFFFFFF);
  static const surfaceRaised = Color(0xFFFFFFFF);
  static const surfaceMuted = Color(0xFFEFEAE0);
  static const surfaceInverse = Color(0xFF123C32);
  static const onBackground = Color(0xFF242A27);
  static const onSurface = Color(0xFF242A27);
  static const onSurfaceMuted = Color(0xFF5D5E58);
  static const onSurfaceInverse = Color(0xFFF7F3E9);
  static const border = Color(0xFFE2DCD0);
  static const borderStrong = Color(0xFF7C7A73);
  static const divider = Color(0xFFE2DCD0);
  static const primary = Color(0xFF123C32);
  static const primaryHover = Color(0xFF1A4D3F);
  static const primaryPressed = Color(0xFF0B2820);
  static const onPrimary = Color(0xFFFFFFFF);
  static const primaryContainer = Color(0xFFEEF5F2);
  static const onPrimaryContainer = Color(0xFF123C32);
  static const secondary = Color(0xFF2F7965);
  static const secondaryHover = Color(0xFF24614F);
  static const onSecondary = Color(0xFFFFFFFF);
  static const accent = Color(0xFFC6A45A);
  static const accentContainer = Color(0xFFFBF6EA);
  static const onAccentContainer = Color(0xFF54421F);
  static const accentText = Color(0xFF6E5627);
  static const link = Color(0xFF24614F);
  static const focus = Color(0xFF2F7965);
  static const success = Color(0xFF2F6B23);
  static const successContainer = Color(0xFFEAF4E6);
  static const onSuccessContainer = Color(0xFF1B4014);
  static const warning = Color(0xFF8F5B00);
  static const warningContainer = Color(0xFFFDF3DD);
  static const onWarningContainer = Color(0xFF553600);
  static const error = Color(0xFFB3261E);
  static const errorContainer = Color(0xFFFBEAE8);
  static const onErrorContainer = Color(0xFF6E1712);
  static const info = Color(0xFF2B5F8A);
  static const infoContainer = Color(0xFFE7F0F7);
  static const onInfoContainer = Color(0xFF173A57);
  static const disabled = Color(0xFFE2DCD0);
  static const onDisabled = Color(0xFF7C7A73);
  static const skeleton = Color(0xFFE2DCD0);
  static const skeletonHighlight = Color(0xFFEFEAE0);
  static const scrim = Color(0x80161A18);
}

/// Semantische kleuren voor het donkere thema.
abstract final class TokDark {
  static const background = Color(0xFF0C1D18);
  static const surface = Color(0xFF12271F);
  static const surfaceRaised = Color(0xFF183229);
  static const surfaceMuted = Color(0xFF1D3830);
  static const surfaceInverse = Color(0xFFF7F3E9);
  static const onBackground = Color(0xFFF2EEE4);
  static const onSurface = Color(0xFFF2EEE4);
  static const onSurfaceMuted = Color(0xFFB4BEB8);
  static const onSurfaceInverse = Color(0xFF242A27);
  static const border = Color(0xFF2B463D);
  static const borderStrong = Color(0xFF6F8C81);
  static const divider = Color(0xFF223D34);
  static const primary = Color(0xFFADCFC2);
  static const primaryHover = Color(0xFFD5E7E0);
  static const primaryPressed = Color(0xFF7FB2A0);
  static const onPrimary = Color(0xFF0B2820);
  static const primaryContainer = Color(0xFF1A4D3F);
  static const onPrimaryContainer = Color(0xFFEEF5F2);
  static const secondary = Color(0xFF7FB2A0);
  static const secondaryHover = Color(0xFFADCFC2);
  static const onSecondary = Color(0xFF061A14);
  static const accent = Color(0xFFC6A45A);
  static const accentContainer = Color(0xFF3A3020);
  static const onAccentContainer = Color(0xFFF4E9CC);
  static const accentText = Color(0xFFDCBD77);
  static const link = Color(0xFFADCFC2);
  static const focus = Color(0xFFDCBD77);
  static const success = Color(0xFFCFE6C7);
  static const successContainer = Color(0xFF1E3A18);
  static const onSuccessContainer = Color(0xFFEAF4E6);
  static const warning = Color(0xFFFAE3B0);
  static const warningContainer = Color(0xFF3D2C08);
  static const onWarningContainer = Color(0xFFFDF3DD);
  static const error = Color(0xFFFFB4AB);
  static const errorContainer = Color(0xFF4A1A16);
  static const onErrorContainer = Color(0xFFFBEAE8);
  static const info = Color(0xFFC8DCEC);
  static const infoContainer = Color(0xFF16304A);
  static const onInfoContainer = Color(0xFFE7F0F7);
  static const disabled = Color(0xFF22382F);
  static const onDisabled = Color(0xFF7D8C85);
  static const skeleton = Color(0xFF1D3830);
  static const skeletonHighlight = Color(0xFF254439);
  static const scrim = Color(0xA6000000);
}

/// Semantische kleuren van het actieve thema: `Theme.of(context).extension<AppColors>()!` of `context.colors`.
@immutable
class AppColors extends ThemeExtension<AppColors> {
  const AppColors({
    required this.background,
    required this.surface,
    required this.surfaceRaised,
    required this.surfaceMuted,
    required this.surfaceInverse,
    required this.onBackground,
    required this.onSurface,
    required this.onSurfaceMuted,
    required this.onSurfaceInverse,
    required this.border,
    required this.borderStrong,
    required this.divider,
    required this.primary,
    required this.primaryHover,
    required this.primaryPressed,
    required this.onPrimary,
    required this.primaryContainer,
    required this.onPrimaryContainer,
    required this.secondary,
    required this.secondaryHover,
    required this.onSecondary,
    required this.accent,
    required this.accentContainer,
    required this.onAccentContainer,
    required this.accentText,
    required this.link,
    required this.focus,
    required this.success,
    required this.successContainer,
    required this.onSuccessContainer,
    required this.warning,
    required this.warningContainer,
    required this.onWarningContainer,
    required this.error,
    required this.errorContainer,
    required this.onErrorContainer,
    required this.info,
    required this.infoContainer,
    required this.onInfoContainer,
    required this.disabled,
    required this.onDisabled,
    required this.skeleton,
    required this.skeletonHighlight,
    required this.scrim,
  });

  final Color background;
  final Color surface;
  final Color surfaceRaised;
  final Color surfaceMuted;
  final Color surfaceInverse;
  final Color onBackground;
  final Color onSurface;
  final Color onSurfaceMuted;
  final Color onSurfaceInverse;
  final Color border;
  final Color borderStrong;
  final Color divider;
  final Color primary;
  final Color primaryHover;
  final Color primaryPressed;
  final Color onPrimary;
  final Color primaryContainer;
  final Color onPrimaryContainer;
  final Color secondary;
  final Color secondaryHover;
  final Color onSecondary;
  final Color accent;
  final Color accentContainer;
  final Color onAccentContainer;
  final Color accentText;
  final Color link;
  final Color focus;
  final Color success;
  final Color successContainer;
  final Color onSuccessContainer;
  final Color warning;
  final Color warningContainer;
  final Color onWarningContainer;
  final Color error;
  final Color errorContainer;
  final Color onErrorContainer;
  final Color info;
  final Color infoContainer;
  final Color onInfoContainer;
  final Color disabled;
  final Color onDisabled;
  final Color skeleton;
  final Color skeletonHighlight;
  final Color scrim;

  static const light = AppColors(
    background: TokLight.background,
    surface: TokLight.surface,
    surfaceRaised: TokLight.surfaceRaised,
    surfaceMuted: TokLight.surfaceMuted,
    surfaceInverse: TokLight.surfaceInverse,
    onBackground: TokLight.onBackground,
    onSurface: TokLight.onSurface,
    onSurfaceMuted: TokLight.onSurfaceMuted,
    onSurfaceInverse: TokLight.onSurfaceInverse,
    border: TokLight.border,
    borderStrong: TokLight.borderStrong,
    divider: TokLight.divider,
    primary: TokLight.primary,
    primaryHover: TokLight.primaryHover,
    primaryPressed: TokLight.primaryPressed,
    onPrimary: TokLight.onPrimary,
    primaryContainer: TokLight.primaryContainer,
    onPrimaryContainer: TokLight.onPrimaryContainer,
    secondary: TokLight.secondary,
    secondaryHover: TokLight.secondaryHover,
    onSecondary: TokLight.onSecondary,
    accent: TokLight.accent,
    accentContainer: TokLight.accentContainer,
    onAccentContainer: TokLight.onAccentContainer,
    accentText: TokLight.accentText,
    link: TokLight.link,
    focus: TokLight.focus,
    success: TokLight.success,
    successContainer: TokLight.successContainer,
    onSuccessContainer: TokLight.onSuccessContainer,
    warning: TokLight.warning,
    warningContainer: TokLight.warningContainer,
    onWarningContainer: TokLight.onWarningContainer,
    error: TokLight.error,
    errorContainer: TokLight.errorContainer,
    onErrorContainer: TokLight.onErrorContainer,
    info: TokLight.info,
    infoContainer: TokLight.infoContainer,
    onInfoContainer: TokLight.onInfoContainer,
    disabled: TokLight.disabled,
    onDisabled: TokLight.onDisabled,
    skeleton: TokLight.skeleton,
    skeletonHighlight: TokLight.skeletonHighlight,
    scrim: TokLight.scrim,
  );

  static const dark = AppColors(
    background: TokDark.background,
    surface: TokDark.surface,
    surfaceRaised: TokDark.surfaceRaised,
    surfaceMuted: TokDark.surfaceMuted,
    surfaceInverse: TokDark.surfaceInverse,
    onBackground: TokDark.onBackground,
    onSurface: TokDark.onSurface,
    onSurfaceMuted: TokDark.onSurfaceMuted,
    onSurfaceInverse: TokDark.onSurfaceInverse,
    border: TokDark.border,
    borderStrong: TokDark.borderStrong,
    divider: TokDark.divider,
    primary: TokDark.primary,
    primaryHover: TokDark.primaryHover,
    primaryPressed: TokDark.primaryPressed,
    onPrimary: TokDark.onPrimary,
    primaryContainer: TokDark.primaryContainer,
    onPrimaryContainer: TokDark.onPrimaryContainer,
    secondary: TokDark.secondary,
    secondaryHover: TokDark.secondaryHover,
    onSecondary: TokDark.onSecondary,
    accent: TokDark.accent,
    accentContainer: TokDark.accentContainer,
    onAccentContainer: TokDark.onAccentContainer,
    accentText: TokDark.accentText,
    link: TokDark.link,
    focus: TokDark.focus,
    success: TokDark.success,
    successContainer: TokDark.successContainer,
    onSuccessContainer: TokDark.onSuccessContainer,
    warning: TokDark.warning,
    warningContainer: TokDark.warningContainer,
    onWarningContainer: TokDark.onWarningContainer,
    error: TokDark.error,
    errorContainer: TokDark.errorContainer,
    onErrorContainer: TokDark.onErrorContainer,
    info: TokDark.info,
    infoContainer: TokDark.infoContainer,
    onInfoContainer: TokDark.onInfoContainer,
    disabled: TokDark.disabled,
    onDisabled: TokDark.onDisabled,
    skeleton: TokDark.skeleton,
    skeletonHighlight: TokDark.skeletonHighlight,
    scrim: TokDark.scrim,
  );

  @override
  AppColors copyWith({
    Color? background,
    Color? surface,
    Color? surfaceRaised,
    Color? surfaceMuted,
    Color? surfaceInverse,
    Color? onBackground,
    Color? onSurface,
    Color? onSurfaceMuted,
    Color? onSurfaceInverse,
    Color? border,
    Color? borderStrong,
    Color? divider,
    Color? primary,
    Color? primaryHover,
    Color? primaryPressed,
    Color? onPrimary,
    Color? primaryContainer,
    Color? onPrimaryContainer,
    Color? secondary,
    Color? secondaryHover,
    Color? onSecondary,
    Color? accent,
    Color? accentContainer,
    Color? onAccentContainer,
    Color? accentText,
    Color? link,
    Color? focus,
    Color? success,
    Color? successContainer,
    Color? onSuccessContainer,
    Color? warning,
    Color? warningContainer,
    Color? onWarningContainer,
    Color? error,
    Color? errorContainer,
    Color? onErrorContainer,
    Color? info,
    Color? infoContainer,
    Color? onInfoContainer,
    Color? disabled,
    Color? onDisabled,
    Color? skeleton,
    Color? skeletonHighlight,
    Color? scrim,
  }) {
    return AppColors(
      background: background ?? this.background,
      surface: surface ?? this.surface,
      surfaceRaised: surfaceRaised ?? this.surfaceRaised,
      surfaceMuted: surfaceMuted ?? this.surfaceMuted,
      surfaceInverse: surfaceInverse ?? this.surfaceInverse,
      onBackground: onBackground ?? this.onBackground,
      onSurface: onSurface ?? this.onSurface,
      onSurfaceMuted: onSurfaceMuted ?? this.onSurfaceMuted,
      onSurfaceInverse: onSurfaceInverse ?? this.onSurfaceInverse,
      border: border ?? this.border,
      borderStrong: borderStrong ?? this.borderStrong,
      divider: divider ?? this.divider,
      primary: primary ?? this.primary,
      primaryHover: primaryHover ?? this.primaryHover,
      primaryPressed: primaryPressed ?? this.primaryPressed,
      onPrimary: onPrimary ?? this.onPrimary,
      primaryContainer: primaryContainer ?? this.primaryContainer,
      onPrimaryContainer: onPrimaryContainer ?? this.onPrimaryContainer,
      secondary: secondary ?? this.secondary,
      secondaryHover: secondaryHover ?? this.secondaryHover,
      onSecondary: onSecondary ?? this.onSecondary,
      accent: accent ?? this.accent,
      accentContainer: accentContainer ?? this.accentContainer,
      onAccentContainer: onAccentContainer ?? this.onAccentContainer,
      accentText: accentText ?? this.accentText,
      link: link ?? this.link,
      focus: focus ?? this.focus,
      success: success ?? this.success,
      successContainer: successContainer ?? this.successContainer,
      onSuccessContainer: onSuccessContainer ?? this.onSuccessContainer,
      warning: warning ?? this.warning,
      warningContainer: warningContainer ?? this.warningContainer,
      onWarningContainer: onWarningContainer ?? this.onWarningContainer,
      error: error ?? this.error,
      errorContainer: errorContainer ?? this.errorContainer,
      onErrorContainer: onErrorContainer ?? this.onErrorContainer,
      info: info ?? this.info,
      infoContainer: infoContainer ?? this.infoContainer,
      onInfoContainer: onInfoContainer ?? this.onInfoContainer,
      disabled: disabled ?? this.disabled,
      onDisabled: onDisabled ?? this.onDisabled,
      skeleton: skeleton ?? this.skeleton,
      skeletonHighlight: skeletonHighlight ?? this.skeletonHighlight,
      scrim: scrim ?? this.scrim,
    );
  }

  @override
  AppColors lerp(AppColors? other, double t) {
    if (other == null) return this;
    return AppColors(
      background: Color.lerp(background, other.background, t)!,
      surface: Color.lerp(surface, other.surface, t)!,
      surfaceRaised: Color.lerp(surfaceRaised, other.surfaceRaised, t)!,
      surfaceMuted: Color.lerp(surfaceMuted, other.surfaceMuted, t)!,
      surfaceInverse: Color.lerp(surfaceInverse, other.surfaceInverse, t)!,
      onBackground: Color.lerp(onBackground, other.onBackground, t)!,
      onSurface: Color.lerp(onSurface, other.onSurface, t)!,
      onSurfaceMuted: Color.lerp(onSurfaceMuted, other.onSurfaceMuted, t)!,
      onSurfaceInverse: Color.lerp(onSurfaceInverse, other.onSurfaceInverse, t)!,
      border: Color.lerp(border, other.border, t)!,
      borderStrong: Color.lerp(borderStrong, other.borderStrong, t)!,
      divider: Color.lerp(divider, other.divider, t)!,
      primary: Color.lerp(primary, other.primary, t)!,
      primaryHover: Color.lerp(primaryHover, other.primaryHover, t)!,
      primaryPressed: Color.lerp(primaryPressed, other.primaryPressed, t)!,
      onPrimary: Color.lerp(onPrimary, other.onPrimary, t)!,
      primaryContainer: Color.lerp(primaryContainer, other.primaryContainer, t)!,
      onPrimaryContainer: Color.lerp(onPrimaryContainer, other.onPrimaryContainer, t)!,
      secondary: Color.lerp(secondary, other.secondary, t)!,
      secondaryHover: Color.lerp(secondaryHover, other.secondaryHover, t)!,
      onSecondary: Color.lerp(onSecondary, other.onSecondary, t)!,
      accent: Color.lerp(accent, other.accent, t)!,
      accentContainer: Color.lerp(accentContainer, other.accentContainer, t)!,
      onAccentContainer: Color.lerp(onAccentContainer, other.onAccentContainer, t)!,
      accentText: Color.lerp(accentText, other.accentText, t)!,
      link: Color.lerp(link, other.link, t)!,
      focus: Color.lerp(focus, other.focus, t)!,
      success: Color.lerp(success, other.success, t)!,
      successContainer: Color.lerp(successContainer, other.successContainer, t)!,
      onSuccessContainer: Color.lerp(onSuccessContainer, other.onSuccessContainer, t)!,
      warning: Color.lerp(warning, other.warning, t)!,
      warningContainer: Color.lerp(warningContainer, other.warningContainer, t)!,
      onWarningContainer: Color.lerp(onWarningContainer, other.onWarningContainer, t)!,
      error: Color.lerp(error, other.error, t)!,
      errorContainer: Color.lerp(errorContainer, other.errorContainer, t)!,
      onErrorContainer: Color.lerp(onErrorContainer, other.onErrorContainer, t)!,
      info: Color.lerp(info, other.info, t)!,
      infoContainer: Color.lerp(infoContainer, other.infoContainer, t)!,
      onInfoContainer: Color.lerp(onInfoContainer, other.onInfoContainer, t)!,
      disabled: Color.lerp(disabled, other.disabled, t)!,
      onDisabled: Color.lerp(onDisabled, other.onDisabled, t)!,
      skeleton: Color.lerp(skeleton, other.skeleton, t)!,
      skeletonHighlight: Color.lerp(skeletonHighlight, other.skeletonHighlight, t)!,
      scrim: Color.lerp(scrim, other.scrim, t)!,
    );
  }
}

/// Lettertypefamilies zoals geregistreerd in pubspec.yaml.
abstract final class TokFont {
  static const display = 'Fraunces';
  static const body = 'Inter';
  static const arabic = 'Noto Naskh Arabic';
}

/// Tekststijlen zonder kleur; de kleur komt uit het thema.
abstract final class TokType {
  static const displayLarge = TextStyle(
    fontFamily: 'Fraunces',
    fontWeight: FontWeight.w600,
    fontSize: 40.0,
    height: 1.2,
    letterSpacing: -0.5,
  );
  static const displayMedium = TextStyle(
    fontFamily: 'Fraunces',
    fontWeight: FontWeight.w600,
    fontSize: 32.0,
    height: 1.25,
    letterSpacing: -0.25,
  );
  static const headlineLarge = TextStyle(
    fontFamily: 'Fraunces',
    fontWeight: FontWeight.w600,
    fontSize: 28.0,
    height: 1.2857,
    letterSpacing: -0.2,
  );
  static const headlineMedium = TextStyle(
    fontFamily: 'Fraunces',
    fontWeight: FontWeight.w600,
    fontSize: 24.0,
    height: 1.3333,
    letterSpacing: 0.0,
  );
  static const headlineSmall = TextStyle(
    fontFamily: 'Fraunces',
    fontWeight: FontWeight.w600,
    fontSize: 20.0,
    height: 1.4,
    letterSpacing: 0.0,
  );
  static const titleLarge = TextStyle(
    fontFamily: 'Inter',
    fontWeight: FontWeight.w600,
    fontSize: 20.0,
    height: 1.4,
    letterSpacing: 0.0,
  );
  static const titleMedium = TextStyle(
    fontFamily: 'Inter',
    fontWeight: FontWeight.w600,
    fontSize: 17.0,
    height: 1.4118,
    letterSpacing: 0.0,
  );
  static const titleSmall = TextStyle(
    fontFamily: 'Inter',
    fontWeight: FontWeight.w600,
    fontSize: 15.0,
    height: 1.4667,
    letterSpacing: 0.0,
  );
  static const bodyLarge = TextStyle(
    fontFamily: 'Inter',
    fontWeight: FontWeight.w400,
    fontSize: 17.0,
    height: 1.5294,
    letterSpacing: 0.0,
  );
  static const bodyMedium = TextStyle(
    fontFamily: 'Inter',
    fontWeight: FontWeight.w400,
    fontSize: 15.0,
    height: 1.4667,
    letterSpacing: 0.0,
  );
  static const bodySmall = TextStyle(
    fontFamily: 'Inter',
    fontWeight: FontWeight.w400,
    fontSize: 13.0,
    height: 1.3846,
    letterSpacing: 0.0,
  );
  static const labelLarge = TextStyle(
    fontFamily: 'Inter',
    fontWeight: FontWeight.w600,
    fontSize: 15.0,
    height: 1.3333,
    letterSpacing: 0.1,
  );
  static const labelMedium = TextStyle(
    fontFamily: 'Inter',
    fontWeight: FontWeight.w500,
    fontSize: 13.0,
    height: 1.3846,
    letterSpacing: 0.2,
  );
  static const labelSmall = TextStyle(
    fontFamily: 'Inter',
    fontWeight: FontWeight.w500,
    fontSize: 12.0,
    height: 1.3333,
    letterSpacing: 0.3,
  );
  static const arabic = TextStyle(
    fontFamily: 'Noto Naskh Arabic',
    fontWeight: FontWeight.w400,
    fontSize: 20.0,
    height: 1.7,
    letterSpacing: 0.0,
  );
}

/// Afstanden (logische pixels). Stap 1 = 4.
abstract final class TokSpace {
  static const s0 = 0.0;
  static const s1 = 4.0;
  static const s2 = 8.0;
  static const s3 = 12.0;
  static const s4 = 16.0;
  static const s5 = 20.0;
  static const s6 = 24.0;
  static const s8 = 32.0;
  static const s10 = 40.0;
  static const s12 = 48.0;
  static const s16 = 64.0;
}

/// Afrondingen van hoeken.
abstract final class TokRadius {
  static const xs = 4.0;
  static const sm = 8.0;
  static const md = 12.0;
  static const lg = 16.0;
  static const xl = 24.0;
  static const full = 999.0;
}

/// Standaardmaten voor iconen.
abstract final class TokIconSize {
  static const sm = 16.0;
  static const md = 20.0;
  static const lg = 24.0;
  static const xl = 32.0;
}

/// Overige maten.
abstract final class TokSize {
  static const touchTarget = 48.0;
  static const contentMaxWidth = 1200.0;
}

/// Schaduwen.
abstract final class TokShadow {
  static const sm = <BoxShadow>[
    BoxShadow(color: Color(0x14123C32), offset: Offset(0.0, 1.0), blurRadius: 2.0, spreadRadius: 0.0),
  ];
  static const md = <BoxShadow>[
    BoxShadow(color: Color(0x14123C32), offset: Offset(0.0, 2.0), blurRadius: 8.0, spreadRadius: 0.0),
    BoxShadow(color: Color(0x0A123C32), offset: Offset(0.0, 1.0), blurRadius: 2.0, spreadRadius: 0.0),
  ];
  static const lg = <BoxShadow>[
    BoxShadow(color: Color(0x1F123C32), offset: Offset(0.0, 12.0), blurRadius: 32.0, spreadRadius: -4.0),
  ];
}

/// Animatieduur en easing.
abstract final class TokMotion {
  static const fast = Duration(milliseconds: 120);
  static const normal = Duration(milliseconds: 200);
  static const slow = Duration(milliseconds: 320);
  static const standard = Cubic(0.2, 0.0, 0.0, 1.0);
  static const emphasized = Cubic(0.3, 0.0, 0.0, 1.0);
}

/// Componentmaten.
abstract final class TokComponent {
  static const buttonHeightSm = 36.0;
  static const buttonHeightMd = 44.0;
  static const buttonHeightLg = 52.0;
  static const buttonRadius = 12.0;
  static const buttonPaddingX = 20.0;
  static const inputHeight = 48.0;
  static const inputRadius = 12.0;
  static const inputBorderWidth = 1.0;
  static const inputFocusBorderWidth = 2.0;
  static const inputPaddingX = 16.0;
  static const cardRadius = 16.0;
  static const cardPadding = 16.0;
  static const navigationBarHeight = 72.0;
  static const navigationIndicatorRadius = 999.0;
  static const navigationSidebarWidth = 264.0;
  static const modalRadius = 24.0;
  static const modalPadding = 24.0;
  static const modalMaxWidth = 560.0;
  static const notificationRadius = 12.0;
  static const notificationPadding = 16.0;
  static const focusRingWidth = 2.0;
  static const focusRingOffset = 2.0;
}
