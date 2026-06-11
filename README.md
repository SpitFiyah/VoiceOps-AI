# VoiceOPS AI - Pure Standalone Android App

A **Voice-First Business Ledger Operating System** designed for high-efficiency operation of street vendors, micro-merchants, and farmers. Instead of typing in complicated apps, users simply toggle region settings, speak naturally in English, Hindi, Hinglish, or their local language script (Tamil, Telugu, Marathi, Kannada, Bengali), and watch VoiceOps instantly structure their ledger book with digital precision.

---

## 🚀 Key Achievements & Features

1. **Perfect Multilingual Voice Translation & Natural Speech**:
   - **Local Script Synchronization**: Switching the language (e.g., to **Hindi**) and speaking a command like *"Ramesh ne do kilo aloo le liya"* forces the AI core to translate, format, and construct the confirmation spoken feedback exclusively in local **Devanagari script** (e.g., `"रमेश ने 2.0 kg आलू खरीद लिया है।"`).
   - **Smooth, Warm, Natural TTS Voice**: The Android Text-to-Speech engine pitch and speed have been finely tuned to a warmer, natural tone (`pitch = 0.96f` and slow-paced `speechRate = 0.82f`) instead of the rushed, screechy defaults.
   - **Pristine Cleanup**: All formatting markdown like asterisks `*`, symbols, or backticks are parsed out prior to sound rendering to guarantee smooth pronunciation and natural delivery out loud.

2. **No Complex Multi-Project Clutter**:
   - This repository is structured purely as a **native Jetpack Compose Android Application** (located under `/app`) containing local Room DB storage, `VoiceOpsViewModel`, and direct `GeminiClient` API bindings, paired with an elegant web-sandbox simulator in root for early prototyping in the browser.

3. **Continuous Local Simulation & Fallback**:
   - For offline use or testing, a custom multilingual simulation matrix matches the language dynamically, ensuring realistic voice responses in the selected regional script.

---

## 📱 How to Access and Install the App on a Phone

### Method A: Download Directly from GitHub (Automatic Release Artifacts)
We have pre-configured a continuous packaging pipeline using **GitHub Actions**. Ready-to-install debug Android APKs are compiled automatically for you or any user!

1. Select **Settings** (Gear Icon) in the top-right corner of the Google AI Studio workbench.
2. Select **Export to GitHub** or **Download as ZIP**.
   - If exported to GitHub, the automated workflow file `.github/workflows/android.yml` immediately triggers a cloud build.
3. On your repository page, click on the **Actions** tab.
4. Click on the latest run matching *"Build Android App"*.
5. Under the **Artifacts** section at the bottom of the page, click on **VoiceOps-AI-Debug-APK** to download the compiled `.apk` file directly to your computer or phone!
6. Copy the `.apk` file to your Android phone, tap to open, allow installation from unknown sources, and start executing!

---

## 🛠️ Build and Deploy Manually (Android Studio)

If you prefer building and extending the Kotlin sources manually:

1. **Import the Project**:
   - Open **Android Studio** (Brave Giraffe or newer recommended).
   - Click **File > Open**, select the root directory containing the `build.gradle.kts` file.

2. **Configure API Secrets**:
   - The app reads your API key securely from localized Gradle build configs (`BuildConfig.GEMINI_API_KEY`).
   - Define your key in your system environment or standard Gradle configurations on your machine.

3. **Install on Phone**:
   - On your Android phone, go to **Settings** > **About Phone** > Tap **Build Number** 7 times to unlock *Developer Options*.
   - Enable **USB Debugging** in developer settings.
   - Plug your phone into your computer via a USB cable.
   - In Android Studio, select your physical phone in the device selection dropdown on the top toolbar.
   - Click the green **Run (Play button)** or press `Shift + F10` to compile, package, install, and launch the application directly on your phone.

---

## 💻 Developer Command-Line Quick Build Guide

Configure, test, or package modules directly via your local CLI:

### 🌐 Web Prototype Sandbox
Assemble or serve the simulated browser application:
```bash
# Install required dependencies
npm install

# Audit and lint source quality
npm run lint

# Build the production React build (outputs ready assets in dist/)
npm run build

# Run local Vite development hot-sever (port 3000)
npm run dev
```

### 🤖 Native Android Build via Gradle
Compile or sign the physical Android application packages:
```bash
# Grant execution rights to Gradle build shell
chmod +x gradlew

# Assemble fully ready Debug APK for fast manual installs
./gradlew assembleDebug
```

---

## ⚙️ Technical Architecture (The Pure Android Stack)

- **UI Framework**: Modern declarative **Jetpack Compose** with Material Design 3 coordinates theme flows.
- **Database Engine**: **Room Database** caches high-speed offline ledgers, mandi commodity prices, transactions, inventory stocks, and sync states.
- **Natural Language Parsing**: **Google Gemini API** (using `gemini-3.5-flash` for high-speed, cost-effective inference) receives speech-to-text strings and returns zero-hallucination JSON transaction schemas.
- **Synthesizer Engine**: Android **TextToSpeech** with high-purity regional voice loaders and custom rhythm controllers.
