# Architecture

## Overview

SignalFind is a single-module Android app (`com.signalfind`) built with
Kotlin, Jetpack Compose (Material 3), and MVVM. The core recovery path is
**SMS in → authenticate → locate → SMS out**. It deliberately avoids every
network dependency: no Firebase, no REST, no WebSockets, no cloud database.
Internet-dependent conveniences (the Google Maps link, the Play-Services
fused provider's network assist) are optional layers on top — the SMS core
works with mobile data and Wi-Fi both off.

```
Recovery phone
      │  SMS: "SIGNALFIND 4829 LOCATION"
      ▼
SmsReceiver (manifest BroadcastReceiver, SMS_RECEIVED)
      │  1. setup complete + recovery enabled?
      │  2. shaped like a command? (cheap pre-filter)
      │  3. sender trusted? (default ON)
      │  4. rate limit / lockout?
      │  5. parses? PIN verifies (constant-time)?
      ▼
SmsResponder ──► LocationProvider ──► FusedLocationProviderClient
      │                                   │  fresh fix (20 s timeout)
      │                                   └──► last-known (marked CACHED w/ age)
      │                                            └──► Unavailable (honest reason)
      ▼
SmsSender ──► SmsManager (preferred SIM or default) ──► multipart SMS
      │
      ▼
HistoryRepository (SQLite, sender masked) ──► Recovery History screen
```

## Module map

| Area | Key files | Notes |
|---|---|---|
| Entry | `MainActivity.kt`, `SignalFindApp.kt` | `SignalFindApp` owns a manual DI container (`AppContainer`); no DI framework |
| SMS in | `sms/SmsReceiver.kt` | `goAsync()` + coroutine; fail-fast, fail-silent ordering |
| Parser | `sms/CommandParser.kt` | Pure Kotlin, unit-tested; strict shapes only |
| SMS out | `sms/SmsSender.kt`, `sms/SmsResponder.kt` | Multipart; dual-SIM via `getSmsManagerForSubscriptionId` |
| Location | `location/LocationProvider.kt` | Fresh ≤60 s = LIVE; older = CACHED with age; else Unavailable |
| Secrets | `security/CryptoManager.kt` | EncryptedSharedPreferences (Keystore); PIN = salt + SHA-256, `MessageDigest.isEqual` |
| Abuse | `security/RateLimiter.kt` | Per-sender: 5 s min interval, 5 fails/10 min → 15 min lockout |
| Storage | `data/db/DatabaseHelper.kt` | Plain `SQLiteOpenHelper` (no annotation processing); trusted numbers + history |
| Settings | `data/SettingsRepository.kt` | Encrypted prefs; PIN/trusted-only/keyword/SIM choice |
| Device | `system/` | `BatteryInfo`, `SimInfo` (SubscriptionManager), `NotificationHelper`, `PermissionHelper` |
| Alarm | `alarm/RingService.kt` | Foreground service, alarm stream at max, 60 s cap, Stop action |
| Boot | `boot/BootReceiver.kt` | Re-posts the recovery status notification; receiver itself is manifest-declared |
| UI | `ui/screens/`, `ui/viewmodel/`, `ui/nav/Routes.kt` | Compose Navigation; one ViewModel per screen |

## Key decisions

- **SQLiteOpenHelper instead of Room.** The spec allows Room "if needed";
  plain SQLite removes the annotation-processing step, keeping the build
  reproducible with both Gradle and a plain kotlinc/aapt2/d8 pipeline.
- **Fused provider + LocationManager fallback.** `getCurrentLocation(
  PRIORITY_HIGH_ACCURACY)` with a 20 s timeout; `lastLocation` and
  `LocationManager.getLastKnownLocation` as cached fallbacks (also covers
  devices without Play Services).
- **Background location permission is required**, because SMS arrives while
  the app is closed. The setup wizard requests foreground first, then
  background separately (Android 11+ rule), with a plain-language rationale.
- **RING from background may be refused** on Android 12+ (`startForegroundService`
  background-start restriction). The app catches it and replies honestly
  instead of pretending.
- **No hidden behavior, ever.** Recovery mode shows a persistent low-priority
  notification; the alarm shows a high-priority one with a Stop action; the
  launcher icon is never hidden.
- **History stores masked senders** (`***3210`), never full numbers.

## Data flow details

- Incoming SMS parts are reassembled with
  `Telephony.Sms.Intents.getMessagesFromIntent`.
- The sender is normalized (`PhoneNumberUtils.normalize`) and matched
  against trusted numbers (exact or last-10-digits).
- Replies are built as plain text with coordinates always present, then
  split with `SmsManager.divideMessage` when over one segment.
- Every handled command is recorded locally (command, masked sender,
  LIVE/CACHED/NONE, accuracy, reply SENT/FAILED). Nothing is uploaded.

## Battery

Nothing polls. The receiver is event-driven; GPS is requested only for an
authenticated LOCATION command, a manual test, or the offline-map preview.
The persistent notification is static (no updates). See `docs/TESTING.md`
for the background-restriction cases.
