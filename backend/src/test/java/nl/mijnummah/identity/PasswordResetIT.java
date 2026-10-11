package nl.mijnummah.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;
import nl.mijnummah.common.mail.MailMessage;
import nl.mijnummah.common.mail.Mailer;

/** Forgotten password with a code by email. Emails are captured instead of sent. */
@Import(PasswordResetIT.CapturingMail.class)
class PasswordResetIT extends AbstractIntegrationTest {

    private static final Pattern CODE = Pattern.compile("\\b([A-Z2-9]{4}-[A-Z2-9]{4})\\b");
    private static final String NEW_PASSWORD = "mijn nieuwe wachtwoord";

    @Autowired
    private CapturingMailer mail;

    private Account account;

    @BeforeEach
    void accountAndEmptyInbox() {
        mail.sent.clear();
        account = user("vergeten");
    }

    @Test
    void theCodeFromTheEmailSetsANewPasswordSignsInAndEndsOtherSessions() {
        String oldRefresh = login(account.email(), PASSWORD).field("refreshToken");

        assertThat(requestCode(account.email()).status()).isEqualTo(202);
        MailMessage email = awaitEmail(account.email());
        assertThat(email.subject()).isEqualTo("Je code om je wachtwoord te herstellen");
        assertThat(email.text()).contains("30 minuten");

        ApiResponse confirmed = confirm(account.email(), codeIn(email).toLowerCase(), NEW_PASSWORD);
        assertThat(confirmed.status()).isEqualTo(200);
        assertThat(api.get("/api/v1/auth/me", confirmed.field("accessToken")).field("email"))
                .isEqualTo(account.email());

        assertThat(api.post("/api/v1/auth/refresh", Map.of("refreshToken", oldRefresh)).status()).isEqualTo(401);
        login(account.email(), NEW_PASSWORD);

        ApiResponse reused = confirm(account.email(), codeIn(email), "nog een ander wachtwoord");
        assertThat(reused.status()).isEqualTo(400);
        assertThat(reused.field("code")).isEqualTo("INVALID_RESET_CODE");
    }

    @Test
    void unknownAndBlockedAddressesGetTheSameAnswerWithoutEmail() {
        api.post("/api/v1/admin/users/" + account.id() + "/block", null, platformAdmin.token());

        assertThat(requestCode("onbekend@example.com").status()).isEqualTo(202);
        assertThat(requestCode(account.email()).status()).isEqualTo(202);

        Account other = user("ander");
        requestCode(other.email());
        awaitEmail(other.email());
        assertThat(mail.sent).extracting(MailMessage::to).containsExactly(other.email());

        ApiResponse unknown = confirm("onbekend@example.com", "ABCD-EFGH", NEW_PASSWORD);
        assertThat(unknown.status()).isEqualTo(400);
        assertThat(unknown.field("code")).isEqualTo("INVALID_RESET_CODE");
    }

    @Test
    void aCodeStopsWorkingAfterFiveWrongAttempts() {
        requestCode(account.email());
        String code = codeIn(awaitEmail(account.email()));

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(confirm(account.email(), "AAAA-AAAA", NEW_PASSWORD).field("code"))
                    .isEqualTo("INVALID_RESET_CODE");
        }
        assertThat(confirm(account.email(), code, NEW_PASSWORD).field("code")).isEqualTo("INVALID_RESET_CODE");
        login(account.email(), PASSWORD);
    }

    @Test
    void aNewCodeReplacesTheOldOneAndAWeakPasswordDoesNotUseUpTheCode() {
        requestCode(account.email());
        String first = codeIn(awaitEmail(account.email()));
        mail.sent.clear();
        requestCode(account.email());
        String second = codeIn(awaitEmail(account.email()));

        assertThat(confirm(account.email(), first, NEW_PASSWORD).field("code")).isEqualTo("INVALID_RESET_CODE");

        ApiResponse weak = confirm(account.email(), second, "kort");
        assertThat(weak.field("code")).isEqualTo("INVALID_PASSWORD");
        assertThat(confirm(account.email(), second, NEW_PASSWORD).status()).isEqualTo(200);
    }

    @Test
    void anAddressReceivesAtMostThreeEmailsPerHour() {
        for (int request = 0; request < 4; request++) {
            assertThat(requestCode(account.email()).status()).isEqualTo(202);
        }
        await().atMost(Duration.ofSeconds(10)).until(() -> mail.sent.size() >= 3);
        // give a fourth email the chance to (wrongly) arrive
        await().pollDelay(Duration.ofMillis(500)).until(() -> true);
        assertThat(mail.sent).hasSize(3);
    }

    private ApiResponse requestCode(String email) {
        return api.post("/api/v1/auth/password-reset", Map.of("email", email));
    }

    private ApiResponse confirm(String email, String code, String newPassword) {
        return api.post("/api/v1/auth/password-reset/confirm",
                Map.of("email", email, "code", code, "newPassword", newPassword));
    }

    private MailMessage awaitEmail(String to) {
        await().atMost(Duration.ofSeconds(10)).until(() -> mail.sent.stream().anyMatch(m -> m.to().equals(to)));
        List<MailMessage> toAddress = mail.sent.stream().filter(m -> m.to().equals(to)).toList();
        return toAddress.getLast();
    }

    private static String codeIn(MailMessage email) {
        Matcher matcher = CODE.matcher(email.text());
        assertThat(matcher.find()).as("code in email").isTrue();
        return matcher.group(1);
    }

    static final class CapturingMailer implements Mailer {

        final List<MailMessage> sent = new CopyOnWriteArrayList<>();

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public void send(MailMessage message) {
            sent.add(message);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CapturingMail {

        @Bean
        @Primary
        CapturingMailer capturingMailer() {
            return new CapturingMailer();
        }
    }
}
