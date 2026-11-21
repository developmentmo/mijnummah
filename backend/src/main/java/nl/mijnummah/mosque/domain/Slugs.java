package nl.mijnummah.mosque.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/** Turns a mosque's name into a unique slug: lower-case letters, digits and single hyphens, at most 60 characters. */
public final class Slugs {

    public static final int MAX_LENGTH = 60;

    private Slugs() {
    }

    /** "Moskee Noeroel-Islam (Den Haag)" becomes "moskee-noeroel-islam-den-haag"; "moskee" when nothing is left. */
    public static String fromName(String name) {
        String ascii = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        String slug = trim(ascii, MAX_LENGTH);
        return slug.isEmpty() ? "moskee" : slug;
    }

    /** {@code base}, or {@code base-2}, {@code base-3} and so on: the first that is not taken. */
    public static String unique(String base, Predicate<String> taken) {
        if (!taken.test(base)) {
            return base;
        }
        for (int n = 2; ; n++) {
            String suffix = "-" + n;
            String candidate = trim(base, MAX_LENGTH - suffix.length()) + suffix;
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
    }

    private static String trim(String slug, int max) {
        return slug.length() <= max ? slug : slug.substring(0, max).replaceAll("-+$", "");
    }
}
