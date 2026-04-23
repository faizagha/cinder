package dev.cinder.note.exception;

import org.springframework.http.HttpStatus;

public class NoteExpiredException extends CinderException {
    public NoteExpiredException(String id) {
        super(HttpStatus.GONE, "This note has expired: " + id);
    }
}