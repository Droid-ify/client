# 🔧 Building Droid-ify from Source

This guide will walk you through the process of building Droid-ify from source code.

## 📋 Prerequisites

Before you begin, ensure you have the following tools installed:

### Required Software
- **Android Studio** Arctic Fox (2020.3.1) or newer
  - Download from [developer.android.com](https://developer.android.com/studio)
- **JDK 17** or higher
  - We recommend using the JDK bundled with Android Studio
- **Android SDK** with API level 35
  - Will be installed automatically with Android Studio
- **Git** for version control
  - Download from [git-scm.com](https://git-scm.com/)

### System Requirements
- **RAM**: 8 GB minimum, 16 GB recommended
- **Storage**: 10 GB free space for Android Studio + dependencies
- **OS**: Windows 10+, macOS 10.14+, or Linux (64-bit)

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/Iamlooker/Droid-ify.git
cd Droid-ify
```

### 2. Open in Android Studio
1. Launch Android Studio
2. Select **"Open an existing project"**
3. Navigate to the cloned `Droid-ify` directory
4. Click **"OK"** and wait for the project to sync

### 3. Build the Project
```bash
# Sync dependencies and build
./gradlew build
```

## 📦 Build Variants

Droid-ify supports multiple build configurations:

### Debug Build
Includes debugging information and additional logging:
```bash
./gradlew assembleDebug
```
**Output**: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build
Optimized production build (requires signing):
```bash
./gradlew assembleRelease
```
**Output**: `app/build/outputs/apk/release/app-release.apk`

## 🏗️ Detailed Build Process

### Step 1: Environment Setup
1. **Install Android Studio** with default settings
2. **Open SDK Manager** (Tools → SDK Manager)
3. Install **Android SDK Platform 35** if not already installed
4. Install **Android SDK Build-Tools 35.0.0**

### Step 2: Project Configuration
1. Clone the repository to your local machine
2. Create a `local.properties` file if it doesn't exist:
   ```properties
   sdk.dir=/path/to/your/android/sdk
   ```

### Step 3: Dependency Resolution
```bash
# Download and resolve all dependencies
./gradlew --refresh-dependencies
```

### Step 4: Build Execution
```bash
# Clean previous builds
./gradlew clean

# Build debug version
./gradlew assembleDebug

# Or build release version
./gradlew assembleRelease
```

## 📱 Installation & Testing

### Install Debug Build
```bash
# Install debug APK directly to connected device
./gradlew assembleDebug
```

### Install via ADB
```bash
# Manual installation using ADB
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🛠️ Development Setup

### Code Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use Android Studio's default code formatting (Ctrl+Alt+L)
- Configure auto-import for optimal imports

### Recommended Plugins
- **Kotlin** (built-in with Android Studio)
- **Detekt** (when implemented) - for static analysis

### Debug Configuration
1. Enable **USB Debugging** on your Android device
2. Connect device via USB
3. Select your device in Android Studio's device dropdown
4. Click the **Run** button (▶️) to install and launch

## 🐛 Troubleshooting

### Common Issues

#### Build Fails with "SDK not found"
**Solution**: Ensure `local.properties` contains the correct SDK path:
```properties
sdk.dir=/Users/[username]/Library/Android/sdk  # macOS
sdk.dir=C\:\\Users\\[username]\\AppData\\Local\\Android\\Sdk  # Windows
sdk.dir=/home/[username]/Android/Sdk  # Linux
```

#### "Could not resolve dependencies"
**Solution**: Check your internet connection and try:
```bash
./gradlew clean --refresh-dependencies
```

#### OutOfMemoryError during build
**Solution**: Increase Gradle heap size in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
```

#### Device not detected
**Solution**:
1. Enable Developer Options on your device
2. Enable USB Debugging
3. Install appropriate USB drivers (Windows)
4. Run `adb devices` to verify connection

### Getting Help
- Check [GitHub Issues](https://github.com/Iamlooker/Droid-ify/issues) for known problems
- Search [Android Developer Documentation](https://developer.android.com/docs)
- Ask questions in [GitHub Discussions](https://github.com/Iamlooker/Droid-ify/discussions)

## 🏗️ Architecture Overview

Understanding the project structure will help you navigate the codebase:

```
app/src/main/kotlin/com/looker/droidify/
├── data/           # Data layer (repositories, database)
├── di/             # Dependency injection modules
├── domain/         # Business logic and models
├── installer/      # App installation management
├── network/        # Network operations and downloading
├── service/        # Background services
├── sync/           # Repository synchronization
├── ui/             # User interface components
└── utility/        # Helper classes and extensions
```

### Key Components
- **Database**: Room-based local storage
- **Network**: Ktor HTTP client for downloads
- **DI**: Hilt for dependency injection
- **UI**: Jetpack Compose (planned) / View system
- **Background Work**: WorkManager and Services

## 📚 Additional Resources

- [Android Developer Guide](https://developer.android.com/guide)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Material Design Guidelines](https://material.io/design)
- [F-Droid Documentation](https://f-droid.org/docs/)

---

*For more information about contributing to Droid-ify, see [CONTRIBUTING.md](../CONTRIBUTING.md)*
