package dev.cinder.note.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Base class for all Cinder application exceptions.
 * Carries an HTTP status so the global handler can map it directly.
 */
@Getter
public abstract class CinderException extends RuntimeException {

    private final HttpStatus status;

    protected CinderException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected CinderException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
