package nl.mijnummah.mosque;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The permission matrix. Changing it is a security decision; this test makes such a change explicit. */
class MosqueRoleTest {

    @Test
    void adminHasEveryPermission() {
        for (MosquePermission permission : MosquePermission.values()) {
            assertThat(MosqueRole.ADMIN.grants(permission)).as(permission.name()).isTrue();
        }
    }

    @Test
    void editorOnlyManagesContent() {
        assertThat(MosqueRole.EDITOR.grants(MosquePermission.MANAGE_CONTENT)).isTrue();
        assertThat(MosqueRole.EDITOR.grants(MosquePermission.VIEW_REGISTRATIONS)).isFalse();
        assertThat(MosqueRole.EDITOR.grants(MosquePermission.MANAGE_MOSQUE)).isFalse();
        assertThat(MosqueRole.EDITOR.grants(MosquePermission.MANAGE_ADMINISTRATORS)).isFalse();
    }
}
