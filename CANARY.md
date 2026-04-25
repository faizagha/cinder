
# Warrant Canary — Cinder

**Last verified:** 2026-04-24

As of the date above, the Cinder operator (Faiz Agha) has:

- **NOT** received any subpoena, court order, gag order, national security letter, or any other legal demand for user data, source code modification, or operational state.
- **NOT** been compelled to modify the published source code in ways inconsistent with the version visible at [github.com/faizagha/cinder](https://github.com/faizagha/cinder).
- **NOT** been compelled to reveal the rate-limit salt, log additional data, or otherwise weaken the documented privacy posture.
- **NOT** had Cinder's infrastructure (database, Redis, JVM process, deployment environment) accessed by any third party.
- **NOT** signed any agreement or non-disclosure that would prevent updating this canary.

This canary is updated **weekly**. If this canary is missing, has not been updated for more than **14 days**, or has been altered to remove specific affirmations, treat that absence as a signal that operator independence may be compromised. **In that case, do not assume the deployed Cinder instance still matches this threat model.**

---

## Notes for users reading this

A warrant canary cannot prevent surveillance. It can only give users a chance to notice that something has changed. Specifically:

- An operator under legal pressure can be compelled to *act*, but typically cannot be compelled to *lie*. Removing or failing to update this canary is not the same as making a false statement.
- Updates appear in the git history of this repository. Anyone can verify when the canary was last touched.
- The canary covers the operator's commitments. It does not cover infrastructure providers (cloud host, DNS, certificate authority) or the user's own endpoint security.

---

## Verification

To verify this canary is current and unmodified:

```bash
git log --follow CANARY.md
```

The most recent commit's date should be within the past 14 days. The commit content should be a date update only (no removed bullets, no weakened language).

If you observe any of the following, treat as a signal:

- Last commit > 14 days ago
- Bullets removed or softened
- Specific affirmations replaced with vague language
- Repository ownership transferred without prior announcement
- Canary file removed entirely

---

— Faiz Agha
— Operator, [github.com/faizagha/cinder](https://github.com/faizagha/cinder)
— 2026-04-24
