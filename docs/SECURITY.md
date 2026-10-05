# Security

## Threat model

SignalFind answers SMS messages with the device's location. The design
treats **every incoming SMS as hostile** until it passes all gates.

### Authentication gates (all must pass)

1. **Shape gate** — the message must match `SIGNALFIND <PIN> <COMMAND>`
   or `<KEYWORD> <PIN>` exactly (case-insensitive, 4–8 digit PIN).
   Anything else is ignored before any auth work.
2. **Trusted-sender gate** (default ON) — the normalized sender must match
   a trusted number (exact or last-10-digits). Unknown senders are ignored
   silently; no reply, no log of their number.
3. **PIN gate** — `MessageDigest.isEqual` (constant-time) against a
   salted SHA-256 digest. The PIN never appears in logs, toasts, or history.
4. **Rate gate** — per-sender: ≥5 s between commands; 5 bad PINs in
   10 minutes → 15-minute silent lockout; success resets the counter.

A wrong PIN from a **trusted** sender gets one helpful
"incorrect PIN" reply (so the owner can fix a typo) — never while locked
out, and never to untrusted senders.

### Storage

- PIN: 16-byte random salt + PBKDF2-HMAC-SHA256 digest (120,000 iterations)
  inside
  **EncryptedSharedPreferences** (AES256-SIV keys, AES256-GCM values),
  master key in the **Android Keystore**. The slow KDF matters because a
  4–8 digit PIN has low entropy — a fast hash would let an attacker with the
  encrypted file brute-force it offline. PINs stored by older versions (fast
  SHA-256) are verified once and transparently re-hashed with PBKDF2.
  Plaintext PIN exists only in memory during entry/verification.
- Trusted numbers + history: local SQLite. History stores **masked**
  senders (`***3210`).
- `allowBackup=false` — encrypted prefs are not backed up (a restore
  would leave Keystore keys behind and a broken state).

### Anti-abuse (by design, not just policy)

- Explicit consent screen before enabling; feature only runs on the
  device whose owner enabled it.
- Persistent "recovery active" notification — the feature is never
  invisible to the device owner. No hidden icon, no stealth mode.
- No `READ_SMS`: unrelated messages are never read, only pattern-checked
  and discarded.
- No contacts, microphone, camera, call recording, or permission bypasses
  anywhere in the codebase.
- Wrong-PIN and malformed-command attempts from senders that pass the early
  gates are recorded (masked) in the local history, so the owner can see
  probing. Stranger noise is never recorded.
- RING is capped at 60 s, raises the alarm stream only for its duration,
  restores volume after, and is stoppable from its notification.
- Location is **never** uploaded anywhere; history never leaves the device.

### Known limits (honest)

- SMS sender IDs can be spoofed by determined attackers via shady
  gateways — the **PIN** is the real authentication, which is why 6–8
  digits are recommended and brute force is rate-limited.
- A 4-digit PIN under sustained attack is weak; the lockout bounds the
  attempt rate (~480/day worst case).
- On Android 12+, a RING command received while the app is fully in the
  background may be refused by the OS background-service-start rule; the
  reply says so instead of pretending.
- If the attacker removes the SIM or the phone is off, no SMS system can
  answer — stated plainly in the UI and TESTING guide.
