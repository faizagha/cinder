package dev.cinder.note.notes;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

    /** nanoid (10 chars), generated in service before save */
    @Id
    @Column(length = 12, nullable = false, updatable = false)
    private String id;

    /** AES-GCM encrypted content, base64-encoded. server is blind to plaintext. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String ciphertext;

    /**
     * AES-GCM initialization vector, base64-encoded. needed by client to decrypt.
     */
    @Column(nullable = false, length = 32)
    private String iv;

    /**
     * PBKDF2 salt for passphrase-protected notes. null for non-passphrase notes.
     */
    @Column(length = 32)
    private String salt;

    /** if true, deletes itself on first successful read */
    @Column(nullable = false)
    private boolean burnAfterReading;

    /** null = never expires. background job hard-deletes expired rows. */
    private Instant expiresAt;

    /** auto-filled by hibernate on insert. UTC. */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** auto-updated on any field change. */
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}