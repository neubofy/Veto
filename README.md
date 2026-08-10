# Veto

Veto is an advanced Android remote management and anti-theft application that provides the ultimate toolkit to track, lock, record, and recover your lost or stolen Android device. It operates across offline SMS, messenger auto-replies, and a secure Vercel-hosted Web Dashboard — providing unmatched multi-transport resilience without relying on commercial tracking servers.

## Features

Veto offers a robust set of remote commands to control and locate your device in emergency situations. It utilizes zero-trust cryptography to guarantee absolute privacy and control over your personal data.

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
*   `veto delete <password> [dryrun]`: Constant-time password-verified emergency factory reset with a 3-second transmission buffer to deliver confirmation. **(DANGER)**

## Transports

Whether your phone has a high-speed 5G connection or no cellular data at all, Veto remains accessible through four independent communication channels:

1.  **Web Dashboard (`NextJsServerTransport`)**: Real-time command dispatch using Firebase FCM push messaging. Features interactive Google Maps location tracking, live telemetry, and media gallery.
2.  **Offline SMS Control (`SmsTransport`)**: Execute commands remotely without an internet connection. Send SMS text commands (e.g., `VETO LOCATE <PIN>`) and receive immediate SMS replies.
3.  **Notification Interception (`NotificationReplyTransport`)**: Intercepts auto-reply notification intents from ANY installed messaging app (WhatsApp, Telegram, Signal, Matrix) and executes commands.
4.  **In-App Test Sandbox (`InAppTransport`)**: Integrated test environment directly inside the Android application interface to preview command behavior and verify system permissions.

## Privacy & Security

Veto is built on zero-trust cryptography and strict data sovereignty principles, ensuring you never lose control of your device or your data.

*   **Argon2id Password & PIN Hashing**: Veto never stores plaintext PINs or passwords. It uses Argon2id (iterations = 1, memory = 128MB, parallelism = 4) with context separation to hash security PINs and prevent GPU dictionary attacks.
*   **AES-256-GCM & RSA-3072 Encryption**: All media payloads and telemetry data are symmetrically encrypted using AES-256-GCM (96-bit random IV, 128-bit auth tag) and asymmetric key wrapping with RSA-3072 OAEP (SHA-256 MGF1).
*   **Zero Tracking & Zero Ads**: Veto contains zero proprietary analytics SDKs, zero crash-reporting telemetry services, and zero advertisement networks. All log entries remain stored strictly on your local device storage.
*   **100% Data Sovereignty**: Retain complete control over your private data. Deploy your own Vercel Web Dashboard and Firebase instance with zero third-party telemetry harvesting.

## Theft Mode Details

The `veto theft` command is the ultimate, unique security macro designed for critical situations. What makes it completely unique compared to standard device managers is its aggressive, multi-layered approach to deterrence and recovery:
1. **Unstoppable Siren**: Instantly overrides all system audio settings, bypasses Do Not Disturb and silent modes, maxes out the alarm volume, and blares a persistent high-decibel siren.
2. **Visual Deterrence & Fake UI**: Forces the device screen on and locks it into a "RadarScanView" that simulates connecting to a mesh network, while showing fake and real terminal logs to intimidate the thief and obscure system access.
3. **Hardware Activation**: Silently enables Bluetooth (for proximity tracking), turns on the flashlight to draw visual attention, and powers on location hardware.
4. **Data Capture**: Gathers and transmits precise GPS location data, while silently capturing background photos and ambient audio, immediately syncing them to your secure cloud storage.
5. **Offline Capability**: The entire sequence can be triggered offline via SMS from a trusted emergency contact, ensuring your device reacts even if the thief disables cellular data or Wi-Fi.


This requires the app to be granted a series of high-level permissions during initial setup, including Device Admin, Location, Notification Access, and Audio modification rights, to ensure these actions can be reliably executed regardless of the device's current state.

## Credits
This project was heavily inspired by the exceptional open-source work of [fmd-android](https://gitlab.com/fmd-foss/fmd-android). We would like to explicitly credit the `fmd-foss` team for their foundational ideas and approaches to Android remote management and anti-theft security.

## License
This project is licensed under the GNU General Public License v3.0 (GPLv3). See the [LICENSE](LICENSE) file for more details.
