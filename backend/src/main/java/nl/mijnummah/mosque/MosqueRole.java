package nl.mijnummah.mosque;

import java.util.EnumSet;
import java.util.Set;

/** Role of a user for one specific mosque. */
public enum MosqueRole {

    /** Everything for this mosque: details, administrators, registrations and all content. */
    ADMIN(EnumSet.allOf(MosquePermission.class)),

    /** Content only: events, media and announcements. No personal data of registrations, no administrators. */
    EDITOR(EnumSet.of(MosquePermission.MANAGE_CONTENT));

    private final Set<MosquePermission> permissions;

    MosqueRole(Set<MosquePermission> permissions) {
        this.permissions = Set.copyOf(permissions);
    }

    public boolean grants(MosquePermission permission) {
        return permissions.contains(permission);
    }
}
