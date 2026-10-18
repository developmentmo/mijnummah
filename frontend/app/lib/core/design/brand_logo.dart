import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../brand/brand.g.dart';

/// Varianten van het logo van de moskee (uit `brands/<slug>/`, zie WHITE_LABEL.md). Zie BRAND_GUIDELINES.md voor gebruik.
enum BrandLogoVariant {
  /// Beeldmerk naast de woordmark; voor headers.
  horizontal('logo-horizontal'),

  /// Beeldmerk boven de woordmark; voor start- en welkomstschermen.
  stacked('logo-stacked'),

  /// Alleen het beeldmerk; voor kleine oppervlakken.
  compact('logo-compact');

  const BrandLogoVariant(this.fileStem);

  final String fileStem;
}

/// Toont het logo in de variant die past bij het actieve (lichte of donkere) thema.
class BrandLogo extends StatelessWidget {
  const BrandLogo({super.key, this.variant = BrandLogoVariant.horizontal, this.height = 40});

  final BrandLogoVariant variant;
  final double height;

  static const semanticLabel = Brand.name;

  static String assetFor(BrandLogoVariant variant, Brightness brightness) =>
      'assets/branding/${variant.fileStem}-${brightness == Brightness.dark ? 'dark' : 'light'}.svg';

  @override
  Widget build(BuildContext context) {
    return SvgPicture.asset(
      assetFor(variant, Theme.of(context).brightness),
      height: height,
      semanticsLabel: semanticLabel,
    );
  }
}
