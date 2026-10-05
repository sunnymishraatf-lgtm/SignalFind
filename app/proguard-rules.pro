# SignalFind ProGuard / R8 rules.
#
# Conservative: the whole app package is kept (the app is small and uses
# reflection-free code), libraries keep what they need via their own rules.
# Verified entry points (manifest-declared) are listed explicitly so a
# future aggressive shrink can't break them.

-keep class com.signalfind.** { *; }

# AndroidX Security Crypto (Keystore / EncryptedSharedPreferences)
-keep class androidx.security.crypto.** { *; }

# Play Services Location (fused provider)
-keep class com.google.android.gms.location.** { *; }

# SQLiteOpenHelper schema is string-based; keep the DB helper anyway.
-keepclassmembers class com.signalfind.data.db.DatabaseHelper { *; }

# BroadcastReceivers / Services referenced from AndroidManifest.xml
-keep class com.signalfind.sms.SmsReceiver { *; }
-keep class com.signalfind.boot.BootReceiver { *; }
-keep class com.signalfind.alarm.RingService { *; }
-keep class com.signalfind.MainActivity { *; }
-keep class com.signalfind.SignalFindApp { *; }
