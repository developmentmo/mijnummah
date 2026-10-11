import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/providers.dart';
import '../../data/models.dart';

/// Actuele aankondigingen voor de startpagina; uitgelichte eerst.
final homeAnnouncementsProvider = FutureProvider.autoDispose<Fetched<PageResult<Announcement>>>(
  (ref) => ref.watch(mosqueRepositoryProvider).announcements(size: 3),
);

final announcementProvider = FutureProvider.autoDispose.family<Fetched<Announcement>, String>(
  (ref, id) => ref.watch(mosqueRepositoryProvider).announcement(id),
);
