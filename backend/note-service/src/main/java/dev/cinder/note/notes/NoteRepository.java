package dev.cinder.note.notes;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, String> {

    int deleteAllByExpiresAtBefore(Instant now);

}
