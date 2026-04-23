package dev.cinder.note.exception;

import org.springframework.http.HttpStatus;

public class NoteNotFoundException extends CinderException {
    public NoteNotFoundException(String id) {
        super(HttpStatus.NOT_FOUND, "Note not found: " + id);
    }
}
