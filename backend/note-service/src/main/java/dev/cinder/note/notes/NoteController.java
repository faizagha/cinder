package dev.cinder.note.notes;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.cinder.note.dto.request.CreateNoteRequest;
import dev.cinder.note.dto.response.CreateNoteResponse;
import dev.cinder.note.dto.response.NoteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    /** POST /api/v1/notes — store a new encrypted note. */
    @PostMapping
    public ResponseEntity<CreateNoteResponse> createNote(@Valid @RequestBody CreateNoteRequest request) {
        CreateNoteResponse response = noteService.createNote(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * GET /api/v1/notes/{id} — fetch ciphertext + crypto metadata. Decryption is
     * client-side.
     */
    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> getNote(@PathVariable String id) {
        NoteResponse response = noteService.getNote(id);
        return ResponseEntity.ok(response);
    }
}