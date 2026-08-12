# Veto

Veto provides the ultimate toolkit to track, lock, record, and recover your lost or stolen Android device across offline SMS, messenger auto-replies, and a secure Vercel-hosted Web Dashboard — without relying on commercial tracking servers.

## Features

Veto offers a robust set of remote commands to control and locate your device in emergency situations. It guarantees absolute privacy and control over your personal data.

**Note on Device Compatibility & Security:**
This app is designed for modern Android devices. Newer devices natively protect the quick settings panel and power-off menu on the lock screen by requiring the device to be unlocked before these actions can be performed. Veto leverages this by not using a separate custom password to prove ownership; instead, it relies entirely on your device's existing lock screen security. Ensure you have a strong, secure PIN or password set on your device.

### Commands

Veto supports a full matrix of commands, accessible via the Web Dashboard, SMS, or Notification Auto-Reply.

*   `veto locate [last|gps]`: Powers on location hardware automatically and fetches accurate GPS/Network/Cell coordinates with Google Maps links.
*   `veto theft`: Master security macro. Activates Theft Mode, fetches GPS coordinates, enables Bluetooth, turns off DND, and blares a 3-minute siren alarm.
*   `veto ring [long|seconds]`: Bypasses DND & silent mode, maxes out alarm stream volume, wakes the screen, and loops a high-decibel alert.
*   `veto lock [custom message]`: Instantly locks the screen using Device Admin API and optionally renders a full-screen owner contact overlay message.
*   `veto camera [front|back] [flash]`: Silently captures a photo using the front or rear camera over lock screens and backs up the file to Google Drive.
*   `veto audio`: Records 30 seconds of high-quality AAC ambient audio from the microphone in the background and uploads it to Google Drive.
*   `veto video [front|back]`: Records 30 seconds of background video from the front or back camera and backs it up to Google Drive. *(BETA)*
*   `veto flash [on|off]`: Toggles the camera flashlight hardware ON/OFF or blinks the torch 10 times for visual location signaling.
*   `veto stats`: Returns device model, Android release, battery %, SIM carrier, SIM phone number, IP addresses, and Wi-Fi SSID scan results.


## Transports

Whether your phone has a high-speed 5G connection or no cellular data at all, Veto remains accessible through four independent communication channels:

1.  **Web Dashboard (`NextJsServerTransport`)**: Real-time command dispatch using Firebase FCM push messaging. Features interactive Google Maps location tracking, live telemetry, and media gallery.
2.  **Offline SMS Control (`SmsTransport`)**: Execute commands remotely without an internet connection. Send SMS text commands (e.g., `VETO LOCATE <PIN>`) and receive immediate SMS replies.
3.  **Notification Interception (`NotificationReplyTransport`)**: Intercepts auto-reply notification intents from ANY installed messaging app (WhatsApp, Telegram, Signal, Matrix) and executes commands.
4.  **In-App Test Sandbox (`InAppTransport`)**: Integrated test environment directly inside the Android application interface to preview command behavior and verify system permissions.

## Privacy & Security

Veto is built on strict data sovereignty principles, ensuring you never lose control of your device or your data.

*   **PBKDF2 & SHA-256 Hashing**: Veto never stores plaintext PINs or passwords. It uses SHA-256 with salt for PIN hashing and PBKDF2-HMAC-SHA256 (100,000 iterations) with context separation to derive E2E encryption keys, preventing GPU dictionary attacks.
*   **Zero Tracking & Zero Ads**: Veto contains zero proprietary analytics SDKs and zero advertisement networks. We do use Firebase Crashlytics to monitor stability, but it strictly only reports which part of the code crashed. It does not expose any of your personal information, it cannot touch your decrypted files, and all your sensitive data remains in encrypted shared preferences.
*   **100% Data Sovereignty**: Retain complete control over your private data. Deploy your own Vercel Web Dashboard and Firebase instance with zero third-party telemetry harvesting.

## Theft Mode Details

The `veto theft` command is designed for critical situations where a device is known to be compromised. When invoked, it:
1. Gathers and transmits precise GPS location data.
2. Overrides device audio settings (disabling Do Not Disturb and silent modes).
3. Maxes out the system alarm volume.
4. Activates a persistent, high-decibel siren alarm to draw attention and deter the thief.
5. Enables Bluetooth (for potential proximity tracking).

This requires the app to be granted a series of high-level permissions during initial setup, including Device Admin, Location, Notification Access, and Audio modification rights, to ensure these actions can be reliably executed regardless of the device's current state.
