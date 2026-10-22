# WatUFound

An Android lost-and-found app for the University of Waterloo community. Report a lost item, share something you found, and connect with other students to help belongings find their way home.

## Demo

[![Watch the WatUFound demo on YouTube](https://img.youtube.com/vi/Sd_eUdJ0rkM/hqdefault.jpg)](https://youtu.be/Sd_eUdJ0rkM)

**[Watch the demo on YouTube](https://youtu.be/Sd_eUdJ0rkM)**

## Features

- **Lost and found posts:** Create item reports with descriptions, categories, photos, campus locations, and dates.
- **Search and filters:** Browse lost or found items and filter by category or date range; search titles, descriptions, and locations.
- **Campus maps:** Select locations using OpenStreetMap and campus building data.
- **Messaging:** Contact other users through real-time conversations.
- **Accounts and profiles:** Sign up, log in, and manage your profile with Firebase Authentication.
- **Post management:** Track and resolve item reports.
- **Administration:** Dashboard and user management tools for administrators.

## Built with

Kotlin, Android XML layouts, Material Components, View Binding, Android Jetpack ViewModels and LiveData, Navigation Component, Firebase Authentication, Cloud Firestore, Firebase Realtime Database, Firebase Storage, osmdroid, and Glide.

The app uses an MVVM structure to separate screens, presentation logic, and data models.

## Getting started

1. Clone this repository and open it in Android Studio with support for Android Gradle Plugin 8.11.1.
2. Install Android SDK 36 and use JDK 17 to run Gradle.
3. Create your own Firebase project and register an Android app with package name `com.ece452.watufound`.
4. Download its `google-services.json` and place it in `app/`, replacing the existing project configuration.
5. Enable Email/Password Authentication, Cloud Firestore, Realtime Database, and Storage. Configure appropriate database and storage access rules for your project; this repository does not supply a complete Firebase backend deployment.
6. Sync Gradle and run the app on an emulator or device running Android 7.0 (API 24) or later.

To build a debug APK from the terminal:

```sh
./gradlew assembleDebug
```

## Project structure

- `app/src/main/java/com/ece452/watufound/models/` — item, user, and message models.
- `app/src/main/java/com/ece452/watufound/ui/` — screens, ViewModels, and adapters.
- `app/src/main/java/com/ece452/watufound/utils/` — category, location, and time helpers.
- `app/src/main/res/` — layouts, navigation, and visual resources.
- `app/src/main/assets/Buildings.csv` — campus building data.

See the [system design document](docs/SYSTEM_DESIGN.md) for the original architecture documentation.

## Team and origin

Developed as a University of Waterloo ECE 452 team project. This repository is a personal portfolio copy of the [original team repository on Waterloo GitLab](https://git.uwaterloo.ca/f35syed/ece452-watufound).

Contributors recorded in the Git history: Fasih Noor, Faizan Syed, Azizul Chowdhury, Leo Qu, Mahir Rathi (`m2rathi`), and Yash Sethi. The original commit history credits individual contributions.
