# Building Droid-ify

Simple guide to build Droid-ify from source.

## Prerequisites

- **Android Studio** (latest version)
- **JDK 17+** (bundled with Android Studio)
- **Git**

## Quick Build

```bash
# Clone the repository
git clone https://github.com/Iamlooker/Droid-ify.git
cd Droid-ify

# Build debug APK
./gradlew assembleDebug

# Or build release APK
./gradlew assembleRelease
```

**Output**: `app/build/outputs/apk/`

## Android Studio

1. Open Android Studio
2. **File** → **Open** → Select `Droid-ify` folder
3. Wait for sync to complete
4. **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**

## Install & Test

```bash
# Install to connected device
./gradlew installDebug

# Or manually install
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Common Issues

**Build fails**: Try `./gradlew clean --refresh-dependencies`

**SDK not found**: Create `local.properties`:
```properties
sdk.dir=/path/to/your/android/sdk
```

**Device not detected**: Enable USB Debugging in Developer Options

---

*For contributing guidelines, see [CONTRIBUTING.md](../CONTRIBUTING.md)*
