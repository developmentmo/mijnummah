package nl.mijnummah.mosque.api;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.mosque.MosqueRole;
import nl.mijnummah.mosque.MosqueStatus;
import nl.mijnummah.mosque.application.MosqueView;
import nl.mijnummah.storage.ImageStorage;

/**
 * Public profile of a mosque.
 *
 * @param logoUrl  path of the logo relative to the API base URL; null when there is none
 * @param coverUrl       path of the cover photo relative to the API base URL; null when there is none
 * @param prayerTimesUrl link to the mosque's prayer timetable; null when there is none
 */
record MosqueResponse(UUID id, String slug, String name, String description, Address address, Contact contact,
                      String prayerTimesUrl, String timeZone, String logoUrl, String coverUrl) {

    record Address(String street, String postalCode, String city, String countryCode) {
    }

    record Contact(String email, String phone, String websiteUrl) {
    }

    static MosqueResponse from(MosqueView view) {
        var d = view.details();
        return new MosqueResponse(view.id(), view.slug(), d.name(), d.description(),
                new Address(d.street(), d.postalCode(), d.city(), d.countryCode()),
                new Contact(d.email(), d.phone(), d.websiteUrl()), d.prayerTimesUrl(), d.timeZone(), imagePath(view.logoImageId()),
                imagePath(view.coverImageId()));
    }

    private static String imagePath(UUID imageId) {
        return imageId == null ? null : ImageStorage.publicPath(imageId);
    }

    /** Admin view: public profile plus the image IDs, status and the caller's own role. */
    record Admin(UUID id, String slug, String name, String description, Address address, Contact contact,
                 String prayerTimesUrl, String timeZone, String logoUrl, String coverUrl, UUID logoImageId,
                 UUID coverImageId, MosqueStatus status, MosqueRole myRole, Instant createdAt, Instant updatedAt) {

        static Admin from(MosqueView view) {
            var p = MosqueResponse.from(view);
            return new Admin(p.id(), p.slug(), p.name(), p.description(), p.address(), p.contact(),
                    p.prayerTimesUrl(), p.timeZone(), p.logoUrl(), p.coverUrl(), view.logoImageId(),
                    view.coverImageId(), view.status(), view.myRole(), view.createdAt(), view.updatedAt());
        }
    }
}
