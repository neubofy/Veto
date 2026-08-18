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
│                      🌐 WEB DASHBOARD (NextJs)                   │
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
        │  (Kotlin)      │      │  (Twilio)   │   │  Auto-Reply    │
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

📂 **Implementation**: [`NextJsServerTransport`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/in/neubofy/veto/domain/transports)

### 2. 📱 Offline SMS Control (`SmsTransport`)
- Execute commands **without internet connection**
- Send SMS commands: `VETO LOCATE <PIN>`
- Immediate SMS replies with results
- Ultimate fallback mechanism

📂 **Implementation**: [`SmsTransport.kt`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/in/neubofy/veto/domain/transports)

### 3. 💬 Notification Auto-Reply (`NotificationReplyTransport`)
- Intercepts auto-reply intents from ANY messaging app
- Supported: WhatsApp, Telegram, Signal, Matrix, etc.
- Commands via messenger replies: `@bot veto locate`
- Zero additional app requirements

📂 **Implementation**: [`NotificationReplyTransport`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/in/neubofy/veto/domain/transports)

### 4. 🧪 In-App Test Sandbox (`InAppTransport`)
- Integrated test environment inside the app
- Preview command behavior
- Verify system permissions
- Development & debugging tool

📂 **Implementation**: [`InAppTransport.kt`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/in/neubofy/veto/domain/transports)

---

## 🔒 Security & Privacy

### Zero-Trust Architecture
- **PBKDF2 & SHA-256 Hashing**: Never stores plaintext PINs
  - SHA-256 with salt for PIN hashing
  - PBKDF2-HMAC-SHA256 (100,000 iterations) with context separation
- **Zero Tracking & Zero Ads**: No proprietary analytics SDKs
- **100% Data Sovereignty**: Deploy your own Vercel + Firebase instance

📂 **Security Implementation**: [`CryptoModule`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/in/neubofy/veto/domain/crypto)

### Privacy Principles
✅ No Firebase Analytics  
✅ No Advertisement Networks  
✅ Firebase Crashlytics (stability monitoring only)  
✅ Complete control over personal data  

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
│    • Accelerometer shock & movement detection     │
│    • Proximity tracking via Bluetooth             │
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

## 📊 Project Statistics

<div align="center">

| Metric | Value |
|--------|-------|
| 🌟 **GitHub Stars** | ⭐⭐ |
| 👀 **Watchers** | 2 |
| 🍴 **Forks** | 0 |
| 📦 **Repository Size** | 4.4 MB |
| 📝 **Open Issues** | 1 |
| 🔐 **License** | GNU GPLv3 |
| 🏢 **Organization** | [neubofy](https://github.com/neubofy) |

</div>

---

## 🛠️ Technology Stack

<div align="center">

| Component | Technology | Percentage |
|-----------|-----------|------------|
| 📱 Android App | **Kotlin** | 68.8% |
| 🌐 Web Dashboard | **TypeScript + Next.js** | 28.8% |
| 🎨 Styling | **CSS** | 2.3% |
| 📦 Utilities | **JavaScript** | 0.1% |

</div>

### Technology Details

- **Backend**: Vercel (Next.js), Firebase (FCM, Authentication)
- **Mobile**: Kotlin with Android Framework, Room Database
- **API Communication**: REST + Firebase Cloud Messaging
- **Security**: End-to-end encryption, zero-trust architecture
- **Storage**: Google Drive (user-controlled backups)

📂 **Project Structure**:
- [`app/`](https://github.com/neubofy/Veto/tree/main/app) - Android Kotlin application
- [`website/`](https://github.com/neubofy/Veto/tree/main/website) - Next.js web dashboard
- [`docs/`](https://github.com/neubofy/Veto/tree/main) - Documentation & guides

---

## 🔄 Internal Workflow & Data Flow

<div align="center">

```
USER COMMAND (Web/SMS/App)
         │
         ▼
    ┌─────────────────────┐
    │  COMMAND VALIDATION │
    │  & Cryptographic    │
    │  Signature Check    │
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
  │ FCM │  │  SMS   │  │ Notification
  │ Push│  │ Reply  │  │   Reply    │
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
   - Command received via Web/SMS/App
   - Cryptographic signature verification
   - PIN/Token authentication

2. **Transport Routing**
   - Route to appropriate handler based on source
   - Queue management for offline scenarios

3. **Execution**
   - Permission verification
   - Hardware activation (GPS, Camera, Mic, etc.)
   - Data collection & processing

4. **Backup & Response**
   - Media upload to Google Drive (if applicable)
   - Response sent back via same transport
   - Local database update

---

## 📂 Key Source Files & Architecture

### Android Application (Kotlin)

| Component | File | Purpose |
|-----------|------|---------|
| **Main Activity** | [`MainActivity.kt`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/in/neubofy/veto) | App entry point & UI coordination |
| **Transports** | [`domain/transports/`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/in/neubofy/veto/domain/transports) | Communication channel implementations |
| **Commands** | [`domain/commands/`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/in/neubofy/veto/domain/commands) | Command execution logic |
| **Crypto** | [`domain/crypto/`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/in/neubofy/veto/domain/crypto) | Encryption & authentication |
| **Database** | [`data/database/`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/in/neubofy/veto/data/database) | Room database entities |
| **Services** | [`data/services/`](https://github.com/neubofy/Veto/tree/main/app/src/main/java/in/neubofy/veto/data/services) | Android services (FCM, SMS, etc.) |

### Web Dashboard (Next.js + TypeScript)

| Component | Directory | Purpose |
|-----------|-----------|---------|
| **Pages** | [`website/pages/`](https://github.com/neubofy/Veto/tree/main/website/pages) | Next.js page routes |
| **Components** | [`website/components/`](https://github.com/neubofy/Veto/tree/main/website/components) | React UI components |
| **API Routes** | [`website/pages/api/`](https://github.com/neubofy/Veto/tree/main/website/pages/api) | Backend API endpoints |
| **Styles** | [`website/styles/`](https://github.com/neubofy/Veto/tree/main/website/styles) | CSS styling |
| **Utils** | [`website/utils/`](https://github.com/neubofy/Veto/tree/main/website/utils) | Helper functions |

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
   npm run dev
   ```

### Configuration

- Set up Firebase project credentials
- Configure FCM push messaging
- Deploy Web Dashboard to Vercel (recommended)
- Configure SMS gateway (Twilio recommended)

---

## 📜 License

This project is licensed under the **GNU General Public License v3.0** (GPLv3).

[![GPL v3 License](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

**See the [LICENSE](https://github.com/neubofy/Veto/blob/main/LICENSE) file for full details.**

---

## 🙏 Credits & Attribution

This project was heavily inspired by the exceptional open-source work of **[fmd-android](https://gitlab.com/fmd-foss/fmd-android)**. We explicitly credit the `fmd-foss` team for their foundational contributions to open-source Android device recovery solutions.

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
Contributions are welcome! Please follow the [Contributing Guidelines](CONTRIBUTING.md) and submit pull requests.

---

<div align="center">

### Made with ❤️ by [neubofy](https://github.com/neubofy)

**Protecting your Android device, one command at a time.**

![Version](https://img.shields.io/github/package-json/v/neubofy/Veto?label=Version&style=flat-square)
![Last Updated](https://img.shields.io/github/last-commit/neubofy/Veto?label=Last%20Updated&style=flat-square)

</div>
