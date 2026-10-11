/// Testgegevens in het formaat van de backend-API. Niet gebruikt in de app zelf.
library;

const mosqueId = '11111111-1111-1111-1111-111111111111';
const slug = 'noeroel-islam';
const base = '/public/mosques/$slug';

Map<String, Object?> mosqueJson({String? description = 'Een moskee midden in de wijk.'}) => {
  'id': mosqueId,
  'slug': slug,
  'name': 'Noeroel Islam',
  'description': description,
  'address': {'street': 'Voorbeeldstraat 1', 'postalCode': '1234 AB', 'city': 'Voorbeeldstad', 'countryCode': 'NL'},
  'contact': {'email': 'info@example.org', 'phone': '010 123 4567', 'websiteUrl': 'https://example.org'},
  'timeZone': 'Europe/Amsterdam',
};

Map<String, Object?> eventJson({
  String id = 'e1',
  String title = 'Lezing over geduld',
  String category = 'LECTURE',
  String startsAt = '2030-11-08T18:30:00Z',
  String endsAt = '2030-11-08T20:00:00Z',
  String status = 'PUBLISHED',
  String visibility = 'PUBLIC',
  bool registrationEnabled = false,
  int? capacity,
  int? availablePlaces,
  bool? registeredByMe,
  String? location = 'Grote zaal',
}) => {
  'id': id,
  'title': title,
  'description': 'Een avond over geduld in het dagelijks leven.',
  'category': category,
  'startsAt': startsAt,
  'endsAt': endsAt,
  'location': location,
  'imageUrl': null,
  'visibility': visibility,
  'status': status,
  'registration': {
    'enabled': registrationEnabled,
    'capacity': capacity,
    'availablePlaces': availablePlaces,
    'registeredByMe': registeredByMe,
  },
};

Map<String, Object?> mediaJson({
  String id = 'm1',
  String title = 'Tafsir van soera Al-Asr',
  String category = 'LECTURE',
  String? speaker = 'Imam Voorbeeld',
  String? videoId = 'dQw4w9WgXcQ',
  Map<String, Object?>? relatedEvent,
}) => {
  'id': id,
  'title': title,
  'description': 'Uitleg van een korte soera.',
  'speaker': speaker,
  'category': category,
  'video': videoId == null
      ? null
      : {
          'provider': 'YOUTUBE',
          'videoId': videoId,
          'watchUrl': 'https://www.youtube.com/watch?v=$videoId',
          'embedUrl': 'https://www.youtube-nocookie.com/embed/$videoId',
        },
  'thumbnailUrl': null,
  'durationSeconds': 2700,
  'recordedOn': '2030-10-01',
  'relatedEvent': relatedEvent,
  'visibility': 'PUBLIC',
  'publishedAt': '2030-10-02T12:00:00Z',
};

Map<String, Object?> announcementJson({String id = 'a1', String title = 'Nieuwe lestijden', bool featured = false}) => {
  'id': id,
  'title': title,
  'body': 'Vanaf volgende week beginnen de lessen een half uur later.',
  'featured': featured,
  'publishedAt': '2030-10-03T09:00:00Z',
  'expiresAt': null,
};

Map<String, Object?> page(List<Map<String, Object?>> items, {int page = 0, int? totalPages, int? totalItems}) => {
  'items': items,
  'page': page,
  'size': 20,
  'totalItems': totalItems ?? items.length,
  'totalPages': totalPages ?? (items.isEmpty ? 0 : 1),
};

Map<String, Object?> userJson() => {
  'id': '22222222-2222-2222-2222-222222222222',
  'email': 'fatima@example.org',
  'displayName': 'Fatima',
  'platformRole': 'USER',
  'status': 'ACTIVE',
  'createdAt': '2026-01-01T00:00:00Z',
};

Map<String, Object?> tokensJson({String access = 'access-1', String refresh = 'refresh-1', String? expiresAt}) => {
  'tokenType': 'Bearer',
  'accessToken': access,
  'accessTokenExpiresAt': expiresAt ?? '2099-01-01T00:00:00Z',
  'refreshToken': refresh,
  'refreshTokenExpiresAt': '2099-02-01T00:00:00Z',
};

Map<String, Object?> followJson({bool announcements = true, bool events = true, bool media = false}) => {
  'mosqueId': mosqueId,
  'mosqueSlug': slug,
  'mosqueName': 'Noeroel Islam',
  'followedSince': '2026-09-01T10:00:00Z',
  'notifications': {'ANNOUNCEMENTS': announcements, 'EVENTS': events, 'MEDIA': media},
};
