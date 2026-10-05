# Setup guide

## First launch (8 steps)

1. **Welcome** — what SignalFind does.
2. **How it works + consent.** You must tick the explicit consent box:
   > "SignalFind can respond to recovery SMS commands with this device's
   > location. I enable this feature on a device I own or have permission
   > to manage."
   Without consent you cannot continue.
3. **Location.** Grant precise location, then background location
   (separate prompt with rationale — required so SMS commands work while
   the app is closed).
4. **SMS.** Grant `RECEIVE_SMS` + `SEND_SMS` (and notifications on
   Android 13+).
5. **Create PIN.** 4–8 digits; **6+ recommended**. Stored as a salted
   SHA-256 hash in Keystore-encrypted storage — never plaintext, never
   logged.
6. **Add trusted number.** The phone you'd borrow if this one is lost
   (e.g. `+911234567890`). More can be added later.
7. **Test.** Type a command exactly as it would arrive by SMS and
   **Preview reply** (runs the real parser + reply builder, sends nothing),
   or **Send test SMS** for a genuine end-to-end check.
8. **Done.** Lost Mode turns on; a persistent notification confirms
   "SignalFind recovery active".

## Trying it for real

From the trusted phone, with mobile data/Wi-Fi **off** on the lost phone:

```
SIGNALFIND 4829 LOCATION
```

Expect the coordinates by SMS within ~30 seconds (up to ~60 s if the phone
needs a fresh GPS fix).

## Dashboard

- **Status card** — `OFFLINE RECOVERY READY` when Lost Mode is on.
- **Lost Mode switch** — toggles the whole SMS recovery path (and its
  notification).
- **Buttons** — Test recovery, Trusted numbers, Recovery history,
  Diagnostics, Offline map, Settings.

## Settings worth knowing

- **Only trusted numbers** (default ON) — ignore commands from unknown
  senders entirely.
- **Emergency keyword** (default `FINDFONE`) — short alias for LOCATION.
- **Reply SIM** — pick which SIM sends replies on dual-SIM phones
  (needs Phone permission; otherwise the system default is used).
- **Change PIN** any time.
- **Battery optimization** — exempt SignalFind for best reliability on
  aggressive OEM skins.

## Offline map

Enter coordinates from an SMS reply to plot them on a schematic grid.
No internet, no tiles — a visual aid only; the coordinates are the truth.

## After a reboot

Open the app once is **not** required: the SMS receiver is
manifest-declared and works after reboot automatically. `BootReceiver`
re-posts the status notification. (If the app was *force-stopped*, Android
blocks broadcasts until you open it once — an OS rule, not an app bug.)
