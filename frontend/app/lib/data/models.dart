import 'json.dart';

export 'json.dart' show PageResult;

/// De moskee waar een activiteit, video of aankondiging bij hoort: genoeg om haar te herkennen en te openen.
class MosqueSummary {
  const MosqueSummary({required this.id, required this.slug, required this.name, this.logoUrl});

  factory MosqueSummary.fromJson(Json json) => MosqueSummary(
    id: json.str('id'),
    slug: json.str('slug'),
    name: json.str('name'),
    logoUrl: json.strOrNull('logoUrl'),
  );

  static MosqueSummary? fromJsonOrNull(Json? json) => json == null ? null : MosqueSummary.fromJson(json);

  final String id;
  final String slug;
  final String name;

  /// Pad vanaf de API of volledige link; null zonder logo.
  final String? logoUrl;
}

/// Publiek profiel van een moskee. Logo en omslagfoto geven elke moskee haar eigen gezicht in de app.
class Mosque {
  const Mosque({
    required this.id,
    required this.slug,
    required this.name,
    this.description,
    this.street,
    this.postalCode,
    this.city,
    this.email,
    this.phone,
    this.websiteUrl,
    this.prayerTimesUrl,
    this.logoUrl,
    this.coverUrl,
  });

  factory Mosque.fromJson(Json json) {
    final address = json.objOrNull('address') ?? const {};
    final contact = json.objOrNull('contact') ?? const {};
    return Mosque(
      id: json.str('id'),
      slug: json.str('slug'),
      name: json.str('name'),
      description: json.strOrNull('description'),
      street: address.strOrNull('street'),
      postalCode: address.strOrNull('postalCode'),
      city: address.strOrNull('city'),
      email: contact.strOrNull('email'),
      phone: contact.strOrNull('phone'),
      websiteUrl: contact.strOrNull('websiteUrl'),
      prayerTimesUrl: json.strOrNull('prayerTimesUrl'),
      logoUrl: json.strOrNull('logoUrl'),
      coverUrl: json.strOrNull('coverUrl'),
    );
  }

  final String id;
  final String slug;
  final String name;
  final String? description;
  final String? street;
  final String? postalCode;
  final String? city;
  final String? email;
  final String? phone;
  final String? websiteUrl;

  /// Link naar de gebedstijden van de moskee (bijvoorbeeld een pdf of webpagina).
  final String? prayerTimesUrl;
  final String? logoUrl;
  final String? coverUrl;

  MosqueSummary get summary => MosqueSummary(id: id, slug: slug, name: name, logoUrl: logoUrl);

  /// Het adres op één regel, of null als het onbekend is.
  String? get addressLine {
    final cityLine = [postalCode, city].whereType<String>().join(' ');
    final parts = [street, if (cityLine.isNotEmpty) cityLine].whereType<String>();
    return parts.isEmpty ? null : parts.join(', ');
  }
}

/// Categorieën van activiteiten, zoals de backend ze kent.
enum EventCategory {
  lecture('LECTURE', 'Lezing'),
  fridayProgram('FRIDAY_PROGRAM', 'Vrijdagprogramma'),
  classes('CLASS', 'Les'),
  youth('YOUTH', 'Jongeren'),
  recordedEvent('RECORDED_EVENT', 'Opname'),
  community('COMMUNITY', 'Gemeenschap'),
  other('OTHER', 'Overig');

  const EventCategory(this.apiValue, this.label);

  final String apiValue;
  final String label;

  static EventCategory parse(String? value) =>
      values.firstWhere((c) => c.apiValue == value, orElse: () => EventCategory.other);
}

/// Wie content mag zien.
enum ContentVisibility {
  public,
  registeredUsers;

  static ContentVisibility parse(String? value) => value == 'REGISTERED_USERS' ? registeredUsers : public;
}

class Event {
  const Event({
    required this.id,
    required this.title,
    required this.category,
    required this.startsAt,
    required this.endsAt,
    required this.visibility,
    required this.cancelled,
    required this.registration,
    this.mosque,
    this.description,
    this.location,
    this.imageUrl,
  });

  factory Event.fromJson(Json json) => Event(
    id: json.str('id'),
    mosque: MosqueSummary.fromJsonOrNull(json.objOrNull('mosque')),
    title: json.str('title'),
    description: json.strOrNull('description'),
    category: EventCategory.parse(json['category'] as String?),
    startsAt: json.time('startsAt'),
    endsAt: json.time('endsAt'),
    location: json.strOrNull('location'),
    imageUrl: json.strOrNull('imageUrl'),
    visibility: ContentVisibility.parse(json['visibility'] as String?),
    cancelled: json['status'] == 'CANCELLED',
    registration: EventRegistration.fromJson(json.objOrNull('registration') ?? const {}),
  );

  final String id;

  /// Waar de activiteit is; null alleen bij oudere gegevens uit de cache.
  final MosqueSummary? mosque;
  final String title;
  final String? description;
  final EventCategory category;
  final DateTime startsAt;
  final DateTime endsAt;
  final String? location;
  final String? imageUrl;
  final ContentVisibility visibility;
  final bool cancelled;
  final EventRegistration registration;

  bool hasStarted(DateTime now) => !now.isBefore(startsAt);

  Event copyWith({EventRegistration? registration}) => Event(
    id: id,
    mosque: mosque,
    title: title,
    description: description,
    category: category,
    startsAt: startsAt,
    endsAt: endsAt,
    location: location,
    imageUrl: imageUrl,
    visibility: visibility,
    cancelled: cancelled,
    registration: registration ?? this.registration,
  );
}

class EventRegistration {
  const EventRegistration({required this.enabled, this.capacity, this.availablePlaces, this.registeredByMe});

  factory EventRegistration.fromJson(Json json) => EventRegistration(
    enabled: json.boolOr('enabled', false),
    capacity: json.intOrNull('capacity'),
    availablePlaces: json.intOrNull('availablePlaces'),
    registeredByMe: json['registeredByMe'] as bool?,
  );

  final bool enabled;

  /// Maximaal aantal deelnemers; null zonder limiet.
  final int? capacity;

  /// Resterende plaatsen; null zonder limiet.
  final int? availablePlaces;

  /// Of de ingelogde gebruiker is aangemeld; null voor anonieme bezoekers.
  final bool? registeredByMe;

  bool get isFull => availablePlaces != null && availablePlaces! <= 0;
}

/// Korte verwijzing naar een activiteit (bij een media-item).
class EventSummary {
  const EventSummary({required this.id, required this.title, required this.startsAt});

  factory EventSummary.fromJson(Json json) =>
      EventSummary(id: json.str('id'), title: json.str('title'), startsAt: json.time('startsAt'));

  final String id;
  final String title;
  final DateTime startsAt;
}

enum MediaCategory {
  lecture('LECTURE', 'Lezing'),
  khutbah('KHUTBAH', 'Khutbah'),
  quran('QURAN', 'Koran'),
  course('COURSE', 'Cursus'),
  youth('YOUTH', 'Jongeren'),
  recordedEvent('RECORDED_EVENT', 'Opname'),
  other('OTHER', 'Overig');

  const MediaCategory(this.apiValue, this.label);

  final String apiValue;
  final String label;

  static MediaCategory parse(String? value) =>
      values.firstWhere((c) => c.apiValue == value, orElse: () => MediaCategory.other);
}

/// Een opgenomen lezing of activiteit. De video zelf staat bij de videoprovider (YouTube).
class MediaItem {
  const MediaItem({
    required this.id,
    required this.title,
    required this.category,
    required this.publishedAt,
    this.mosque,
    this.description,
    this.speaker,
    this.videoId,
    this.watchUrl,
    this.thumbnailUrl,
    this.duration,
    this.recordedOn,
    this.relatedEvent,
  });

  factory MediaItem.fromJson(Json json) {
    final video = json.objOrNull('video') ?? const {};
    final related = json.objOrNull('relatedEvent');
    final seconds = json.intOrNull('durationSeconds');
    final recorded = json.strOrNull('recordedOn');
    return MediaItem(
      id: json.str('id'),
      mosque: MosqueSummary.fromJsonOrNull(json.objOrNull('mosque')),
      title: json.str('title'),
      description: json.strOrNull('description'),
      speaker: json.strOrNull('speaker'),
      category: MediaCategory.parse(json['category'] as String?),
      videoId: video.strOrNull('videoId'),
      watchUrl: video.strOrNull('watchUrl'),
      thumbnailUrl: json.strOrNull('thumbnailUrl'),
      duration: seconds == null ? null : Duration(seconds: seconds),
      recordedOn: recorded == null ? null : DateTime.parse(recorded),
      relatedEvent: related == null ? null : EventSummary.fromJson(related),
      publishedAt: json.time('publishedAt'),
    );
  }

  final String id;
  final MosqueSummary? mosque;
  final String title;
  final String? description;
  final String? speaker;
  final MediaCategory category;

  /// YouTube-video-ID; de app speelt de video af in de privacyvriendelijke modus.
  final String? videoId;

  /// Link om de video bij de provider te openen, als het afspelen in de app niet lukt.
  final String? watchUrl;
  final String? thumbnailUrl;
  final Duration? duration;

  /// Opnamedatum (kalenderdatum, zonder tijd).
  final DateTime? recordedOn;
  final EventSummary? relatedEvent;
  final DateTime publishedAt;
}

class Announcement {
  const Announcement({
    required this.id,
    required this.title,
    required this.body,
    required this.featured,
    required this.publishedAt,
    this.mosque,
    this.imageUrl,
    this.expiresAt,
  });

  factory Announcement.fromJson(Json json) => Announcement(
    id: json.str('id'),
    mosque: MosqueSummary.fromJsonOrNull(json.objOrNull('mosque')),
    imageUrl: json.strOrNull('imageUrl'),
    title: json.str('title'),
    body: json.str('body'),
    featured: json.boolOr('featured', false),
    publishedAt: json.time('publishedAt'),
    expiresAt: json.timeOrNull('expiresAt'),
  );

  final String id;
  final MosqueSummary? mosque;
  final String? imageUrl;
  final String title;
  final String body;
  final bool featured;
  final DateTime publishedAt;
  final DateTime? expiresAt;
}

/// Het eigen account.
class UserProfile {
  const UserProfile({required this.id, required this.email, required this.displayName});

  factory UserProfile.fromJson(Json json) =>
      UserProfile(id: json.str('id'), email: json.str('email'), displayName: json.str('displayName'));

  final String id;
  final String email;
  final String displayName;

  Json toJson() => {'id': id, 'email': email, 'displayName': displayName};
}

/// Een eigen aanmelding voor een activiteit.
class MyRegistration {
  const MyRegistration({
    required this.id,
    required this.eventId,
    required this.eventTitle,
    required this.eventStartsAt,
    required this.mosqueSlug,
  });

  factory MyRegistration.fromJson(Json json) => MyRegistration(
    id: json.str('id'),
    eventId: json.str('eventId'),
    eventTitle: json.str('eventTitle'),
    eventStartsAt: json.time('eventStartsAt'),
    mosqueSlug: json.str('mosqueSlug'),
  );

  final String id;
  final String eventId;
  final String eventTitle;
  final DateTime eventStartsAt;
  final String mosqueSlug;
}

/// Onderwerpen waarvoor een volger meldingen kan willen ontvangen.
enum NotificationTopic {
  announcements('ANNOUNCEMENTS', 'announcements', 'Aankondigingen'),
  events('EVENTS', 'events', 'Activiteiten'),
  media('MEDIA', 'media', 'Nieuwe lezingen');

  const NotificationTopic(this.apiValue, this.requestField, this.label);

  final String apiValue;
  final String requestField;
  final String label;
}

/// Dat de gebruiker de moskee volgt, met de meldingsvoorkeuren. Volgen is geen lidmaatschap.
class FollowStatus {
  const FollowStatus({
    required this.mosqueId,
    required this.mosqueSlug,
    required this.mosqueName,
    required this.followedSince,
    required this.notifications,
    this.mosqueLogoUrl,
    this.favorite = false,
  });

  factory FollowStatus.fromJson(Json json) {
    final raw = json.objOrNull('notifications') ?? const {};
    return FollowStatus(
      mosqueId: json.str('mosqueId'),
      mosqueSlug: json.str('mosqueSlug'),
      mosqueName: json.str('mosqueName'),
      mosqueLogoUrl: json.strOrNull('mosqueLogoUrl'),
      followedSince: json.time('followedSince'),
      favorite: json.boolOr('favorite', false),
      notifications: {for (final topic in NotificationTopic.values) topic: raw[topic.apiValue] as bool? ?? false},
    );
  }

  final String mosqueId;
  final String mosqueSlug;
  final String mosqueName;
  final String? mosqueLogoUrl;
  final DateTime followedSince;

  /// Een favoriete moskee staat bovenaan.
  final bool favorite;
  final Map<NotificationTopic, bool> notifications;

  MosqueSummary get mosque => MosqueSummary(id: mosqueId, slug: mosqueSlug, name: mosqueName, logoUrl: mosqueLogoUrl);
}

/// Een melding van een gevolgde moskee (scherm Meldingen), ook als de pushmelding dit toestel niet bereikte.
class InboxItem {
  const InboxItem({
    required this.id,
    required this.type,
    required this.title,
    required this.body,
    required this.sentAt,
    this.mosque,
    this.contentId,
  });

  factory InboxItem.fromJson(Json json) => InboxItem(
    id: json.str('id'),
    mosque: MosqueSummary.fromJsonOrNull(json.objOrNull('mosque')),
    type: json.str('type'),
    contentId: json.strOrNull('contentId'),
    title: json.str('title'),
    body: json.str('body'),
    sentAt: json.time('sentAt'),
  );

  final String id;
  final MosqueSummary? mosque;

  /// `announcement`, `event`, `media` of `message` (een eigen bericht van de moskee).
  final String type;
  final String? contentId;
  final String title;
  final String body;
  final DateTime sentAt;

  /// Het scherm dat bij deze melding hoort; een eigen bericht opent de moskee.
  String? get location {
    final id = contentId;
    if (id != null) {
      final encoded = Uri.encodeComponent(id);
      switch (type) {
        case 'announcement':
          return '/aankondiging/$encoded';
        case 'event':
          return '/activiteit/$encoded';
        case 'media':
          return '/lezing/$encoded';
      }
    }
    return mosque == null ? null : '/moskee/${Uri.encodeComponent(mosque!.slug)}';
  }
}
