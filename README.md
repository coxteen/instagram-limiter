# Instagram Limiter

A lightweight Android app designed to reduce excessive Instagram scrolling by limiting the number of Reels and Stories a user can view in a session.

When the configured threshold is reached, the app automatically returns the user to the home screen and shows a blocking overlay message encouraging them to take a break.

## Features

- Tracks Instagram Stories viewed in a session
- Tracks Instagram Reels viewed in a session
- Enforces a limit of 3 items before blocking access
- Returns the user to the home screen when the limit is reached
- Displays a full-screen overlay with a reminder message
- Uses Android Accessibility Services to detect Instagram activity

## Project Overview

This project is a small Android application built with Kotlin and the Android Gradle Plugin. It targets Android API 34 and uses:

- Accessibility Service monitoring for Instagram screens
- Overlay permission to show a blocking message
- Event detection for story and reel navigation

## App Behavior

The app monitors for Instagram activity inside the package:

- `com.instagram.android`

When the user opens Instagram and watches Reels or Stories, the service counts them. Once the session reaches the limit:

1. Android returns to the home screen
2. A reminder overlay appears
3. The user must dismiss the overlay manually
4. Counters reset when Instagram is no longer active

## Required Permissions

To work correctly, the app requires:

- Accessibility Service permission
- Display over other apps permission (`SYSTEM_ALERT_WINDOW`)

### Steps to enable

1. Install the app on an Android device
2. Open Android Settings
3. Go to Accessibility
4. Enable the app named `Instagram Limiter`
5. Allow the app to draw over other apps

## Build Instructions

### Prerequisites

- Android Studio
- JDK 17
- Android SDK with API 34
- Gradle wrapper included in the project

### Build the app

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
./gradlew.bat assembleDebug
```

## Run the app

1. Connect an Android device or start an emulator
2. Open the project in Android Studio
3. Select the device/emulator
4. Click Run

## Project Structure

```text
insta-reels-blocker/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/example/instagramlimiter/
│       │   │       └── ReelsBlockerService.kt
│       │   ├── res/
│       │   │   ├── values/
│       │   │   └── xml/
│       │   └── AndroidManifest.xml
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── README.md
```

## Important Notes

- This app depends on Instagram UI elements and view IDs, so it may stop working correctly after Instagram updates redesign the interface.
- Accessibility Services can be sensitive to app changes and Android policy updates.
- This is intended as a personal productivity tool and should be used responsibly.

## License

This project currently does not include a license file. If you are publishing or distributing it, add a suitable open-source license such as MIT or Apache 2.0.

## Disclaimer

This app is intended for self-control and digital wellness use. It is not affiliated with Instagram or Meta.
