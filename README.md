# SignalFind — Offline SMS Phone Finder

SignalFind locates your own lost Android phone **without internet**.
The owner texts an authenticated SMS command from any trusted phone;
the lost phone verifies the sender and PIN, takes a GPS fix, and texts
the coordinates back. No accounts, no cloud, no Firebase, no data needed.

```
Recovery phone  --SMS-->  Lost phone  --SMS-->  Recovery phone
"SIGNALFIND 4829 LOCATION"      "Lat: 20.296059, Lng: 85.824539 …"
```

Example reply:

```
📍 SignalFind Location
Lat: 20.296059
Lng: 85.824539
Accuracy: ±12m
Updated: 7:42 PM
Battery: 41%
Source: GPS (live)
Map: https://maps.google.com/?q=20.296059,85.824539
```

The coordinates are always plain text, so they stay useful on a receiving
phone with no internet (the Maps link is a convenience on top).

> **Scope.** SignalFind is strictly for locating devices you own or that
> you are explicitly permitted to manage. It has no hidden tracking, no
> stealth mode, no microphone/camera activation, and never bypasses Android
> permissions. See [SECURITY.md](docs/SECURITY.md).

## Commands

Sent by SMS from a trusted number:

| SMS text | Action |
|---|---|
| `SIGNALFIND <PIN> LOCATION` | Best available location (live / cached / honest "unavailable") |
| `SIGNALFIND <PIN> BATTERY` | Battery level + charging state |
| `SIGNALFIND <PIN> RING` | Loud 60-second recovery alarm (visible notification, stoppable) |
| `SIGNALFIND <PIN> STATUS` | Battery, location/SIM state, time |
| `FINDFONE <PIN>` | Short alias for `LOCATION` (keyword is configurable) |

Every command requires the correct PIN **and** (by default) a trusted sender
number. Wrong PINs are rate-limited and eventually locked out silently.

## Project layout

```
signalfind/
├── app/                          # Android Studio module
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/signalfind/
│       │   ├── MainActivity.kt
│       │   ├── SignalFindApp.kt          # manual DI container
│       │   ├── sms/          # SmsReceiver, CommandParser, SmsResponder, SmsSender
│       │   ├── location/     # LocationProvider (fused, offline-capable)
│       │   ├── security/     # CryptoManager (Keystore), RateLimiter
│       │   ├── data/         # SettingsRepository, SQLite storage, repos
│       │   ├── system/       # BatteryInfo, SimInfo, NotificationHelper, PermissionHelper
│       │   ├── alarm/        # RingService (foreground alarm)
│       │   ├── boot/         # BootReceiver
│       │   ├── util/         # PhoneNumberUtils, TimeFormat
│       │   └── ui/           # Compose screens + ViewModels (MVVM)
│       └── res/
└── docs/
    ├── ARCHITECTURE.md
    ├── PERMISSIONS.md
    ├── SETUP.md
    ├── SECURITY.md
    ├── TESTING.md
    └── APK_BUILD.md
```

## Tech

- Kotlin · Jetpack Compose (Material 3) · MVVM · Coroutines/Flow
- Fused Location Provider (works offline via GNSS) + LocationManager fallback
- SmsManager / SMS_RECEIVED broadcast · SubscriptionManager (dual SIM)
- EncryptedSharedPreferences + Android Keystore (PIN as salted SHA-256, constant-time verify)
- SQLite (trusted numbers, local recovery history — never leaves the device)
- minSdk 26 · targetSdk 34 · compileSdk 34

## Build

**Android Studio (recommended):** open the `signalfind/` folder, let Gradle
sync, then Run. See [docs/APK_BUILD.md](docs/APK_BUILD.md).

**Command line:** the same Gradle project builds with `./gradlew assembleDebug`
once the Android SDK is installed.

A prebuilt debug APK is at `SignalFind-debug.apk` (see your files).

## Docs

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — components, data flow, decisions
- [docs/PERMISSIONS.md](docs/PERMISSIONS.md) — every permission and why it exists
- [docs/SETUP.md](docs/SETUP.md) — first-launch wizard walkthrough
- [docs/SECURITY.md](docs/SECURITY.md) — threat model and mitigations
- [docs/TESTING.md](docs/TESTING.md) — the 17 test cases and how to run them
- [docs/APK_BUILD.md](docs/APK_BUILD.md) — building and signing the APK
- [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md) — privacy policy (publish before any store listing)
- [docs/PLAY_LISTING.md](docs/PLAY_LISTING.md) — store listing text, data safety answers, and the honest policy verdict
