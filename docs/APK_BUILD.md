# Building and signing the APK

## Option A — Android Studio (recommended)

1. Open the `signalfind/` folder in Android Studio (Hedgehog or newer).
2. Let Gradle sync (needs internet on first run for dependencies).
3. **Build → Build APK(s)** → `app/build/outputs/apk/debug/app-debug.apk`.
4. For a release APK: **Build → Generate Signed Bundle / APK**, create or
   reuse a keystore.

Requirements: JDK 17, Android SDK with platform 34 + build-tools 34.

## Option B — command line (Gradle)

```bash
cd signalfind
./gradlew assembleDebug        # needs ANDROID_HOME / ANDROID_SDK_ROOT set
```

First run downloads the Gradle distribution and all dependencies (~1 GB).

## Option C — the manual pipeline (no Gradle)

The project was also built with a plain toolchain, proving there is no
Gradle magic: `kotlinc` + Compose compiler plugin → `aapt2 compile/link` →
`d8` → `zipalign` → `apksigner`. Notes from that build, if you reproduce it:

- Kotlin 1.9.24 with the Compose compiler plugin (compiler-hosted jar,
  Compose 1.5.14 plugin, UI 1.6.8 / Material3 1.2.1).
- Room was replaced with plain `SQLiteOpenHelper`, so no kapt/KSP step.
- Resource linking needs the transitive AAR resources merged (appcompat,
  material3, activity, lifecycle, savedstate, emoji2, profileinstaller,
  startup, plus constraintlayout); missing legacy color aliases
  (`ripple_material_light`, `secondary_text_default_material_light`)
  are referenced only inside legacy support-library resources and can be
  ignored if they fail to resolve.
- Deduplicate `annotation`/`annotation-jvm` and `collection`/`collection-jvm`
  jars before `d8` (duplicate class definitions fail the build).
- `d8` from build-tools 36 avoids an internal NPE that 34.0.0 hits on
  `lifecycle-livedata-core`.
- Two dex files are produced (method count > 64K); fine on minSdk 26,
  which handles multidex natively.

## Signing

Debug builds use the standard debug key. **For any APK you actually
install and keep**, sign with your own release key and keep the keystore
backed up — Android pins updates to the signing certificate.

## Distribution note

Google Play heavily restricts `RECEIVE_SMS`/`SEND_SMS` (and background
location) to policy-eligible use cases. Distribute SignalFind as a direct
APK (sideload) with the user explicitly granting permissions during setup,
as documented in `docs/PERMISSIONS.md`.
