package nl.mijnummah.mosque.domain;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;

/** The editable profile of a mosque. Text is trimmed; blank optional fields become null. */
public record MosqueDetails(
        String name,
        String description,
        String street,
        String postalCode,
        String city,
        String countryCode,
        String email,
        String phone,
        String websiteUrl,
        String timeZone) {

    public MosqueDetails {
        name = Objects.requireNonNull(trimToNull(name), "name");
        description = trimToNull(description);
        street = trimToNull(street);
        postalCode = trimToNull(postalCode);
        city = trimToNull(city);
        countryCode = countryCode == null || countryCode.isBlank() ? "NL" : countryCode.trim().toUpperCase(Locale.ROOT);
        email = email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
        phone = trimToNull(phone);
        websiteUrl = trimToNull(websiteUrl);
        timeZone = timeZone == null || timeZone.isBlank() ? "Europe/Amsterdam" : timeZone.trim();
        try {
            ZoneId.of(timeZone);
        } catch (DateTimeException ex) {
            throw new InvalidMosqueException("timeZone is not a valid IANA time zone (e.g. Europe/Amsterdam).");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
