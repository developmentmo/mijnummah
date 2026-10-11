import 'json.dart';

export 'json.dart' show PageResult;

/// Publiek profiel van de moskee.
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
    this.description,
    this.location,
    this.imageUrl,
  });

  factory Event.fromJson(Json json) => Event(
    id: json.str('id'),
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
    this.expiresAt,
  });

  factory Announcement.fromJson(Json json) => Announcement(
    id: json.str('id'),
    title: json.str('title'),
    body: json.str('body'),
    featured: json.boolOr('featured', false),
    publishedAt: json.time('publishedAt'),
    expiresAt: json.timeOrNull('expiresAt'),
  );

  final String id;
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
  const FollowStatus({required this.mosqueId, required this.followedSince, required this.notifications});

  factory FollowStatus.fromJson(Json json) {
    final raw = json.objOrNull('notifications') ?? const {};
    return FollowStatus(
      mosqueId: json.str('mosqueId'),
      followedSince: json.time('followedSince'),
      notifications: {for (final topic in NotificationTopic.values) topic: raw[topic.apiValue] as bool? ?? false},
    );
  }

  final String mosqueId;
  final DateTime followedSince;
  final Map<NotificationTopic, bool> notifications;
}
