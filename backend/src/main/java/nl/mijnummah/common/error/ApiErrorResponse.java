package nl.mijnummah.common.error;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Uniform error body returned by every REST endpoint.
 *
 * @param timestamp   moment the error was produced (UTC)
 * @param status      HTTP status code
 * @param code        stable, machine-readable error code (e.g. {@code EVENT_FULL}); clients branch on this
 * @param message     human-readable explanation; never contains stack traces or internal details
 * @param path        request path that produced the error
 * @param fieldErrors per-field validation errors (only for {@code VALIDATION_FAILED}), so forms can show them
 *                    next to the right input
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors
) {

    public ApiErrorResponse(Instant timestamp, int status, String code, String message, String path) {
        this(timestamp, status, code, message, path, List.of());
    }

    public record FieldError(String field, String message) {
    }
}
