import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:youtube_player_iframe/youtube_player_iframe.dart';

import '../../core/design/design.dart';
import '../../data/models.dart';

/// Bouwt de speler voor een video. Tests vervangen deze, omdat een WebView daar niet beschikbaar is.
typedef VideoPlayerBuilder = Widget Function(String videoId, VoidCallback onUnavailable);

final videoPlayerBuilderProvider = Provider<VideoPlayerBuilder>(
  (ref) =>
      (videoId, onUnavailable) => YoutubeVideoPlayer(videoId: videoId, onUnavailable: onUnavailable),
);

/// De video van een media-item, met een duidelijke melding en een uitweg als afspelen niet lukt.
class MediaVideo extends ConsumerStatefulWidget {
  const MediaVideo({super.key, required this.item});

  final MediaItem item;

  @override
  ConsumerState<MediaVideo> createState() => _MediaVideoState();
}

class _MediaVideoState extends ConsumerState<MediaVideo> {
  bool _unavailable = false;

  @override
  void didUpdateWidget(MediaVideo oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.item.id != widget.item.id) _unavailable = false;
  }

  @override
  Widget build(BuildContext context) {
    final videoId = widget.item.videoId;
    if (videoId == null || _unavailable) return _Unavailable(watchUrl: widget.item.watchUrl);
    return ref.watch(videoPlayerBuilderProvider)(videoId, () {
      if (mounted) setState(() => _unavailable = true);
    });
  }
}

class _Unavailable extends StatelessWidget {
  const _Unavailable({required this.watchUrl});

  final String? watchUrl;

  @override
  Widget build(BuildContext context) {
    final url = watchUrl == null ? null : Uri.tryParse(watchUrl!);
    return Padding(
      padding: const EdgeInsets.all(TokSpace.s4),
      child: StatusMessage(
        kind: StatusKind.warning,
        title: 'Video niet beschikbaar',
        message: url == null
            ? 'Deze video kan op dit moment niet worden afgespeeld.'
            : 'Deze video kan hier niet worden afgespeeld. Je kunt hem wel bij YouTube bekijken.',
        actionLabel: url == null ? null : 'Bekijken op YouTube',
        onAction: url == null ? null : () => launchUrl(url, mode: LaunchMode.externalApplication),
      ),
    );
  }
}

/// YouTube-speler in de privacyvriendelijke modus (youtube-nocookie.com), met de bediening van YouTube zelf.
class YoutubeVideoPlayer extends StatefulWidget {
  const YoutubeVideoPlayer({super.key, required this.videoId, required this.onUnavailable});

  final String videoId;
  final VoidCallback onUnavailable;

  @override
  State<YoutubeVideoPlayer> createState() => _YoutubeVideoPlayerState();
}

class _YoutubeVideoPlayerState extends State<YoutubeVideoPlayer> {
  late final YoutubePlayerController _controller = YoutubePlayerController.fromVideoId(
    videoId: widget.videoId,
    autoPlay: false,
    params: const YoutubePlayerParams(showFullscreenButton: true, strictRelatedVideos: true, privacyEnhancedMode: true),
  );
  late final StreamSubscription<YoutubePlayerValue> _subscription;
  Timer? _loadTimeout;

  /// Zonder verbinding met YouTube laadt de speler nooit en blijft het vlak leeg; na deze tijd volgt de melding.
  static const _maxLoadTime = Duration(seconds: 15);

  @override
  void initState() {
    super.initState();
    _loadTimeout = Timer(_maxLoadTime, widget.onUnavailable);
    _subscription = _controller.stream.listen((value) {
      if (value.hasError) {
        _loadTimeout?.cancel();
        widget.onUnavailable();
      } else if (value.playerState != PlayerState.unknown) {
        _loadTimeout?.cancel();
      }
    });
  }

  @override
  void dispose() {
    _loadTimeout?.cancel();
    _subscription.cancel();
    _controller.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Videospeler',
      child: YoutubePlayer(controller: _controller, aspectRatio: 16 / 9),
    );
  }
}
