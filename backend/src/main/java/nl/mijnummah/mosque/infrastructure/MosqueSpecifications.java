package nl.mijnummah.mosque.infrastructure;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import nl.mijnummah.mosque.MosqueStatus;
import nl.mijnummah.mosque.domain.Mosque;

/** Filters for the public list of mosques. A null or blank argument means "no filter". */
public final class MosqueSpecifications {

    private MosqueSpecifications() {
    }

    public static Specification<Mosque> status(MosqueStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Case-insensitive search in the name. LIKE wildcards in the input are matched literally. */
    public static Specification<Mosque> nameMatches(String text) {
        if (text == null || text.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + escape(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\');
    }

    /** The exact place, ignoring case. */
    public static Specification<Mosque> city(String city) {
        if (city == null || city.isBlank()) {
            return Specification.unrestricted();
        }
        String value = city.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("city")), value);
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
