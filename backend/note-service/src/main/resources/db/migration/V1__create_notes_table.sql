CREATE TABLE notes(
	id 					VARCHAR(12) 			    NOT NULL,
	burn_after_reading  BOOLEAN 				    NOT NULL,
	ciphertext 			TEXT 					    NOT NULL,
	created_at 			TIMESTAMP WITH TIME ZONE    NOT NULL,
	expires_at 			TIMESTAMP WITH TIME ZONE,
	iv 					VARCHAR(32) 			    NOT NULL,
	salt 				VARCHAR(32),
	updated_at 			TIMESTAMP WITH TIME ZONE    NOT NULL,
	PRIMARY KEY (id)
);

CREATE INDEX idx_notes_expires_at
    ON notes (expires_at)
    WHERE expires_at IS NOT NULL;