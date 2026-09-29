# الرسائل — Risala

A glass-styled LSPosed/Xposed module that enhances **Google Messages**
(`com.google.android.apps.messaging`) with privacy and power-user features on a
rooted Android device.

Package: `ro.buroot.risala` · min SDK 29 (Android 10+) · target SDK 35
Tested against One UI 7 (Samsung) with Magisk + LSPosed.

---

## Features

All features are **off by default** and toggled from the glass control panel.
The module stays inert until you enable something.

### Privacy & Security
- **Hidden vault** — move conversations into an AES-encrypted store gated behind
  biometric unlock. The key lives in the Android Keystore.
- **Block apps from reading SMS** — denies other apps access to the SMS content
  provider (allowlist supported); denied apps just see "no messages".
- **Silent-SMS detection** — detects stealth type-0 tracking messages that
  Android normally drops silently, and alerts you.
- **SMS access log** — records which apps read or received your messages.

### Messaging
- **Auto-copy verification codes** — OTP codes land on the clipboard instantly.
- **Auto-delete codes** — OTP messages are removed after N minutes.
- **Separate banks & promotions** — sorts service/OTP/promo traffic into tabs
  (local, heuristic; never deletes or reroutes).
- **Custom number labels** — attach your own readable name to any number. The
  label is shown **in addition to** the real number, never replacing it.
- **Reliable scheduled send** — send-later via a system alarm that survives app
  kills and reboots.
- **Reply from notification** — reply / mark read / delete without opening the app.

### System & SIM
- **Radio-level block** — drop messages/calls from your listed numbers before
  they are logged, inside the telephony stack.
- **Per-conversation SIM** — pin each thread to a SIM and remember it.
- **Message centre (SMSC)** — view/set the SMSC per SIM.
- **SIM / IMEI change watch** — alerts when SIM or device identity changes
  (stored locally as a hash; nothing is transmitted).

### Backup & Export
- **Full backup & restore** — complete SMS/MMS backup as restorable JSON.
- **Readable export** — export conversations to HTML / JSON / PDF.
- **Recover deleted messages** — best-effort scan of the raw database free pages
  for recoverable rows (no guarantee).

---

## Intentionally NOT included

Two ideas were dropped on purpose because their main real-world use is
fabricating fake conversations (forged screenshots shown to others):

- Rewriting a received/sent message's **timestamp** in the database.
- **Spoofing the displayed sender** to an arbitrary number.

The **Custom number labels** feature covers the legitimate need (organising
service numbers) while always keeping the true origin visible.

---

## Build

APKs are built via **GitHub Actions** — no local Android Studio needed.

1. Push this project to a GitHub repo.
2. The workflow in `.github/workflows/build.yml` runs on push (or trigger it
   manually from the Actions tab).
3. Download `risala-apks` from the run's artifacts. Use the debug APK for
   testing; sign the release APK before distributing.

Locally (if you have the SDK):

```bash
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

---

## Install

1. Install the APK.
2. In **LSPosed → Modules**, enable *الرسائل* and tick the scope:
   **Google Messages**, **System Framework**, **Phone/Telephony**.
3. Reboot (or force-stop Google Messages).
4. Open *الرسائل*, enable the features you want.

> Some features hook system components (silent-SMS, radio block, SMS-read
> blocking). These vary by ROM/Android version; if one doesn't take effect,
> check the LSPosed log — the module logs each hook under the `[Risala]` tag and
> never crashes the host on failure.

---

## Notes on how it works
- Settings are shared with the hook process via `XSharedPreferences`; enable
  "New XSharedPreferences" for the module in LSPosed if your build needs it.
- Data features (vault, backup, export, OTP wipe, scheduling) operate on the
  standard Telephony provider using the app's own SMS permissions.
- Nothing is sent off-device.
