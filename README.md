# ♟️ Chess Clock for Android

A modern, professional, and customizable **Android Chess Clock** app designed for over-the-board chess games. Built natively with Java and Material Design 3, it offers an intuitive head-to-head interface, high-visibility themes for any playing environment, and precise timing.

---

## ✨ Features

- **🔄 Head-to-Head Rotated Layout:** Top player clock is rotated 180° so two players sitting opposite each other can comfortably view and tap their clock.
- **☀️ Light & 🌙 Dark Mode:** Switch between Dark Mode and High-Visibility Light Mode with white player cards for maximum readability in outdoor or bright lighting conditions.
- **⏱️ Standard Time Control Presets:**
  - **Bullet:** `1 | 0`, `1 | 1`, `2 | 1`
  - **Blitz:** `3 | 0`, `3 | 2`, `5 | 0`, `5 | 3`
  - **Rapid:** `10 | 0`, `15 | 10`
  - **Classical:** `30 | 0`
- **➕ Fischer Increment Support:** Automatically adds bonus seconds per move (e.g., +1s, +2s, +3s, +10s).
- **⚡ High-Precision Scramble Timer:** Displays tenths-of-a-second (`0:0s.ms`) when remaining time falls below 10 seconds.
- **📊 Move Counter:** Tracks total moves played by each player automatically.
- **🔔 Audio & Haptic Feedback:** Plays crisp beep tones and subtle vibration feedback on clock taps and time flag alerts.
- **🔒 Keep Screen Awake:** Prevents the device screen from turning off during a match.
- **⏸️ Pause, Resume & Safe Reset:** Easily pause and resume matches, with an alert confirmation dialog to prevent accidental resets during gameplay.

---

## 🛠️ Tech Stack & Requirements

- **Language:** Java 11
- **Target SDK:** Android 17 / API 37
- **Min SDK:** Android 8.1 / API 27
- **UI Components:** Material Design 3 (`com.google.android.material`), AndroidX ConstraintLayout, Vector Drawables
- **Build System:** Gradle (Kotlin DSL `build.gradle.kts`)

---

## 🚀 Getting Started

### Prerequisites
- [Android Studio Ladybug | 2024.2.1+](https://developer.android.com/studio) or newer
- JDK 11 or higher
- Android SDK 37

### Building & Running

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/IsaacFungulwe/ChessClock.git
   cd ChessClock
   ```

2. **Build Debug APK via Gradle:**
   ```bash
   ./gradlew :app:assembleDebug
   ```

3. **Run Unit Tests:**
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

---

## 📁 Project Structure

```text
ChessClock/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/chessclock/
│   │   │   │   └── MainActivity.java       # Main Game Loop, Timers & Controls
│   │   │   ├── res/
│   │   │   │   ├── drawable/              # Custom Icons & Badges
│   │   │   │   ├── layout/
│   │   │   │   │   └── activity_main.xml   # Dual Rotated Cards & Control Bar
│   │   │   │   └── values/
│   │   │   │       ├── colors.xml          # Light & Dark Mode Color Palettes
│   │   │   │       ├── strings.xml         # String Resources
│   │   │   │       └── themes.xml          # Material3 Application Theme
│   │   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
