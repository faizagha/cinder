package dev.cinder.note.exception;

import org.springframework.http.HttpStatus;

public class NoteAlreadyViewedException extends CinderException {
    public NoteAlreadyViewedException(String id) {
        super(HttpStatus.GONE, "This note was already viewed and burned: " + id);
    }
}