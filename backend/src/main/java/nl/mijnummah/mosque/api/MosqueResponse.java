package nl.mijnummah.mosque.api;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.mosque.MosqueRole;
import nl.mijnummah.mosque.application.MosqueView;

/** Public profile of a mosque. */
record MosqueResponse(UUID id, String slug, String name, String description, Address address, Contact contact,
                      String timeZone) {

    record Address(String street, String postalCode, String city, String countryCode) {
    }

    record Contact(String email, String phone, String websiteUrl) {
    }

    static MosqueResponse from(MosqueView view) {
        var d = view.details();
        return new MosqueResponse(view.id(), view.slug(), d.name(), d.description(),
                new Address(d.street(), d.postalCode(), d.city(), d.countryCode()),
                new Contact(d.email(), d.phone(), d.websiteUrl()), d.timeZone());
    }

    /** Admin view: public profile plus status and the caller's own role. */
    record Admin(UUID id, String slug, String name, String description, Address address, Contact contact,
                 String timeZone, boolean active, MosqueRole myRole, Instant createdAt, Instant updatedAt) {

        static Admin from(MosqueView view) {
            var p = MosqueResponse.from(view);
            return new Admin(p.id(), p.slug(), p.name(), p.description(), p.address(), p.contact(), p.timeZone(),
                    view.active(), view.myRole(), view.createdAt(), view.updatedAt());
        }
    }
}
