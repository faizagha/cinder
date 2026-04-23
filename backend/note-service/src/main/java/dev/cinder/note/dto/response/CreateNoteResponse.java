package dev.cinder.note.dto.response;

import dev.cinder.note.notes.Note;

public record CreateNoteResponse(
                String id) {

        public static CreateNoteResponse convertToResponse(Note note) {
                return new CreateNoteResponse(note.getId());
        }
}