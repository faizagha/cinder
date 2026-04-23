package dev.cinder.note.notes;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Background job that hard-deletes expired notes.
 * Runs on a fixed cadence — interval tunes the trade-off between
 * staleness and DB load. 60s is a sane default for V1.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NoteCleanup {

    private final NoteRepository noteRepository;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void deleteExpiredNotes() {
        int deleted = noteRepository.deleteAllByExpiresAtBefore(Instant.now());
        if (deleted > 0) {
            log.info("cleanup deleted {} expired notes", deleted);
        }
    }
}