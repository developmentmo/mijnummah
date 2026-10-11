import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

/// Opent een link buiten de app (kaart, e-mail, telefoon, website) en meldt het als dat niet lukt.
Future<void> openExternal(BuildContext context, Uri uri) async {
  final messenger = ScaffoldMessenger.of(context);
  bool opened;
  try {
    opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
  } on Exception {
    opened = false;
  }
  if (!opened) {
    messenger.showSnackBar(const SnackBar(content: Text('Deze link kan op dit toestel niet worden geopend.')));
  }
}

/// Link naar de kaartenapp van het toestel voor een adres.
Uri mapsUri(String address) => defaultTargetPlatform == TargetPlatform.iOS
    ? Uri.https('maps.apple.com', '/', {'q': address})
    : Uri(scheme: 'geo', path: '0,0', queryParameters: {'q': address});
