import '../core/api/api_client.dart';
import '../core/api/api_exception.dart';
import 'json.dart';
import 'models.dart';

/// Publieke content van de moskee waarvoor deze app is gebouwd. Alle methoden vallen zonder verbinding terug
/// op de laatst opgehaalde gegevens ([Fetched.cachedAt]).
class MosqueRepository {
  MosqueRepository(this._api, this.slug);

  final ApiClient _api;
  final String slug;

  String get _base => '/public/mosques/${Uri.encodeComponent(slug)}';

  Future<Fetched<Mosque>> mosque() async => (await _api.getCached(_base)).map((json) => Mosque.fromJson(json as Json));

  /// Aankomende activiteiten, oplopend op begintijd.
  Future<Fetched<PageResult<Event>>> events({EventCategory? category, int page = 0, int size = 20}) async {
    final result = await _api.getCached(
      '$_base/events',
      query: {'category': category?.apiValue, 'page': page, 'size': size},
    );
    return result.map((json) => PageResult.fromJson(json as Json, Event.fromJson));
  }

  Future<Fetched<Event>> event(String id) async =>
      (await _api.getCached('$_base/events/${Uri.encodeComponent(id)}')).map((json) => Event.fromJson(json as Json));

  /// De mediabibliotheek, nieuwste eerst. [query] zoekt in titel en spreker.
  Future<Fetched<PageResult<MediaItem>>> media({
    String? query,
    MediaCategory? category,
    int page = 0,
    int size = 20,
  }) async {
    final result = await _api.getCached(
      '$_base/media',
      query: {'q': query?.trim(), 'category': category?.apiValue, 'page': page, 'size': size},
    );
    return result.map((json) => PageResult.fromJson(json as Json, MediaItem.fromJson));
  }

  Future<Fetched<MediaItem>> mediaItem(String id) async =>
      (await _api.getCached('$_base/media/${Uri.encodeComponent(id)}')).map((json) => MediaItem.fromJson(json as Json));

  Future<Fetched<List<MediaItem>>> relatedMedia(String id) async =>
      (await _api.getCached('$_base/media/${Uri.encodeComponent(id)}/related'))
          .map((json) => [for (final item in json as List<dynamic>) MediaItem.fromJson(item as Json)]);

  /// Actuele aankondigingen; uitgelicht eerst.
  Future<Fetched<PageResult<Announcement>>> announcements({int page = 0, int size = 20}) async {
    final result = await _api.getCached('$_base/announcements', query: {'page': page, 'size': size});
    return result.map((json) => PageResult.fromJson(json as Json, Announcement.fromJson));
  }

  Future<Fetched<Announcement>> announcement(String id) async =>
      (await _api.getCached('$_base/announcements/${Uri.encodeComponent(id)}'))
          .map((json) => Announcement.fromJson(json as Json));
}

/// Acties van de ingelogde gebruiker: aanmelden, volgen en meldingsvoorkeuren.
class MeRepository {
  MeRepository(this._api);

  final ApiClient _api;

  Future<void> register(String eventId) => _api.post('/events/${Uri.encodeComponent(eventId)}/registrations');

  Future<void> cancelRegistration(String eventId) =>
      _api.delete('/events/${Uri.encodeComponent(eventId)}/registrations/me');

  /// Eigen aanmeldingen voor activiteiten die nog komen of net zijn geweest, nieuwste aanmelding eerst.
  Future<List<MyRegistration>> registrations() async {
    final json = await _api.get('/me/event-registrations', query: {'size': 100}) as Json;
    return PageResult.fromJson(json, MyRegistration.fromJson).items;
  }

  /// De volgstatus voor een moskee, of null als de gebruiker haar niet volgt.
  Future<FollowStatus?> followStatus(String mosqueId) async {
    try {
      return FollowStatus.fromJson(await _api.get('/me/follows/${Uri.encodeComponent(mosqueId)}') as Json);
    } on ApiException catch (e) {
      if (e.code == 'NOT_FOLLOWING') return null;
      rethrow;
    }
  }

  Future<FollowStatus> follow(String mosqueId) async =>
      FollowStatus.fromJson(await _api.put('/me/follows/${Uri.encodeComponent(mosqueId)}') as Json);

  Future<void> unfollow(String mosqueId) => _api.delete('/me/follows/${Uri.encodeComponent(mosqueId)}');

  Future<FollowStatus> updateNotification(String mosqueId, NotificationTopic topic, bool enabled) async =>
      FollowStatus.fromJson(
        await _api.put(
          '/me/follows/${Uri.encodeComponent(mosqueId)}/notifications',
          body: {topic.requestField: enabled},
        ) as Json,
      );
}
