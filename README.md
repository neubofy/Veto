<div align="center">

# 🛡️ Veto

**Advanced Android Remote Management & Anti-Theft Application**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/neubofy/Veto.svg)](https://github.com/neubofy/Veto)
[![Language: Kotlin](https://img.shields.io/badge/Kotlin-68.8%25-7F52FF.svg)](https://kotlinlang.org)
[![Language: TypeScript](https://img.shields.io/badge/TypeScript-28.8%25-3178C6.svg)](https://www.typescriptlang.org)
[![Website](https://img.shields.io/badge/Website-veto.neubofy.in-green.svg)](https://veto.neubofy.in)

</div>

---

## 📱 Overview

Veto is a **completely free**, advanced Android remote management and anti-theft application that provides the ultimate toolkit to **locate, lock, record, and recover** your lost or stolen Android device.

<div align="center">

### 🚀 Key Differentiator
Unlike traditional third-party tracking apps, Veto does **not** rely on continuous background workers, eliminating battery drain and performance issues—it remains **entirely dormant** until a command is received.

</div>

---

## 🎯 Core Features

<div align="center">

| Feature | Description | Transport |
|---------|-------------|-----------|
| 📍 **Locate** | Powers on GPS automatically, fetches accurate coordinates | Web/SMS |
| 🚨 **Theft Mode** | Master security macro: GPS + Siren + Bluetooth + Audio capture | Web/SMS |
| 📞 **Ring** | Bypasses DND, maxes volume, wakes screen with siren | Web/SMS |
| 🔒 **Lock** | Instant screen lock with custom owner message | Web/SMS |
| 📷 **Photo** | Silent front/rear camera capture over lock screens | Web Dashboard |
| 🎙️ **Audio** | 30s background audio recording to Google Drive | Web/SMS |
| 🎥 **Video** | 30s background video capture (BETA) | Web Dashboard |
| 💡 **Flash** | Toggle flashlight or blink 10x for visual location | Web/SMS |
| 📊 **Stats** | Device info: model, battery, carrier, IPs, WiFi scan | Web/SMS |

</div>

---

## 🏗️ Architecture Overview

<div align="center">

```
┌─────────────────────────────────────────────────────────────────┐
│                      🌐 WEB DASHBOARD (Next.js)                  │
│              Hosted on Vercel with Firebase FCM Integration      │
├─────────────────────────────────────────────────────────────────┤
│  • Real-time command dispatch                                    │
│  • Interactive Google Maps tracking                              │
│  • Live telemetry & media gallery                                │
│  • 100% self-hosted option available                             │
└────────────┬─────────────────────────────────────────────────────┘
             │
    ┌────────┴────────┐
    │                 │
    ▼                 ▼
┌─────────┐    ┌────────────────┐
│ Firebase │    │  Google Drive  │
│   FCM    │    │  (Backups)     │
└─────────┘    └────────────────┘
    │
    └─────────────────┬──────────────────────┬──────────────┐
                      │                      │              │
        ┌─────────────▼──┐      ┌───────────▼─┐   ┌────────▼──────┐
        │  Android App   │      │ SMS Gateway │   │  Notification │
        │  (Kotlin)      │      │  (Native)   │   │  Auto-Reply    │
        │                │      │             │   │  (WhatsApp,    │
        │ • Device Admin │      │ • Offline   │   │   Telegram,    │
        │ • Location API │      │   Control   │   │   Signal)      │
        │ • Camera/Mic   │      │ • No Data   │   │                │
        │ • Sensors      │      │   Required  │   │ • Command      │
        │ • Audio Stream │      │             │   │   Interception │
        └────────────────┘      └─────────────┘   └────────────────┘

                    🔐 ZERO-TRUST CRYPTOGRAPHY 🔐
                   (End-to-End Encrypted Communication)
```

</div>

---

## 🚚 Communication Transports

Veto remains accessible through **four independent communication channels**, ensuring connectivity regardless of network conditions:

### 1. 🌐 Web Dashboard (`NextJsServerTransport`)
- **Real-time** command dispatch via Firebase FCM push messaging
- Interactive Google Maps location tracking
- Live telemetry & media gallery
- Premium user experience with full feature set

📂 **Implementation**: [`NextJsServerTransport.kt`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/com/neubofy/veto/transports)

### 2. 📱 Offline SMS Control (`SmsTransport`)
- Execute commands **without internet connection**
- Send SMS commands: `veto <PIN> locate`
- Immediate SMS replies with results
- Ultimate fallback mechanism

📂 **Implementation**: [`SmsTransport.kt`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/com/neubofy/veto/transports)

### 3. 💬 Notification Auto-Reply (`NotificationReplyTransport`)
- Intercepts auto-reply intents from ANY messaging app
- Supported: WhatsApp, Telegram, Signal, Matrix, etc.
- Commands via messenger replies: `veto locate`
- Zero additional app requirements

📂 **Implementation**: [`NotificationReplyTransport.kt`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/com/neubofy/veto/transports)

### 4. 🧪 In-App Test Sandbox (`InAppTransport`)
- Integrated test environment inside the app
- Preview command behavior
- Verify system permissions
- Development & debugging tool

📂 **Implementation**: [`InAppTransport.kt`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/com/neubofy/veto/transports)

---

## 🔒 Security & Privacy

### Zero-Trust Architecture
- **AES-GCM Encryption**: All FCM commands are encrypted with your PIN and user ID before being sent.
- **PBKDF2 & SHA-256 Hashing**: Never stores plaintext PINs
  - SHA-256 with salt for PIN hashing
  - PBKDF2-HMAC-SHA256 (100,000 iterations) with context separation for key derivation
- **Zero Tracking & Zero Ads**: No proprietary analytics SDKs
- **100% Data Sovereignty**: Deploy your own Vercel + Firebase instance

📂 **Security Implementation**: [`VetoCrypto.kt`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/com/neubofy/veto/utils)

### Privacy Principles
✅ No Third-Party Analytics
✅ No Advertisement Networks  
✅ Complete control over personal data  
✅ Media is only sent to your linked Google Drive

---

## 💥 Theft Mode - The Ultimate Security Macro

The `veto theft` command is designed for **critical emergency situations**:

```
┌───────────────────────────────────────────────────┐
│         🚨 VETO THEFT COMMAND ACTIVATED 🚨         │
├───────────────────────────────────────────────────┤
│ 1. 🔊 UNSTOPPABLE SIREN                           │
│    • Overrides system audio settings              │
│    • Bypasses Do Not Disturb & silent modes       │
│    • Maxes alarm stream volume                    │
│    • 3-minute high-decibel alert loop             │
│                                                   │
│ 2. 🔌 HARDWARE ACTIVATION                         │
│    • Enables GPS & Bluetooth silently             │
│    • Disables DND, resets volume to 100%          │
│    • Ensures max connectivity for tracking        │
│                                                   │
│ 3. 📡 DATA CAPTURE                                │
│    • Gathers precise GPS location                 │
│    • Silent background photo capture              │
│    • Ambient audio recording (30s)                │
│    • Immediate cloud sync to Google Drive         │
│                                                   │
│ 4. 🔌 OFFLINE CAPABILITY                          │
│    • Trigger via SMS from emergency contact       │
│    • Works even if data/WiFi disabled             │
│    • Complete autonomy without internet           │
│                                                   │
│ ⚠️ REQUIRES: Device Admin, Location,              │
│    Notification Access, Audio & Mic Permissions   │
└───────────────────────────────────────────────────┘
```

---
## 🛠️ Technology Stack

<div align="center">

| Component | Technology | Percentage |
|-----------|-----------|------------|
| 📱 Android App | **Kotlin / Jetpack Compose** | 68.8% |
| 🌐 Web Dashboard | **TypeScript + Next.js** | 28.8% |
| 🎨 Styling | **CSS** | 2.3% |
| 📦 Utilities | **JavaScript** | 0.1% |

</div>

### Technology Details

- **Backend**: Vercel (Next.js), Firebase (FCM, Authentication, Firestore)
- **Mobile**: Kotlin 2.2.x, Jetpack Compose, Material 3, Room Database, WorkManager
- **API Communication**: REST + Firebase Cloud Messaging
- **Security**: End-to-end encryption, zero-trust architecture
- **Storage**: Google Drive (user-controlled media backups)

📂 **Project Structure**:
- [`app/`](https://github.com/neubofy/Veto/tree/main/app) - Android Kotlin application (`com.neubofy.veto`)
- [`website/`](https://github.com/neubofy/Veto/tree/main/website) - Next.js web dashboard (`/app` directory)

---

## 🔄 Internal Workflow & Data Flow

<div align="center">

```
USER COMMAND (Web/SMS/App)
         │
         ▼
    ┌─────────────────────┐
    │  COMMAND VALIDATION │
    │  & AES Decryption   │
    │  (If from Web)      │
    └──────────┬──────────┘
               │
         ┌─────▼──────┐
         │  TRANSPORT  │
         │   ROUTER    │
         └─────┬───────┘
               │
      ┌────────┼────────────┐
      │        │            │
      ▼        ▼            ▼
  ┌─────┐  ┌────────┐  ┌───────────┐
  │ FCM │  │ Native │  │ Notification
  │ Push│  │  SMS   │  │   Reply    │
  └──┬──┘  └────┬───┘  └──────┬────┘
     │          │             │
     └──────────┼─────────────┘
                │
         ┌──────▼──────────┐
         │  COMMAND HANDLER│
         │  (Kotlin Layer) │
         └──────┬──────────┘
                │
      ┌─────────┼──────────┐
      │         │          │
      ▼         ▼          ▼
   📍GPS    🔊AUDIO    📷CAMERA
   📊STATS  🔒LOCK     💡FLASH
      │         │          │
      └─────────┼──────────┘
                │
         ┌──────▼──────────┐
         │ GOOGLE DRIVE    │
         │   BACKUP        │
         └─────────────────┘
```

</div>

### Command Processing Pipeline

1. **Reception & Validation**
   - Command received via Web (FCM), SMS, or Notification Reply.
   - For Web Dashboard, the command is end-to-end encrypted with AES-GCM and must be decrypted using the user's PIN.
   - For SMS, the sender's number is verified against the Allowlist or requires the PIN.

2. **Transport Routing**
   - Commands are handled by `CommandExecutionWorker` using Android's `WorkManager`.

3. **Execution**
   - Permissions are checked.
   - Command logic runs (e.g., getting GPS, taking a photo).

4. **Backup & Response**
   - Media (photos, audio) is uploaded directly to the user's Google Drive.
   - Response text is sent back via the originating transport (Firestore for Web, SMS reply for SMS).

---

## 📂 Key Source Files & Architecture

### Android Application (`com.neubofy.veto`)

| Component | Directory / File | Purpose |
|-----------|------------------|---------|
| **UI** | [`app/src/main/java/com/neubofy/veto/ui/`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/com/neubofy/veto/ui) | Jetpack Compose screens and Activities |
| **Transports** | [`app/src/main/java/com/neubofy/veto/transports/`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/com/neubofy/veto/transports) | Communication channel implementations (FCM, SMS) |
| **Commands** | [`app/src/main/java/com/neubofy/veto/commands/`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/com/neubofy/veto/commands) | Implementations of Locate, Lock, Ring, etc. |
| **Data/Crypto** | [`app/src/main/java/com/neubofy/veto/utils/VetoCrypto.kt`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/com/neubofy/veto/utils) | AES-GCM encryption & PBKDF2 key derivation |
| **Workers** | [`app/src/main/java/com/neubofy/veto/workers/`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/com/neubofy/veto/workers) | WorkManager tasks (CommandExecutionWorker) |
| **Services** | [`app/src/main/java/com/neubofy/veto/services/`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/com/neubofy/veto/services) | FirebaseMessagingService for push notifications |

### Web Dashboard (Next.js App Router)

| Component | Directory / File | Purpose |
|-----------|------------------|---------|
| **Pages** | [`website/app/`](https://github.com/neubofy/Veto/tree/main/website/app) | Next.js App Router pages (Dashboard, Console, Login) |
| **Components** | [`website/components/`](https://github.com/neubofy/Veto/tree/main/website/components) | React UI components (e.g., CommandConsole, PinGateModal) |
| **API Routes** | [`website/app/api/`](https://github.com/neubofy/Veto/tree/main/website/app/api) | Backend endpoints (Firebase Admin commands) |
| **Crypto** | [`website/lib/clientCrypto.ts`](https://github.com/neubofy/Veto/tree/main/website/lib) | Web-side AES-GCM encryption/decryption |

---

## 🚀 Getting Started

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/neubofy/Veto.git
   cd Veto
   ```

2. **Android Setup**
   ```bash
   cd app
   # Build with Android Studio or Gradle
   ./gradlew build
   ```

3. **Web Dashboard Setup**
   ```bash
   cd website
   npm install
   # Start the development server (in the background, e.g., using 'npm start &')
   ```

### Configuration

- Create a Firebase project and add Android and Web apps.
- Add `google-services.json` to the Android `app/` directory.
- Configure Firebase Authentication (Google Sign-In) and Firestore.
- Deploy the Web Dashboard to Vercel (recommended) and set the Firebase Admin environment variables.

---

## 📜 License

This project is licensed under the **GNU General Public License v3.0** (GPLv3).

[![GPL v3 License](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

**See the [LICENSE](https://github.com/neubofy/Veto/blob/main/LICENSE) file for full details.**

---

## 🙏 Credits & Attribution

This project draws inspiration from the exceptional open-source work of **[fmd-android](https://gitlab.com/fmd-foss/fmd-android)**. We explicitly credit the `fmd-foss` team for their foundational contributions to open-source Android device recovery solutions.

---

## 🔗 Links & Resources

| Resource | Link |
|----------|------|
| 🌐 **Official Website** | [veto.neubofy.in](https://veto.neubofy.in) |
| 📊 **GitHub Repository** | [neubofy/Veto](https://github.com/neubofy/Veto) |
| 🏢 **Organization** | [neubofy on GitHub](https://github.com/neubofy) |
| 📧 **Report Issues** | [GitHub Issues](https://github.com/neubofy/Veto/issues) |
| 💬 **Discussions** | [GitHub Discussions](https://github.com/neubofy/Veto/discussions) |

---

## 📞 Support & Contributing

### Report Issues
Found a bug? Open an issue at [GitHub Issues](https://github.com/neubofy/Veto/issues)

### Contribute
Contributions are welcome! Please submit pull requests or open issues to discuss proposed changes.

---

<div align="center">

### Made with ❤️ by [neubofy](https://github.com/neubofy)

**Protecting your Android device, one command at a time.**

![Version](https://img.shields.io/github/package-json/v/neubofy/Veto?label=Version&style=flat-square)
![Last Updated](https://img.shields.io/github/last-commit/neubofy/Veto?label=Last%20Updated&style=flat-square)

</div>
