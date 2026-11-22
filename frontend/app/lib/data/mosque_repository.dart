import '../core/api/api_client.dart';
import '../core/api/api_exception.dart';
import 'json.dart';
import 'models.dart';

/// Publieke content van alle aangesloten moskeeën. Lijsten nemen optioneel [mosques] (slugs): alleen die moskeeën,
/// bijvoorbeeld de gevolgde. Alle methoden vallen zonder verbinding terug op de laatst opgehaalde gegevens
/// ([Fetched.cachedAt]).
class ContentRepository {
  ContentRepository(this._api);

  final ApiClient _api;

  static const _public = '/public';

  /// Actieve moskeeën, alfabetisch. [query] zoekt in de naam, [city] is de plaats.
  Future<Fetched<PageResult<Mosque>>> mosques({String? query, String? city, int page = 0, int size = 20}) async {
    final result = await _api.getCached(
      '$_public/mosques',
      query: {'q': query?.trim(), 'city': city, 'page': page, 'size': size},
    );
    return result.map((json) => PageResult.fromJson(json as Json, Mosque.fromJson));
  }

  /// Plaatsen met een aangesloten moskee, voor het filter op plaats.
  Future<Fetched<List<String>>> cities() async =>
      (await _api.getCached('$_public/mosques/cities')).map((json) => [for (final c in json as List<dynamic>) '$c']);

  Future<Fetched<Mosque>> mosque(String slug) async =>
      (await _api.getCached('$_public/mosques/${Uri.encodeComponent(slug)}'))
          .map((json) => Mosque.fromJson(json as Json));

  /// Aankomende activiteiten, oplopend op begintijd. [from] en [to] beperken de periode.
  Future<Fetched<PageResult<Event>>> events({
    List<String>? mosques,
    EventCategory? category,
    DateTime? from,
    DateTime? to,
    int page = 0,
    int size = 20,
  }) async {
    final result = await _api.getCached(
      '$_public/events',
      query: {
        'mosque': mosques,
        'category': category?.apiValue,
        'from': from?.toUtc().toIso8601String(),
        'to': to?.toUtc().toIso8601String(),
        'page': page,
        'size': size,
      },
    );
    return result.map((json) => PageResult.fromJson(json as Json, Event.fromJson));
  }

  Future<Fetched<Event>> event(String id) async =>
      (await _api.getCached('$_public/events/${Uri.encodeComponent(id)}')).map((json) => Event.fromJson(json as Json));

  /// Lezingen en video's, nieuwste eerst. [query] zoekt in titel en spreker.
  Future<Fetched<PageResult<MediaItem>>> media({
    List<String>? mosques,
    String? query,
    MediaCategory? category,
    int page = 0,
    int size = 20,
  }) async {
    final result = await _api.getCached(
      '$_public/media',
      query: {'mosque': mosques, 'q': query?.trim(), 'category': category?.apiValue, 'page': page, 'size': size},
    );
    return result.map((json) => PageResult.fromJson(json as Json, MediaItem.fromJson));
  }

  Future<Fetched<MediaItem>> mediaItem(String id) async =>
      (await _api.getCached('$_public/media/${Uri.encodeComponent(id)}'))
          .map((json) => MediaItem.fromJson(json as Json));

  Future<Fetched<List<MediaItem>>> relatedMedia(String id) async =>
      (await _api.getCached('$_public/media/${Uri.encodeComponent(id)}/related'))
          .map((json) => [for (final item in json as List<dynamic>) MediaItem.fromJson(item as Json)]);

  /// Actuele aankondigingen, nieuwste eerst.
  Future<Fetched<PageResult<Announcement>>> announcements({List<String>? mosques, int page = 0, int size = 20}) async {
    final result = await _api.getCached(
      '$_public/announcements',
      query: {'mosque': mosques, 'page': page, 'size': size},
    );
    return result.map((json) => PageResult.fromJson(json as Json, Announcement.fromJson));
  }

  Future<Fetched<Announcement>> announcement(String id) async =>
      (await _api.getCached('$_public/announcements/${Uri.encodeComponent(id)}'))
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

  /// Gevolgde moskeeën met meldingsvoorkeuren; favorieten eerst.
  Future<List<FollowStatus>> follows() async => [
    for (final item in await _api.get('/me/follows') as List<dynamic>) FollowStatus.fromJson(item as Json),
  ];

  /// Markeert een moskee als favoriet (volgt haar zo nodig) of haalt de markering weg (blijft gevolgd).
  Future<FollowStatus> setFavorite(String mosqueId, bool favorite) async {
    final path = '/me/follows/${Uri.encodeComponent(mosqueId)}/favorite';
    return FollowStatus.fromJson(await (favorite ? _api.put(path) : _api.delete(path)) as Json);
  }

  /// IDs van de favoriete video's.
  Future<Set<String>> favoriteMediaIds() async => {
    for (final id in await _api.get('/me/favorite-media/ids') as List<dynamic>) '$id',
  };

  /// Favoriete video's die nu te bekijken zijn, laatst opgeslagen eerst.
  Future<PageResult<MediaItem>> favoriteMedia({int page = 0, int size = 20}) async => PageResult.fromJson(
    await _api.get('/me/favorite-media', query: {'page': page, 'size': size}) as Json,
    MediaItem.fromJson,
  );

  Future<void> setFavoriteMedia(String mediaId, bool favorite) {
    final path = '/me/favorite-media/${Uri.encodeComponent(mediaId)}';
    return favorite ? _api.put(path) : _api.delete(path);
  }

  /// Meldingen van gevolgde moskeeën van de laatste 30 dagen, nieuwste eerst.
  Future<PageResult<InboxItem>> notifications({int page = 0, int size = 20}) async => PageResult.fromJson(
    await _api.get('/me/notifications', query: {'page': page, 'size': size}) as Json,
    InboxItem.fromJson,
  );

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
