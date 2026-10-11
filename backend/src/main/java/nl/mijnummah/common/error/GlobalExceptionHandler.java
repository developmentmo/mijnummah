package nl.mijnummah.common.error;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates exceptions into the uniform {@link ApiErrorResponse} format.
 * <p>
 * Business errors extend {@link ApplicationException}; their {@link ErrorCategory} determines the HTTP status.
 * Framework errors are mapped explicitly below. Anything else becomes a 500 without leaking details.
 * Authentication failures in the security filter chain never reach this class; the identity module writes
 * them in the same format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ApiErrorResponse> handleApplication(ApplicationException ex, HttpServletRequest request) {
        HttpStatus status = statusOf(ex.category());
        ApiErrorResponse body = body(status, ex.code(), ex.getMessage(), request);
        if (ex instanceof RetryAfterAware retry) {
            return ResponseEntity.status(status)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(RetryAfterAware.seconds(retry.retryAfter())))
                    .body(body);
        }
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action.", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ApiErrorResponse.FieldError::field)
                        .thenComparing(ApiErrorResponse.FieldError::message))
                .toList();
        var globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(error -> new ApiErrorResponse.FieldError(error.getObjectName(), error.getDefaultMessage()));
        List<ApiErrorResponse.FieldError> all = java.util.stream.Stream.concat(fieldErrors.stream(), globalErrors).toList();
        return validationFailed(all, request);
    }

    /** Constraint violations on {@code @RequestParam} / {@code @PathVariable} (method validation). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiErrorResponse> handleMethodValidation(HandlerMethodValidationException ex,
                                                            HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ApiErrorResponse.FieldError(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return validationFailed(errors, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                        HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Parameter '" + ex.getName() + "' has an invalid value.", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Required parameter '" + ex.getParameterName() + "' is missing.", request);
    }

    /** Two administrators edited the same item at the same time; the later save loses. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                                          HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "This item was changed by someone else. Reload it and try again.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource does not exist.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                              HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(body(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                        "HTTP method " + ex.getMethod() + " is not supported for this resource.", request));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                     HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Content type is not supported.", request);
    }

    /** Last-resort handler. Logs the full exception server-side but never leaks its message to the client. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.", request);
    }

    static HttpStatus statusOf(ErrorCategory category) {
        return switch (category) {
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case BUSINESS_RULE_VIOLATION -> HttpStatus.valueOf(422); // constant name differs across Spring versions
            case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }

    private ResponseEntity<ApiErrorResponse> validationFailed(List<ApiErrorResponse.FieldError> errors,
                                                              HttpServletRequest request) {
        String message = errors.isEmpty()
                ? "Request validation failed."
                : String.join("; ", errors.stream().map(e -> e.field() + ": " + e.message()).toList());
        ApiErrorResponse body = new ApiErrorResponse(Instant.now(clock), 400, "VALIDATION_FAILED", message,
                request.getRequestURI(), errors);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message,
                                                   HttpServletRequest request) {
        return ResponseEntity.status(status).body(body(status, code, message, request));
    }

    private ApiErrorResponse body(HttpStatus status, String code, String message, HttpServletRequest request) {
        return new ApiErrorResponse(Instant.now(clock), status.value(), code, message, request.getRequestURI());
    }
}
