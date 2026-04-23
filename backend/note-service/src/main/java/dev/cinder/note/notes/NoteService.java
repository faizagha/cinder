package dev.cinder.note.notes;

import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import dev.cinder.note.dto.request.CreateNoteRequest;
import dev.cinder.note.dto.response.CreateNoteResponse;
import dev.cinder.note.dto.response.NoteResponse;
import dev.cinder.note.exception.NoteNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ID_LENGTH = 10;
    private static final long DEFAULT_TTL_SECONDS = 5 * 60; // 15 min, privacy-first
    private static final long MAX_TTL_SECONDS = 30L * 24 * 60 * 60; // 30 days hard cap

    private final NoteRepository noteRepository;

    /**
     * Stores an encrypted note. Server is blind to plaintext — it only persists
     * ciphertext + crypto metadata supplied by the client.
     */
    public CreateNoteResponse createNote(CreateNoteRequest req) {
        String id = NanoIdUtils.randomNanoId(RANDOM, NanoIdUtils.DEFAULT_ALPHABET, ID_LENGTH);

        boolean burn = req.burnAfterReading() == null || req.burnAfterReading();
        Instant expiresAt = computeExpiresAt(req.expiresInSeconds());
        boolean hasPassphrase = req.salt() != null;

        Note note = Note.builder()
                .id(id)
                .ciphertext(req.ciphertext())
                .iv(req.iv())
                .salt(req.salt())
                .burnAfterReading(burn)
                .expiresAt(expiresAt)
                .build();

        Note saved = noteRepository.save(note);

        log.info("note created id={} burn={} passphrase={} expiresAt={}",
                id, burn, hasPassphrase, expiresAt);

        return CreateNoteResponse.convertToResponse(saved);
    }

    /**
     * Returns an encrypted note by id. Decryption happens client-side using
     * the key from the URL fragment.
     */
    public NoteResponse getNote(String id) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("note lookup miss id={}", id);
                    return new NoteNotFoundException(id);
                });

        log.debug("note retrieved id={}", id);
        NoteResponse noteResponse = NoteResponse.convertToResponse(note);

        if (note.isBurnAfterReading()) {
            noteRepository.deleteById(note.getId());
            log.info("note burned after reading id={}", id);
        }

        return noteResponse;
    }

    /**
     * Computes expiry timestamp from client-supplied TTL.
     * - null → default 15 min
     * - ≤ 0 → never expires (capped at MAX_TTL_SECONDS for storage hygiene)
     * - > MAX → capped at MAX_TTL_SECONDS
     * - else → now + ttl
     */
    private Instant computeExpiresAt(Long ttlSeconds) {
        if (ttlSeconds == null) {
            return Instant.now().plusSeconds(DEFAULT_TTL_SECONDS);
        }
        if (ttlSeconds <= 0 || ttlSeconds > MAX_TTL_SECONDS) {
            return Instant.now().plusSeconds(MAX_TTL_SECONDS);
        }
        return Instant.now().plusSeconds(ttlSeconds);
    }
}