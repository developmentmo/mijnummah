import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/data/models.dart';

import '../helpers/fixtures.dart';

void main() {
  test('leest een activiteit met aanmeldgegevens', () {
    final event = Event.fromJson(
      eventJson(registrationEnabled: true, capacity: 40, availablePlaces: 0, registeredByMe: false),
    );
    expect(event.category, EventCategory.lecture);
    expect(event.startsAt, DateTime.utc(2030, 11, 8, 18, 30));
    expect(event.registration.isFull, isTrue);
    expect(event.registration.registeredByMe, isFalse);
    expect(event.cancelled, isFalse);
  });

  test('onbekende categorieën worden Overig', () {
    expect(Event.fromJson(eventJson(category: 'NIEUW')).category, EventCategory.other);
    expect(MediaItem.fromJson(mediaJson(category: 'NIEUW')).category, MediaCategory.other);
  });

  test('leest een media-item zonder video', () {
    final item = MediaItem.fromJson(mediaJson(videoId: null, speaker: null));
    expect(item.videoId, isNull);
    expect(item.watchUrl, isNull);
    expect(item.speaker, isNull);
    expect(item.duration, const Duration(minutes: 45));
    expect(item.recordedOn, DateTime(2030, 10, 1));
  });

  test('stelt het adres samen uit de beschikbare delen', () {
    expect(Mosque.fromJson(mosqueJson()).addressLine, 'Voorbeeldstraat 1, 1234 AB Voorbeeldstad');
    expect(Mosque.fromJson({'id': 'x', 'slug': 's', 'name': 'N', 'address': {}, 'contact': {}}).addressLine, isNull);
  });

  test('leest volgstatus met voorkeuren per onderwerp', () {
    final status = FollowStatus.fromJson(followJson(media: true, events: false));
    expect(status.notifications, {
      NotificationTopic.announcements: true,
      NotificationTopic.events: false,
      NotificationTopic.media: true,
    });
  });
}
