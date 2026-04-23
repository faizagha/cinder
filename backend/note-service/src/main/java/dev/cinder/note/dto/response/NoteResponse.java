package dev.cinder.note.dto.response;

import java.time.Instant;

import dev.cinder.note.notes.Note;

public record NoteResponse(
        String id,
        String ciphertext,
        String iv,
        String salt,
        boolean burnAfterReading,
        Instant expiresAt,
        Instant createdAt) {
    public static NoteResponse convertToResponse(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getCiphertext(),
                note.getIv(),
                note.getSalt(),
                note.isBurnAfterReading(),
                note.getExpiresAt(),
                note.getCreatedAt());
    }
}
