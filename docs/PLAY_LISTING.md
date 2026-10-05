# Play Store listing prep — SignalFind

> **Honest verdict first.** Google Play's
> [SMS permissions policy](https://support.google.com/googleplay/android-developer/answer/10208820)
> limits `RECEIVE_SMS`/`SEND_SMS` to default SMS handlers and a short list of
> eligible exceptions (e.g. financial SMS transactions). A lost-phone finder
> driven by SMS commands is **not** among the published eligible use cases —
> comparable apps have been rejected with "Ineligible use case". Expect a
> Permissions Declaration rejection if you submit this build to Play.
>
> Additionally, `ACCESS_BACKGROUND_LOCATION` requires a prominent in-app
> disclosure, a feature demonstration video, and manual review.
>
> **Realistic distribution:** direct APK (sideload) from your own site/GitHub
> releases, as documented in `docs/APK_BUILD.md`. Everything below is prepared
> anyway, so the listing work is done if policy ever changes or you ship a
> Play-compliant variant.

## Listing text

**Title:** SignalFind — Offline SMS Phone Finder

**Short description (80 chars):**
Find your lost phone by SMS. No internet needed. Your data stays on-device.

**Full description:**
SignalFind locates your own Android phone through authenticated SMS — even
with mobile data and Wi-Fi turned off.

From any trusted phone, text: SIGNALFIND <PIN> LOCATION. Your lost phone
verifies the sender and PIN, takes a GPS fix, and texts the coordinates back.
Also supports BATTERY, RING (loud 60-second alarm), and STATUS commands, plus
a short FINDFONE alias.

• Works fully offline — SMS only, no accounts, no cloud, no Firebase
• Honest location reporting: LIVE, CACHED (with age), or clearly unavailable
• PIN + trusted-number authentication, rate limiting, and lockout protection
• Keystore-encrypted storage; recovery history stays on your device
• Visible, consent-based operation — no stealth, no tracking, no ads

Note: background location and SMS permissions are required for the core
feature and are requested with plain-language explanations during setup.

## Data safety form (answers)

| Question | Answer |
|---|---|
| Does the app collect or share user data? | **Yes — Location only, as described below.** Nothing else. |
| Location — collected? | Yes: transmitted **by SMS to phone numbers the user explicitly designates as trusted**, and only in response to an authenticated command the user sends. Purpose: App functionality. |
| Location — shared with third parties? | No. (The carrier transports the SMS, as with any text message.) |
| Is collected data ephemeral? | Replies are transient. Coordinates in local history stay on-device and are user-deletable. |
| Contacts, SMS content, identifiers, other types? | No. Unrelated SMS are ignored, never stored. |
| Data encrypted in transit? | SMS replies use the carrier's SMS transport (not internet TLS). No internet transmission exists. |
| Data deletion | In-app: history wipe; uninstall removes everything. |

## Content rating

No user-generated content, no ads, no social features — expected **Everyone**.

## Release checklist

- [ ] Release keystore created, backed up offline, **never committed**
- [ ] `signingConfigs.release` wired in `app/build.gradle.kts` (template included, commented)
- [ ] `./gradlew assembleRelease` → smoke test install on a real device
- [ ] Full 17-case matrix in `docs/TESTING.md` run on hardware
- [ ] Privacy policy URL live (publish `docs/PRIVACY_POLICY.md` with contact filled in)
- [ ] Permissions Declaration Form submitted with a screen-recorded demo video
- [ ] Background-location prominent disclosure video recorded
- [ ] Data safety form filled per the table above
- [ ] Feature graphic + screenshots (phone + 7" / 10" tablet sets)

## Category

Tools (or Personalization-adjacent). Not designed for the "Safety" categories
that carry extra review.
