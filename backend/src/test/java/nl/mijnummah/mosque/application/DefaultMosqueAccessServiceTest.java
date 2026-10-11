package nl.mijnummah.mosque.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.mijnummah.common.error.NotFoundException;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.identity.PlatformRole;
import nl.mijnummah.mosque.MosqueAccessDeniedException;
import nl.mijnummah.mosque.MosquePermission;
import nl.mijnummah.mosque.MosqueRole;
import nl.mijnummah.mosque.domain.MosqueAdministrator;
import nl.mijnummah.mosque.infrastructure.MosqueAdministratorRepository;
import nl.mijnummah.mosque.infrastructure.MosqueRepository;

class DefaultMosqueAccessServiceTest {

    private final UUID mosqueA = UUID.randomUUID();
    private final UUID mosqueB = UUID.randomUUID();
    private final AuthenticatedUser platformAdmin = user(PlatformRole.PLATFORM_ADMIN);
    private final AuthenticatedUser adminOfA = user(PlatformRole.USER);
    private final AuthenticatedUser editorOfA = user(PlatformRole.USER);
    private final AuthenticatedUser stranger = user(PlatformRole.USER);

    private MosqueRepository mosques;
    private MosqueAdministratorRepository administrators;
    private DefaultMosqueAccessService access;

    @BeforeEach
    void setUp() {
        mosques = mock(MosqueRepository.class);
        administrators = mock(MosqueAdministratorRepository.class);
        when(mosques.existsById(mosqueA)).thenReturn(true);
        when(mosques.existsById(mosqueB)).thenReturn(true);
        when(administrators.findByMosqueIdAndUserId(mosqueA, adminOfA.id()))
                .thenReturn(Optional.of(MosqueAdministrator.grant(mosqueA, adminOfA.id(), MosqueRole.ADMIN, null, Instant.now())));
        when(administrators.findByMosqueIdAndUserId(mosqueA, editorOfA.id()))
                .thenReturn(Optional.of(MosqueAdministrator.grant(mosqueA, editorOfA.id(), MosqueRole.EDITOR, null, Instant.now())));
        access = new DefaultMosqueAccessService(mosques, administrators);
    }

    @Test
    void platformAdminMayDoEverythingForEveryMosque() {
        for (MosquePermission permission : MosquePermission.values()) {
            assertThatCode(() -> access.require(platformAdmin, mosqueA, permission)).doesNotThrowAnyException();
            assertThatCode(() -> access.require(platformAdmin, mosqueB, permission)).doesNotThrowAnyException();
        }
    }

    @Test
    void adminMayDoEverythingForTheirOwnMosqueOnly() {
        for (MosquePermission permission : MosquePermission.values()) {
            assertThat(access.has(adminOfA, mosqueA, permission)).isTrue();
            assertThat(access.has(adminOfA, mosqueB, permission)).isFalse();
        }
        assertThatThrownBy(() -> access.require(adminOfA, mosqueB, MosquePermission.MANAGE_CONTENT))
                .isInstanceOf(MosqueAccessDeniedException.class);
    }

    @Test
    void editorMayOnlyManageContentOfTheirMosque() {
        assertThat(access.has(editorOfA, mosqueA, MosquePermission.MANAGE_CONTENT)).isTrue();
        assertThat(access.has(editorOfA, mosqueA, MosquePermission.VIEW_REGISTRATIONS)).isFalse();
        assertThat(access.has(editorOfA, mosqueA, MosquePermission.MANAGE_MOSQUE)).isFalse();
        assertThat(access.has(editorOfA, mosqueA, MosquePermission.MANAGE_ADMINISTRATORS)).isFalse();
        assertThat(access.has(editorOfA, mosqueB, MosquePermission.MANAGE_CONTENT)).isFalse();
    }

    @Test
    void userWithoutRoleMayDoNothing() {
        for (MosquePermission permission : MosquePermission.values()) {
            assertThatThrownBy(() -> access.require(stranger, mosqueA, permission))
                    .isInstanceOf(MosqueAccessDeniedException.class);
        }
    }

    @Test
    void unknownMosqueIsNotFoundEvenForPlatformAdmin() {
        UUID unknown = UUID.randomUUID();
        assertThatThrownBy(() -> access.require(platformAdmin, unknown, MosquePermission.MANAGE_CONTENT))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo("MOSQUE_NOT_FOUND");
        assertThat(access.has(platformAdmin, unknown, MosquePermission.MANAGE_CONTENT)).isFalse();
    }

    private static AuthenticatedUser user(PlatformRole role) {
        return new AuthenticatedUser(UUID.randomUUID(), UUID.randomUUID() + "@example.com", role);
    }
}
