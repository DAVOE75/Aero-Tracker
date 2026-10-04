# Aero Tracker — CdA Estimator for Karoo 2/3

> **Virtual wind tunnel for your Hammerhead Karoo 2/3**
> Calculate your Aerodynamic Drag Coefficient (CdA) in real-time using power, speed, and gradient data.

*Read this in other languages: [Español](README.md)*

---

## What is Aero Tracker?

Aero Tracker is an extension for the Hammerhead Karoo 2 and Karoo 3 cycling computers that estimates your **CdA (Coefficient of Drag Area)** in real-time during your ride, without the need for a formal wind tunnel session.

### Physics behind the calculation

```
P_total = P_aero + P_rolling + P_gravity

P_aero    = 0.5 × ρ × CdA × (v + v_wind)² × v
P_rolling = Crr × m × g × v
P_gravity = m × g × grad × v

⟹ CdA = (P - P_rolling - P_gravity) / (0.5 × ρ × v_rel² × v)
```

Where:
- **ρ** = air density (adjusted by altitude)
- **v** = rider's speed in m/s
- **m** = total mass (rider + bike) in kg
- **Crr** = rolling resistance coefficient (~0.004 for road)

---

## Available data fields on Karoo

| Field | ID | Description | Unit |
|---|---|---|---|
| Live CdA | `aerotracker-cda` | CdA calculated every second | m² |
| Smoothed CdA | `aerotracker-cda-smooth` | 5-second moving average | m² |
| Watts Saved | `aerotracker-watts-saved` | Watts saved vs baseline posture (CdA 0.32) | W |
| Aero Power | `aerotracker-power-aero` | Power spent overcoming the wind | W |
| Aero Posture | `aerotracker-category` | Descriptive category | — |

---

## CdA Categories

| CdA | Category | Description |
|---|---|---|
| < 0.20 | 🟢 Super Aero | Extreme TT position (very low head) |
| 0.20–0.25 | 🟢 Aero | TT position or aggressive drops |
| 0.25–0.32 | 🟡 Efficient | Comfortable drops or semi-aero |
| 0.32–0.40 | 🟠 Upright | Sitting upright, hands on tops |
| > 0.40 | 🔴 Very upright | Hands on hoods, very vertical torso |

---

## Requirements

- **Device**: Hammerhead Karoo 2 or Karoo 3
- **Required sensors**:
  - ✅ Power Meter — **mandatory**
  - ✅ GPS Speed or speed sensor — **mandatory**
  - 📡 Cadence sensor — optional (improves accuracy)
- **Optional sensors**:
  - Barometer (for precise gradient — already integrated in Karoo)

---

## Installation

### Method 1: Hammerhead Extension Library (recommended)
1. Create an account at [dashboard.hammerhead.io](https://dashboard.hammerhead.io)
2. Search for "Aero Tracker" in the Karoo Extension Library
3. Install and restart the device

### Method 2: Sideload (testing)
```bash
# Compile APK
./gradlew assembleRelease

# Install via ADB (Karoo connected via USB)
adb install app/build/outputs/apk/release/app-release.apk
```

---

## Configuration

### Default Parameters
```
Rider mass:     75 kg
Bike mass:      8 kg
Crr (rolling):  0.004 (wet tarmac = 0.006, dry tarmac = 0.003)
Baseline CdA:   0.32 (normal upright posture)
Smoothing:      5 seconds
```

### Usage Recommendations

1. **Flat with no wind**: The most reliable results are obtained on flat segments.
2. **Minimum speed**: The calculation requires >10 km/h and an active power meter.
3. **Testing postures**: Hold each position for 30+ seconds to see the effect.
4. **Improvement reference**: The "Watts saved" field compares against a baseline CdA of 0.32.

---

## Project Structure

```
app/src/main/kotlin/com/aerotracker/karoo/
├── MainActivity.kt                    # Config/simulator screen
├── engine/
│   └── CdaCalculator.kt              # CdA physics engine
├── extension/
│   └── AeroTrackerExtension.kt       # Karoo extension service
├── model/
│   └── AeroModels.kt                 # Data models
└── ui/
    ├── AeroGaugeView.kt              # Custom needle gauge
    └── theme/
        └── AeroTrackerTheme.kt       # Dark theme
```

---

## Roadmap

- [ ] **v1.1**: Integration with OpenWeatherMap API (real-time wind data)
- [x] **v1.2**: Custom Karoo screen with "wind tunnel" style needle gauge
- [ ] **v1.3**: Posture comparison mode (save sessions to compare)
- [ ] **v1.4**: Export CdA data to Strava/Garmin Connect
- [ ] **v2.0**: Automatic calibration with reference session

---

## 📦 Compilation for Developers

The project uses Gradle and Kotlin. Requires JDK 17 and Android SDK (API Level 34).

```bash
./gradlew assembleDebug # Compiles the test APK
./gradlew testDebugUnitTest # Runs the math tests
```

---

## 🤝 Credits and Acknowledgements

- Built on top of the official Hammerhead [karoo-ext](https://github.com/hammerheadnav/karoo-ext) SDK (Apache 2.0 License).
- Inspired by the Karoo open-source modding community (like the legendary *Ki2* or *Climber+* extensions).
- Developed by **David García Pascual**.

📄 **License and Disclaimer**

This open-source project is distributed under the **MIT** license - Copyright 2026 David García Pascual. *Disclaimer: This extension is not affiliated with, endorsed by, sponsored by, or supported by Hammerhead or SRAM. Use it at your own risk and please always keep your eyes on the road and your hands on the handlebars.*

---

## ☕ Support the project

If you found this extension useful and want to support its continuous development:

<a href="https://www.buymeacoffee.com/" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;" ></a>