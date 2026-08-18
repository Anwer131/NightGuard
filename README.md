# NightGuard

NightGuard is an Android app that helps enforce a configurable
Night Mode by blocking selected applications during a scheduled
time window.

## Features

- Configurable Night Mode schedule
- Cross-midnight schedules
- App whitelist
- Accessibility-based app detection
- App blocking during Night Mode
- Whitelist and schedule locked during Night Mode
- Accessibility permission detection
- Persistent configuration using SharedPreferences

## How it works

NightGuard uses Android Accessibility Service to detect the
currently active application.

During Night Mode:

1. NightGuard detects the foreground application.
2. The application is checked against the whitelist.
3. If the application is not allowed, NightGuard blocks access.
4. Configuration changes are locked until Night Mode ends.

## Requirements

- Android
- Accessibility Service permission
- Android Studio
- JDK compatible with the project

## Disclaimer

NightGuard requires Accessibility Service access to provide
its app-blocking functionality.
