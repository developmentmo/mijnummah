package nl.mijnummah.mosque.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import nl.mijnummah.mosque.domain.MosqueDetails;

/** Editable mosque profile. Optional fields may be omitted or blank. */
record MosqueDetailsRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 4000) String description,
        @Size(max = 150) String street,
        @Size(max = 20) String postalCode,
        @Size(max = 100) String city,
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "must be a two-letter country code") String countryCode,
        @Email @Size(max = 254) String email,
        @Size(max = 30) String phone,
        @Size(max = 500) @Pattern(regexp = "^https?://\\S+$", message = "must be an http(s) URL") String websiteUrl,
        @Size(max = 50) String timeZone) {

    MosqueDetails toDetails() {
        return new MosqueDetails(name, description, street, postalCode, city, countryCode, email, phone, websiteUrl,
                timeZone);
    }
}
