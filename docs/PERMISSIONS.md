# Permissions

Every permission SignalFind requests, why it exists, and what happens
without it. Grant status for each is visible in **Diagnostics**.

| Permission | Why | Without it |
|---|---|---|
| `RECEIVE_SMS` | Detect incoming recovery commands (`SMS_RECEIVED`) | Core feature dead — no commands received |
| `SEND_SMS` | Text the location/battery/status replies back | Commands are processed but no reply goes out (history shows FAILED) |
| `ACCESS_FINE_LOCATION` | Precise GPS coordinates for LOCATION | Replies say location unavailable |
| `ACCESS_COARSE_LOCATION` | Fallback when fine is denied | Same as above, less accuracy |
| `ACCESS_BACKGROUND_LOCATION` | SMS arrives while the app is closed; Android 10+ requires this for background location | LOCATION replies report "permission not granted" when the app isn't in the foreground |
| `READ_PHONE_STATE` | List SIM subscriptions for the dual-SIM reply selector | Replies use the system default SIM; selector hidden |
| `POST_NOTIFICATIONS` (13+) | Recovery-active status + alarm notification | Status stays in-app only; alarm still sounds but with no Stop notification (use in-app stop) |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | `RingService` alarm playback | RING command cannot play the alarm |
| `RECEIVE_BOOT_COMPLETED` | Re-post the recovery status after reboot | Status notification missing after reboot until the app is opened (the SMS receiver itself still works — it's manifest-declared) |

## Request strategy (setup wizard)

1. **Foreground location first** (`FINE` + `COARSE` together).
2. **Background location separately afterwards**, with its own rationale
   screen — Android 11+ ignores background location when requested in the
   same call as foreground, and Play policy requires the separation anyway.
3. **SMS pair together** (`RECEIVE_SMS` + `SEND_SMS`).
4. **Notifications** on Android 13+ (recommended, not blocking).
5. **Phone state** is requested lazily from Settings → SIM selector, not
   during setup.

## Not requested (deliberately)

- No `READ_SMS` / `READ_CONTACTS` / call log — the app never reads the
  user's messages or contacts.
- No `RECORD_AUDIO`, no `CAMERA` — there is no surveillance surface at all.
- No `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — the app *asks* the user to
  exempt it via system settings instead of requesting the exemption
  directly (Play-policy-sensitive permission).
- No `SYSTEM_ALERT_WINDOW`, no device-admin, no accessibility service.

## Background-location honesty

Google Play heavily restricts background location; this app is distributed
as a direct APK (sideloaded), where the user explicitly grants it during
setup after reading the rationale. The Diagnostics screen re-verifies the
grant at any time.
