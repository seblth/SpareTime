# 📱 SpareTime App

SpareTime is an Android app that helps users limit and manage their screen time.  
The app sets restrictions for selected applications to encourage more mindful smartphone usage.

---

## 🚀 Features

- ⏱ **Per-App Limits**  
  Set daily time limits for each app or use slot-based logic for flexible usage.

- 🔒 **Overlay Lock Screen**  
  Once the limit is reached, an overlay blocks access to the app.

- 🔔 **Notifications & Alerts**  
  Users are notified when remaining time is about to run out.

- 📊 **Usage Overview**  
  Track your screen time for locked apps.

- ➕ **Extend Time**  
  Users can extend their limits if needed.

- 🎨 **Simple UI**  
  Intuitive interface for quick setup and management.

---

## 🛠️ Tech Stack

- **Language:** Kotlin / Java  
- **Build System:** Gradle  
- **CI/CD:** GitHub Actions (Lint, Unit Tests, Build APK)  
- **Architecture:** MVVM (Model-View-ViewModel)  
- **Minimum SDK:** 24  

---

## 📦 Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/seblth/SpareTime.git
   cd SpareTime
2. Build the debug APK:
     ./gradlew assembleDebug
3. Install the APK (e.g., via ADB):
     adb install app/build/outputs/apk/debug/app-debug.apk

---

## 🤖 CI/CD Pipeline

This project uses GitHub Actions for automated builds and testing:
Lint & Unit Tests → Runs static code analysis and unit tests
Build Debug APK → Produces an installable debug version of the app
These checks run automatically on every push and pull request targeting the dev and main branches.

---

## 👥 Team

This project was developed as part of the Project Practicum (4th semester Computer Science, Provadis University of Applied Sciences).
Team roles:
👨‍💻 Developers: Alexander Savkov, Dennix Böx
🧪 Testers: Nurzhan Kukeyev
📚 Pipeline & Documentation: Sebastian Leithoff, Dennis Böx
