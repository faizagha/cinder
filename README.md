
# Cinder

> End-to-end encrypted ephemeral notes. The server cannot read your content — even if it wanted to.

Cinder is a paste-bin-style note service where the server is **fundamentally blind** to what users store. Encryption happens entirely in the browser. The decryption key lives in the URL fragment, which browsers never send to the server. Lose the URL, lose the note — by design.

```
Browser:  encrypts plaintext  →  builds URL like  cinder.app/p/abc123#xK9zP...
                                                                    └── key, never seen by server

Server:   stores ciphertext + IV.  Has no idea what's in it.  Cannot decrypt. Cannot recover.
```

---

## Why this exists

Most "encrypted" web tools encrypt data **in transit** (TLS) and **at rest** (server-side encryption with operator-held keys). That's fine for casual privacy, but it means the operator — or anyone who compromises the operator — can read everything.

Cinder uses a different model: **the operator never has the key**. The server stores opaque ciphertext. A subpoena, a database leak, a malicious sysadmin — none of them yield plaintext. The price: if a user loses the URL, the content is gone forever. There is no recovery mechanism, by construction.

This is the same model used by [PrivateBin](https://privatebin.info/), [Firefox Send](https://en.wikipedia.org/wiki/Firefox_Send) (RIP), and [CryptPad](https://cryptpad.org/). Cinder is a small, focused implementation of the same primitive.

---

## How it works

### Create flow

1. Browser generates a fresh **AES-GCM 256-bit key (`K`)** and a **96-bit IV** for this note.
2. Browser encrypts the plaintext: `ciphertext = AES-GCM(plaintext, K, IV)`.
3. Browser sends `{ciphertext, iv, expiresIn, burnAfterReading}` to the backend. **`K` is never transmitted.**
4. Backend persists the row, returns a nanoid `id`.
5. Browser builds the share URL: `<host>/#<id>.<base64url(K)>`.
6. User copies the URL and shares it through any trusted channel.

### Read flow

1. Recipient opens the URL.
2. Browser parses `id` and `K` from the URL fragment (everything after `#`).
3. Browser fetches `GET /api/v1/notes/{id}` → receives `{ciphertext, iv, ...}`.
4. Browser decrypts locally: `plaintext = AES-GCM-decrypt(ciphertext, K, IV)`.
5. If the note was flagged `burnAfterReading`, the backend hard-deletes the row immediately on first GET.

### What the server stores

```
notes
├── id              VARCHAR(12)   ← random nanoid
├── ciphertext      TEXT          ← opaque base64-encoded blob
├── iv              VARCHAR(32)   ← AES-GCM initialization vector (not secret)
├── salt            VARCHAR(32)   ← reserved for passphrase-protected notes
├── burn_after_reading BOOLEAN
├── expires_at      TIMESTAMPTZ
├── created_at      TIMESTAMPTZ
└── updated_at      TIMESTAMPTZ
```

No content. No keys. No IPs. No correlation between rows and users. A complete database dump is a list of useless ciphertexts.

---

## Architecture

Monorepo with feature-grouped Spring Boot service. Frontend is a single-file vanilla HTML app — no build step, no framework.

```
cinder/
├── backend/
│   └── note-service/                 ← Spring Boot 4 + JPA + H2 (Postgres in V1.5)
│       └── src/main/java/dev/cinder/note/
│           ├── notes/                ← entity, repo, service, controller, cleanup job
│           ├── dto/                  ← request/response records
│           ├── exception/            ← custom exception hierarchy + global handler
│           └── configs/              ← CORS, web config
├── frontend/
│   └── index.html                    ← brutalist UI + Web Crypto layer
├── gradle/                           ← wrapper
└── settings.gradle.kts
```

### Stack

- **Java 21** + **Spring Boot 4.x** (Spring Web, Spring Data JPA, Validation)
- **H2** in-memory for V1 (Postgres + Flyway in V1.5)
- **Lombok** for boilerplate-free entities + DTOs
- **jnanoid** for URL-safe random IDs
- **Web Crypto API** for client-side AES-GCM 256
- **Vanilla HTML/CSS/JS** for frontend — zero dependencies, brutalist by intent

---

## Running it locally

### Prerequisites
- Java 21+ (Temurin recommended)
- Python 3 (for serving the frontend statically — anything that serves files works)

### Backend
```bash
./gradlew :backend:note-service:bootRun
```
Service starts on `http://localhost:8080`.

H2 console available at `http://localhost:8080/h2-console`:
- JDBC URL: `jdbc:h2:mem:cinderdb`
- User: `sa`
- Password: *(empty)*

### Frontend
```bash
cd frontend
python3 -m http.server 8000
```
Open `http://localhost:8000/index.html`.

### Smoke test
1. Type a note in the editor
2. Click **Drop it**
3. Copy the URL
4. Open it in a new (incognito) tab — note decodes
5. If burn-after-reading was on, refresh — note is gone

---

## API

### `POST /api/v1/notes`
Stores a new encrypted note. Server is blind to plaintext.

```json
{
  "ciphertext": "<base64>",
  "iv": "<base64>",
  "salt": null,
  "burnAfterReading": true,
  "expiresInSeconds": 900
}
```

Returns `201 Created`:
```json
{ "id": "aB3kZ9xQpL" }
```

### `GET /api/v1/notes/{id}`
Returns the encrypted note. Decryption is client-side. If `burnAfterReading=true`, the row is hard-deleted on first successful GET.

```json
{
  "id": "aB3kZ9xQpL",
  "ciphertext": "<base64>",
  "iv": "<base64>",
  "salt": null,
  "burnAfterReading": true,
  "expiresAt": "2026-04-22T13:00:00Z",
  "createdAt": "2026-04-22T12:00:00Z"
}
```

Errors are returned as a consistent JSON shape (handled by `GlobalExceptionHandler`):
```json
{
  "timestamp": "2026-04-22T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Note not found: aB3kZ9xQpL",
  "path": "/api/v1/notes/aB3kZ9xQpL"
}
```

---

## Threat model

See [`THREAT_MODEL.md`](./THREAT_MODEL.md) for the full breakdown. TL;DR:

**Cinder protects against:**
- Server compromise — only ciphertext is exposed
- Operator subpoena — there is no plaintext to hand over
- Database leaks — useless without per-note keys
- Casual URL leakage — short expirations + burn-after-reading
- Operator misconduct — operator cannot decrypt content

**Cinder does NOT protect against:**
- A user sharing the URL with the wrong person
- Compromised endpoints (malware on the recipient's device)
- Coercion of the operator (forced code changes — endpoint trust problem)
- Targeted state-level adversaries
- URL leakage via insecure channels (e.g. plaintext SMS)

For state-level threats, use [SecureDrop](https://securedrop.org/), [Signal](https://signal.org/), or air-gapped systems. **Cinder is the right tool for "I want to share a 2FA code or password without it living forever in Discord."**

---

## Logging policy

Cinder logs operational events using note IDs only. Cinder does NOT log:
- Note content or ciphertext
- IP addresses
- User-Agent strings
- Anything that could correlate users to specific notes

Note IDs are random nanoids and do not identify users.

---

## Roadmap

### ✅ V1 — Encrypted note service (shipped)
- Server-blind storage (ciphertext + IV + crypto metadata only)
- AES-GCM 256 client-side encryption via Web Crypto API
- URL-fragment key transport (`#<id>.<key>`)
- Burn-after-reading deletion
- Scheduled expiration cleanup (60s sweep)
- Custom exception hierarchy + global error handler
- Privacy-aware logging

### 🚧 V1.5 — Production-shape backend (next)
- Migrate H2 → **PostgreSQL** with Flyway-managed migrations
- **Custom Redis-backed token-bucket rate limiter** (atomic Lua scripts, per-IP + per-endpoint)
- **View-burst detection** — auto-delete notes hit by anomalous read patterns (Redis-only, no PII)
- **Observability stack** — Prometheus metrics, Grafana dashboards, structured JSON logs to Loki
- **Load testing** with k6 + benchmarks doc
- **Passphrase-protected notes** (PBKDF2 key derivation on top of the URL key)
- Deployment with Docker + docker-compose

### 🔮 V2 — Realtime collaborative notes
- Add Node.js `realtime-service` (y-websocket protocol)
- Encrypted CRDT sync — server relays opaque updates, never sees plaintext
- Cursor presence + awareness (encrypted same way)
- Persistent encrypted document history in Postgres
- API gateway (nginx) routing REST + WebSocket
- Full multi-service deployment

---

## License

MIT. See [`LICENSE`](./LICENSE).

---

## Acknowledgements

The threat model and architectural pattern is shamelessly inspired by [PrivateBin](https://privatebin.info/) and [Firefox Send](https://en.wikipedia.org/wiki/Firefox_Send). This project exists because I wanted to understand how those tools actually work, end to end — and the best way to understand a system is to build a small version of it.
