package nl.mijnummah.common.profile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class LocalOnlyTest {

    @Test
    void developmentSettingsAreRefusedOutsideLocalAndTest() {
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("prod");
        assertThatIllegalStateException().isThrownBy(() -> LocalOnly.require(production, "MAIL_PROVIDER=log"))
                .withMessageContaining("MAIL_PROVIDER=log");
        assertThatIllegalStateException().isThrownBy(() -> LocalOnly.require(new MockEnvironment(), "PUSH_PROVIDER=log"));
    }

    @Test
    void developmentSettingsAreAllowedLocally() {
        for (String profile : new String[] {"local", "test"}) {
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles(profile);
            assertThatCode(() -> LocalOnly.require(environment, "MAIL_PROVIDER=log")).doesNotThrowAnyException();
        }
    }
}
