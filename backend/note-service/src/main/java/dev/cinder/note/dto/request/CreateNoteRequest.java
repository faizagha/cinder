package dev.cinder.note.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound payload for creating a new note.
 * The server is blind to plaintext — it only stores ciphertext + crypto
 * metadata.
 */
public record CreateNoteRequest(
        /** AES-GCM encrypted content, base64-encoded. */
        @NotBlank @Size(max = 1_000_000) String ciphertext,

        /** AES-GCM initialization vector, base64-encoded. */
        @NotBlank @Size(max = 32) String iv,

        /** PBKDF2 salt, base64-encoded. null for non-passphrase notes. */
        @Size(max = 32) String salt,

        /** If true, the note is hard-deleted on first successful read. */
        Boolean burnAfterReading,

        /** Time-to-live in seconds. null = never expires. */
        Long expiresInSeconds) {
}