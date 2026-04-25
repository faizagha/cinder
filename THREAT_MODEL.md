# Threat Model

This document describes what Cinder protects against, what it doesn't, and the assumptions underlying its security claims. It exists so users can make informed decisions about whether Cinder is the right tool for their use case.

**TL;DR:** Cinder protects users from the operator, the operator's database, and casual URL leakage. It does NOT protect against compromised endpoints, malicious operator-controlled code, or sophisticated targeted adversaries. For state-level threats, use [Signal](https://signal.org/) or [SecureDrop](https://securedrop.org/).

For active operational guarantees, see the [warrant canary](./CANARY.md).

---

## Security goals

In priority order, Cinder is designed to ensure:

1. **The server cannot decrypt user content.** Even with full access to the database, application code, runtime memory snapshots, and operator cooperation, the server has no path to plaintext.
2. **Notes are ephemeral by default.** Burn-after-reading is on by default; expiration defaults to 15 minutes; no note can live longer than 30 days.
3. **No persistent correlation between users and content.** Server logs and database use random note IDs only — no IPs, no User-Agents, no fingerprintable metadata. The rate limiter uses hashed-and-salted IPs with a rotating in-memory salt; raw IPs are never persisted anywhere.
4. **Recovery is impossible by design.** A lost URL means a lost note. There is no support flow, no admin override, no backup that helps.

These properties are enforced by **architecture**, not by **policy**. The operator cannot decrypt content even if compelled to.

---

## Cryptographic primitives

| Concern                                    | Choice                                         | Why                                                                                                                                                        |
| ------------------------------------------ | ---------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Symmetric encryption                       | **AES-GCM** with 256-bit keys                  | NIST-approved AEAD, hardware-accelerated, provides both confidentiality and integrity. Tampering causes decryption to throw, not silently produce garbage. |
| Key generation                             | `crypto.subtle.generateKey()` + browser CSPRNG | Cryptographically secure randomness from the OS. No predictable inputs.                                                                                    |
| IV                                         | 96-bit random per note                         | AES-GCM standard. Fresh per encryption. Public — stored alongside ciphertext.                                                                              |
| ID generation                              | `nanoid` (10 chars, URL-safe alphabet)         | ~10^18 possible IDs, no collisions in practice, safer than UUIDs in URLs.                                                                                  |
| Key transport                              | URL fragment (`#<id>.<base64url(K)>`)          | Browsers do not transmit URL fragments to servers. RFC 3986 § 3.5.                                                                                         |
| Rate-limit IP hashing                      | SHA-256(IP \|\| salt), salt rotated hourly     | Avoids storing raw IPs while allowing per-caller rate enforcement. Salt makes brute-force reversal infeasible.                                             |
| Admin token comparison                     | `MessageDigest.isEqual` (constant-time)        | Prevents timing attacks on the panic-wipe endpoint.                                                                                                        |
| Key derivation (passphrase notes, planned) | PBKDF2-HMAC-SHA256, 600k iterations            | OWASP-recommended for password-derived keys.                                                                                                               |

All content cryptography is performed by the browser's Web Crypto API. No custom crypto, no third-party crypto libraries, no JavaScript implementations of primitives.

---

## Threat actors

### ✅ Cinder protects against

#### **The honest operator with a hostile server environment**

A well-intentioned operator runs Cinder on infrastructure they don't fully control (cloud provider, shared host, compromised dependency). The operator cannot decrypt content even if they wanted to — the keys never reach the server. A complete server breach yields only ciphertext.

#### **The operator under legal compulsion (content)**

A subpoena demanding "all data for note ID X" can only return the ciphertext + IV + crypto metadata. The operator cannot produce plaintext under any legal order, because the operator has never possessed the decryption key.

#### **Database leaks and accidental exposure**

A misconfigured backup, an exposed S3 bucket, a careless `pg_dump` shared in a Slack channel — none of these reveal user content. The leaked data is ciphertext indexed by random IDs.

#### **The malicious sysadmin / insider threat**

An employee or contractor with database access cannot read content. Cannot infer who created a note. Cannot correlate notes to users via persisted records.

#### **Casual URL leakage**

A URL accidentally shared too broadly is mitigated by:

- **Burn-after-reading** (default on) — first reader destroys the note
- **Short expiration** (default 15 min, max 30 days) — leaked URLs become useless quickly

#### **Network observation**

HTTPS protects the URL (including the fragment) in transit between sender and recipient. Network observers see only encrypted TLS traffic. The fragment never appears in HTTP request lines, server logs, or proxy caches.

#### **Casual scraping and abuse**

The Redis-backed token-bucket rate limiter throttles per-caller request rates without storing identifiable data. Caller buckets are keyed on a hashed-and-salted IP; the salt rotates hourly and lives only in process memory.

---

### ⚠️ Cinder does NOT protect against

#### **A compromised endpoint**

If malware is running on the recipient's device, it can read the note as the user reads it (DOM access, screen capture, keylogger). No client-side cryptography solves this — the plaintext must exist somewhere visible to the user.

**Mitigation:** Standard endpoint hygiene. Out of scope for Cinder.

#### **Operator-deployed malicious code**

Cinder's security depends on the JavaScript served by the operator actually doing what it claims. A malicious or compromised operator could push code that exfiltrates the URL fragment, leaks the decrypted plaintext, or weakens the encryption. Users have no built-in way to verify the JavaScript matches the open-source code.

**Mitigation:** Open-source the codebase (done). Browser extensions like Meta's [Code Verify](https://engineering.fb.com/2022/03/10/security/code-verify/) attempt to address this for messaging apps. Not implemented in Cinder. Users with this concern should self-host.

#### **Coercion of the operator**

A sufficiently motivated adversary (state actor, large corporation) can compel an operator to push code changes, log additional data, or shut down the service. This is the [Lavabit](https://en.wikipedia.org/wiki/Lavabit) problem.

**Mitigation:** Bounded by design (panic-wipe endpoint + warrant canary — see "Operator coercion mitigations" below). Cannot be eliminated entirely.

#### **The recipient turning untrustworthy**

Once a recipient successfully decrypts a note, they have the plaintext. They can save it, screenshot it, share it, or testify about it. Cinder makes no claims about what happens after a successful decryption.

**Mitigation:** None possible. This is a fundamental limit of any "share content with another person" system.

#### **URL leakage via insecure channels**

Sharing a Cinder URL via unencrypted SMS, plaintext email, a public forum, or a hostile messaging app exposes the URL (and therefore the key). Anyone observing that channel can decrypt the note.

**Mitigation:** Share URLs only through end-to-end encrypted channels (Signal, iMessage, encrypted email). The URL is the secret — treat it accordingly.

#### **Targeted state-level adversaries**

Nation-state attackers have access to: zero-day exploits in browsers and operating systems, hardware supply chain manipulation, network-level surveillance, traffic analysis, legal coercion, and physical access to operator infrastructure. Cinder is not engineered to resist this class of threat and does not claim to.

**Mitigation:** Use [Tor](https://www.torproject.org/), [Tails](https://tails.boum.org/), [SecureDrop](https://securedrop.org/), or [Signal](https://signal.org/) — tools purpose-built for this threat model with years of dedicated engineering and audits.

#### **Browser extension or third-party script attacks**

A malicious browser extension can read DOM contents and `window.location.hash`. Cinder's frontend includes no third-party scripts on view pages, but cannot prevent extensions from snooping.

**Mitigation:** Use a clean browser profile when reading sensitive notes. Disable untrusted extensions.

#### **Side-channel attacks**

Timing attacks, cache attacks, and other side channels are not in Cinder's threat model for the cryptographic primitives. The Web Crypto API's resistance to side channels is browser-implementation-dependent. (The admin token comparison does use constant-time equality — see below.)

**Mitigation:** None within Cinder for the crypto layer. Inherent limitation of running in a browser.

---

## Specific design decisions

### Why is the key in the URL fragment instead of the path?

URL fragments (`#...`) are explicitly browser-only per [RFC 3986 § 3.5](https://www.rfc-editor.org/rfc/rfc3986#section-3.5). Browsers strip fragments before sending HTTP requests. Putting the key in the path would expose it in:

- Server access logs
- Proxy and CDN logs
- Browser referrer headers (when navigating away)
- Any analytics that capture URLs

The fragment placement makes accidental leakage to the server architecturally impossible.

### Why is burn-after-reading default-on?

Privacy-first defaults. Users who want a note to persist must consciously opt out. The cost is occasional surprise ("the link only worked once") — we judge this acceptable for a tool whose purpose is ephemerality.

### Why no view counts or per-note analytics?

A view count is metadata the server doesn't need to know to serve the note. Storing it would create a surveillance vector — the operator could observe access patterns. Anomaly-based detection (planned) uses ephemeral Redis counters keyed off rotating hashes, never persisted, never exposed via API.

### Why no user accounts?

Identity is the most powerful surveillance vector. Without accounts:

- The operator cannot link multiple notes to the same person
- There is no password to leak, reset, or social-engineer
- A subpoena for "all notes by user X" cannot be honored — there is no "user X"

The cost is no recovery, no "my notes" dashboard, no per-user features. This is a deliberate trade.

### Why is there no race-protection on burn-after-reading in V1?

Two simultaneous GETs on a burn note can both succeed before the delete completes. This is a known limitation. The atomic delete-with-return-count fix is queued for an upcoming release; only the winning request will receive the content, the other will get HTTP 410. The race window today is on the order of milliseconds and considered acceptable for the threat model.

### How rate limiting protects users without storing PII

Cinder rate-limits API requests to prevent abuse (scraping, DoS, mass note creation). The challenge: rate limiting fundamentally requires the server to recognize "have I seen this caller before?" — which usually means logging IP addresses, which is PII.

Cinder's rate limiter avoids storing IPs at any layer:

1. **The IP is hashed before it ever touches storage.** SHA-256 of `(ip || salt)` produces a 256-bit hex string used as the Redis bucket key. The raw IP is never persisted — not in Redis, not in logs, not in metrics.

2. **A rotating salt makes hashes uncrackable.** SHA-256 alone is reversible by brute force on the IPv4 space (~4 billion possibilities, seconds on a GPU). The salt makes brute-force require knowing the salt. Without it, hashes are mathematically irreversible.

3. **The salt rotates every hour.** After rotation, prior hashes can no longer be matched to current requests. The maximum window in which captured hashes could correlate to known IPs is bounded to one hour.

4. **The salt lives in JVM memory only.** It is never persisted to Redis, disk, or logs. A Redis dump alone yields no useful information — only opaque hashes whose salt is no longer recoverable. An attacker would need to compromise the running JVM process AND capture the salt before it rotates.

5. **Rate-limit buckets auto-expire.** Each bucket has a 1-hour TTL, aligned with the salt rotation interval. Idle callers' buckets are deleted automatically; abandoned IPs do not accumulate forever.

6. **The rate-limit decision is atomic.** The bucket read-modify-write happens as a single Redis-server-side Lua script execution, eliminating race conditions where two simultaneous requests could both consume the last token.

This design provides effective abuse prevention while ensuring that:

- Server logs contain no IPs
- Database contains no IPs
- Even a Redis dump (without the salt) yields nothing useful
- Cross-rotation correlation is impossible
- Memory bloat from inactive callers is naturally bounded

### Operator coercion mitigations

A determined adversary could compel the operator to hand over runtime state during the salt-rotation window. Cinder mitigates this with three layers:

1. **In-memory salt** (no historical record) — only the current salt exists anywhere. Past salts are not recoverable from any storage layer.

2. **Manual panic-wipe endpoint** (`POST /admin/panic-wipe`) — protected by an admin token (`X-Admin-Token` header) loaded from the `CINDER_ADMIN_TOKEN` environment variable. When invoked:
   - The current salt is immediately rotated
   - All Redis rate-limit keys are deleted
   - Any state captured prior to the wipe becomes uncorrelatable with future state

   The endpoint requires the admin token to be set; deployments that omit it have admin operations effectively disabled. The token check uses constant-time comparison (`MessageDigest.isEqual`) to prevent timing attacks. The endpoint is itself rate-limited to defeat brute-force token guessing.

3. **Warrant canary** — see [`CANARY.md`](./CANARY.md). The canary is updated weekly by the operator and affirms that no legal demands have been received. Its absence or staleness signals to users that operator independence may be compromised.

### What this still does NOT protect against

- **Continuous active surveillance** during the salt-rotation window — if an adversary captures both Redis state AND the JVM-resident salt in real time, they can correlate IP→request for that window.
- **Pre-existing surveillance** — if traffic was being captured before suspicion arose, the panic-wipe cannot retroactively erase it.
- **The warrant canary is a signal, not a shield** — it tells future users to be cautious; it does not protect users whose data was already captured.

For threat models that require defense against any of the above, deploy without the built-in rate limiter and use proxy-level rate limiting (nginx, Cloudflare). In that configuration, Cinder itself stores nothing about callers.

---

## Logging

Cinder logs:

- Note creation events (id, burn flag, expiration timestamp)
- Note retrieval events (id only)
- Note expiration deletion counts (aggregate)
- Lookup misses (id only)
- Salt rotation events (timestamp only — NOT the salt itself)
- Rate-limit rejections (hashed key only — never the raw IP)
- Panic-wipe invocations (timestamp + count of keys wiped)
- Application errors (full stack traces, internal only — never returned to clients)

Cinder does NOT log:

- Note content or ciphertext
- IV or salt values
- Raw IP addresses, User-Agent headers, or any request fingerprint
- Request bodies or response bodies
- Anything that could correlate users to specific notes

Note IDs are random nanoids and do not identify users. The combination "user X created note Y" cannot be reconstructed from logs because Cinder never observes "user X."

Production deployments with `application-prod.yml` should disable Hibernate's parameter binding logs (`org.hibernate.orm.jdbc.bind`) — these would log ciphertext values during INSERTs.

---

## Reporting security issues

Found a security flaw? Open an issue on [GitHub](https://github.com/faizagha/cinder/issues) with the label `security`, or email the maintainer directly.

This is a personal/portfolio project, not a production service — there is no bug bounty, no formal disclosure process, and no SLA on fixes. That said, security reports are taken seriously and credited if the reporter wants attribution.

---

## Final note

Threat models exist to be honest about what a tool can and cannot do. Cinder makes a small number of strong claims — server-blind storage, ephemerality by default, no persistent user correlation — and no claim beyond those. If your threat model exceeds Cinder's, use a tool built for your threat model.

The two most important things to remember:

1. **The URL is the secret.** Whoever has the URL can read the note. Lose it = lose the note. Share carelessly = leak the note.
2. **Trust in the operator's deployed code is unavoidable.** Cinder minimizes what the operator CAN do. It cannot eliminate the trust requirement entirely.
