package dev.cinder.note.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Translates exceptions into consistent JSON error responses.
 * Lives at the boundary so service/repo code stays free of HTTP concerns.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Any of our custom exceptions — uses their built-in status. */
    @ExceptionHandler(CinderException.class)
    public ResponseEntity<ErrorResponse> handleCinderException(
            CinderException ex, HttpServletRequest request) {

        log.info("handled {} status={} path={}",
                ex.getClass().getSimpleName(), ex.getStatus(), request.getRequestURI());

        return ResponseEntity
                .status(ex.getStatus())
                .body(ErrorResponse.of(
                        ex.getStatus().value(),
                        ex.getStatus().getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    /** Bean validation failures from @Valid on @RequestBody. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        // collapse all field errors into one message
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("validation failed");

        log.info("validation error path={} message={}", request.getRequestURI(), message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", message, request.getRequestURI()));
    }

    /**
     * Catch-all for unexpected errors. NEVER leak stack traces or internal details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        // log full stack trace internally, but DON'T expose it to the client
        log.error("unhandled exception path={}", request.getRequestURI(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error",
                        "An unexpected error occurred", request.getRequestURI()));
    }
}
