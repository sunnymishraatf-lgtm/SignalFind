# Testing guide

## Unit tests (run on any JVM)

The parser and phone-number logic are pure Kotlin and tested without a
device. 27 assertions cover: all four commands, case-insensitivity,
whitespace, the `FINDFONE` alias (incl. rejection of trailing tokens),
PIN shape validation, unrelated-message rejection, newline injection,
`looksLike` pre-filter (incl. the `SIGNALFINDER` prefix trap), number
normalization, trusted matching, masking, and plausibility checks.

They were run against the real source files during development and all
passed. Re-run any time with a local kotlinc:

```bash
kotlinc -d out \
  app/src/main/java/com/signalfind/sms/CommandParser.kt \
  app/src/main/java/com/signalfind/util/PhoneNumberUtils.kt \
  <your-test-file>.kt
java -cp out:<kotlin-stdlib.jar> <TestMain>
```

## On-device matrix (real phone + real SIM required)

Nothing below was or can be simulated here — run it on hardware before
trusting the app. Expected outcome for every row is in parentheses.

1. **Internet on, GPS on** — `SIGNALFIND <PIN> LOCATION` → LIVE reply ≤30 s.
2. **Internet off (data + Wi-Fi), GPS on** — the headline case: LIVE reply
   by SMS. GNSS needs no data.
3. **Wi-Fi and data off, SMS only** — same as 2; reply is SMS regardless.
4. **Location off** — reply: "Location unavailable… location is turned off".
5. **Airplane mode** — no reply at all (no radio). Documented limitation.
6. **No SIM** — no reply; commands can't arrive.
7. **Weak GPS** — first fix may take ~60 s; reply still arrives (timeout is
   20 s + cached fallback, so expect a CACHED reply with age, then a fresh
   one on retry).
8. **Indoors** — likely CACHED reply (last-known) with age; honest.
9. **Dual SIM** — set Reply SIM; confirm replies arrive from that SIM.
10. **Wrong PIN** — trusted sender gets one "incorrect PIN" SMS; app log
    shows no PIN.
11. **Untrusted sender** — total silence. No reply, nothing in history.
12. **5 rapid wrong PINs** — 15-minute lockout; even the correct PIN gets no
    reply during lockout; `Diagnostics`/history reflect it.
13. **Reboot** — status notification reappears; commands still work without
    opening the app.
14. **Low battery** — BATTERY/STATUS replies report the true level.
15. **Background restrictions** — with battery optimization ON, LOCATION may
    be slow or fall back to cached; exempt the app if needed.
16. **Cached-only** — after GPS off + moving, reply is labeled CACHED with
    the fix age; never presented as live.
17. **SMS send failure** (e.g. no credit) — history records reply FAILED;
    sender sees nothing (carrier-side, nothing the app can do).

## Diagnostics screen

Run it before the matrix: it checks setup completion, Lost Mode,
permissions (incl. background location), location services on/off,
SIM/subscription state, notification channel, and battery-optimization
exemption in one pass.

## What "verified" means here

The parser unit tests passed on JVM. Everything in the 17-case matrix
needs a physical device and is **not** claimed as done — run it and log
results before relying on the app.
