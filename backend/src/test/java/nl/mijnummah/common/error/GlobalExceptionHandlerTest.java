package nl.mijnummah.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    @Test
    void everyCategoryMapsToItsHttpStatus() {
        assertThat(GlobalExceptionHandler.statusOf(ErrorCategory.INVALID_REQUEST)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(GlobalExceptionHandler.statusOf(ErrorCategory.UNAUTHORIZED)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(GlobalExceptionHandler.statusOf(ErrorCategory.FORBIDDEN)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(GlobalExceptionHandler.statusOf(ErrorCategory.NOT_FOUND)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(GlobalExceptionHandler.statusOf(ErrorCategory.CONFLICT)).isEqualTo(HttpStatus.CONFLICT);
        assertThat(GlobalExceptionHandler.statusOf(ErrorCategory.BUSINESS_RULE_VIOLATION).value()).isEqualTo(422);
        assertThat(GlobalExceptionHandler.statusOf(ErrorCategory.TOO_MANY_REQUESTS))
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
