# Privacy Policy — SignalFind

**Effective date:** October 2026

SignalFind ("the app") is an offline phone-recovery tool. It has no user
accounts, no servers, no analytics, no advertising, and no third-party code
that transmits your data anywhere.

## What the app does with your information

- **Location.** When you (or someone using one of your trusted recovery
  numbers) send an authenticated SMS command (`SIGNALFIND <PIN> LOCATION`),
  the app obtains the phone's location using Android's on-device location
  services and sends the coordinates back **by SMS to that trusted number**.
  Coordinates are never uploaded to the internet, a cloud service, or the
  developer. If you never send a command, no location is ever obtained.
- **SMS.** The app reads incoming SMS messages only to detect messages
  shaped like recovery commands (`SIGNALFIND <PIN> …`). All other messages
  are ignored and never stored. Replies are sent via your carrier's SMS
  service to your trusted numbers.
- **On-device storage.** Your PIN (as a salted PBKDF2 hash, never plaintext),
  trusted numbers, settings, and recovery history are stored only on your
  phone, in Android Keystore-encrypted storage and a local database. History
  entries store sender numbers masked (last 4 digits only). Nothing is
  backed up off-device (`allowBackup=false`).
- **No network.** The app makes zero internet requests. There is nothing to
  intercept, and the app cannot "phone home".

## Permissions

The app requests SMS, location (including background location), phone-state
(for dual-SIM reply selection), notifications, and boot-completed permissions
solely to implement the SMS recovery feature described above. See
`docs/PERMISSIONS.md` for a permission-by-permission explanation.

## Your control

- Recovery can be switched off at any time (Dashboard → Lost Mode); when off,
  no SMS is ever answered.
- Recovery history can be deleted per-entry or wiped entirely.
- Uninstalling the app deletes all local data.

## Contact

[INSERT CONTACT EMAIL] — replace this placeholder before publishing.
