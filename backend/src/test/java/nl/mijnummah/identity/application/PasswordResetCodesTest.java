package nl.mijnummah.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PasswordResetCodesTest {

    @Test
    void codesUseOnlyUnambiguousCharacters() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            String code = PasswordResetService.generate();
            assertThat(code).hasSize(8).matches("[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{8}");
            seen.add(code);
        }
        assertThat(seen).hasSizeGreaterThan(990);
    }

    @Test
    void typedCodesAreNormalizedAndShownInTwoGroups() {
        assertThat(PasswordResetService.normalize(" abcd-efgh ")).isEqualTo("ABCDEFGH");
        assertThat(PasswordResetService.normalize("ABCD EFGH")).isEqualTo("ABCDEFGH");
        assertThat(PasswordResetService.normalize(null)).isEmpty();
        assertThat(PasswordResetService.format("ABCDEFGH")).isEqualTo("ABCD-EFGH");
    }

    @Test
    void theEmailContainsTheFormattedCodeAndNoOtherSecrets() {
        String text = PasswordResetMailer.text(new PasswordResetCodeIssued("a@example.com", "Amina", "ABCDEFGH",
                PasswordResetCodeIssued.Kind.SELF_SERVICE), "");
        assertThat(text).contains("Salaam Amina", "ABCD-EFGH", "30 minuten");
    }

    @Test
    void anInvitationLinksToThePortalWhenItsAddressIsKnown() {
        var invitation = new PasswordResetCodeIssued("a@example.com", "Amina", "ABCDEFGH",
                PasswordResetCodeIssued.Kind.INVITATION);
        assertThat(PasswordResetMailer.text(invitation, "https://beheer.example.nl"))
                .contains("ABCD-EFGH", "72 uur", "https://beheer.example.nl/wachtwoord-instellen");
        assertThat(PasswordResetMailer.text(invitation, "")).doesNotContain("http").contains("Wachtwoord instellen");
        assertThat(PasswordResetMailer.subject(invitation)).isEqualTo("Je account bij Mijn Ummah");
    }
}
