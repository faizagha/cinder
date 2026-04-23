package dev.cinder.note.exception;

import org.springframework.http.HttpStatus;

public class InvalidEncryptionDataException extends CinderException {
    public InvalidEncryptionDataException(String reason) {
        super(HttpStatus.BAD_REQUEST, "Invalid encryption data: " + reason);
    }
}