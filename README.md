# Floating Clock Timing

[![Android CI Build](https://github.com/BayuBatam2008/floating-clock-timing/actions/workflows/android-build.yml/badge.svg)](https://github.com/BayuBatam2008/floating-clock-timing/actions/workflows/android-build.yml)
[![Release Build](https://github.com/BayuBatam2008/floating-clock-timing/actions/workflows/release.yml/badge.svg)](https://github.com/BayuBatam2008/floating-clock-timing/actions/workflows/release.yml)
[![GitHub release](https://img.shields.io/github/v/release/BayuBatam2008/floating-clock-timing)](https://github.com/BayuBatam2008/floating-clock-timing/releases)
[![License](https://img.shields.io/github/license/BayuBatam2008/floating-clock-timing)](LICENSE)

An Android application that provides a precise floating clock overlay with NTP time synchronization and event scheduling capabilities.

## 📥 Download

**Latest Release**: [Download APK](https://github.com/BayuBatam2008/floating-clock-timing/releases/latest)

For development builds from the latest commits, check the [Actions](https://github.com/BayuBatam2008/floating-clock-timing/actions) tab and download artifacts.

## 📚 Documentation

- **[Quick Start Guide](QUICK_START.md)** - Get started in 5 minutes
- **[Installation Guide](INSTALLATION.md)** - Detailed installation instructions
- **[Workflows Guide](.github/WORKFLOWS.md)** - CI/CD and release documentation

## Features

- **Floating Clock Overlay**: Display a customizable clock that stays on top of other apps
- **NTP Time Synchronization**: Sync with NTP servers for precise time tracking
- **Event Scheduling**: Schedule events with 5-minute reminder notifications
- **Picture-in-Picture Mode**: Minimize the clock into PiP mode
- **Customizable UI**: Adjust font size, opacity, colors, and display format
- **Multiple Time Display Modes**: Show hours, minutes, seconds, and milliseconds

## Project Structure

```
app/src/main/java/com/floatingclock/timing/
├── data/
│   ├── AppDependencies.kt          # Dependency injection container
│   ├── EventRepository.kt          # Event data management
│   ├── NtpClient.kt                # NTP time synchronization client
│   ├── PerformanceOptimizations.kt # Performance utilities
│   ├── PreferencesRepository.kt    # User preferences storage
│   └── TimeSyncManager.kt          # Time synchronization manager
│
├── notification/
│   └── EventNotificationManager.kt # Event notification scheduling
│
├── overlay/
│   ├── FloatingClockController.kt  # Overlay window controller
│   ├── FloatingClockService.kt     # Foreground service for overlay
│   └── OverlayUi.kt                # Overlay UI components
│
├── ui/
│   ├── events/
│   │   ├── EventEditModal.kt       # Event creation/editing UI
│   │   ├── EventsScreen.kt         # Events list screen
│   │   └── EventViewModel.kt       # Events screen ViewModel
│   │
│   ├── theme/
│   │   └── Theme.kt                # Material 3 theme configuration
│   │
│   └── FloatingClockApp.kt         # Main UI composable
│
├── utils/
│   ├── DateTimeFormatters.kt       # Date/time formatting utilities
│   └── UIComponents.kt             # Reusable UI components
│
├── FloatingClockApplication.kt     # Application class
├── MainActivity.kt                 # Main activity
└── MainViewModel.kt                # Main ViewModel
```

## Key Components

### Data Layer

- **EventRepository**: Manages event data with DataStore persistence
- **PreferencesRepository**: Handles user preferences and settings
- **NtpClient**: Implements NTP protocol for time synchronization
- **TimeSyncManager**: Coordinates time synchronization with multiple NTP servers

### Notification System

- **EventNotificationManager**: Schedules precise event reminders using Android AlarmManager
  - Sends notifications 5 minutes before scheduled events
  - Uses exact alarms for precise timing
  - Efficient resource usage with no background polling

### Overlay System

- **FloatingClockService**: Foreground service that maintains the floating clock
- **FloatingClockController**: Manages the overlay window lifecycle
- **OverlayUi**: Renders the floating clock with customizable appearance

### UI Layer

- **FloatingClockApp**: Main app UI with tabs for Clock, Sync, Customization, and Events
- **EventViewModel**: Manages event state and business logic
- **Theme**: Material 3 dynamic theming support

## Permissions

The app requires the following permissions:

- `INTERNET`: For NTP time synchronization
- `ACCESS_NETWORK_STATE`: To check network connectivity
- `POST_NOTIFICATIONS`: For event reminder notifications (Android 13+)
- `SCHEDULE_EXACT_ALARM`: For precise alarm scheduling
- `SYSTEM_ALERT_WINDOW`: For displaying the floating overlay
- `WAKE_LOCK`: To keep the service running
- `VIBRATE`: For notification vibrations

## Requirements

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36 (Android 14)
- **Kotlin**: 1.9+
- **Compose**: Material 3

## Building the Project

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on an Android device or emulator (API 26+)

```bash
./gradlew build
```

## Architecture

The app follows modern Android development practices:

- **MVVM Architecture**: Separation of UI and business logic
- **Jetpack Compose**: Modern declarative UI framework
- **Kotlin Coroutines & Flow**: Asynchronous programming
- **DataStore**: Type-safe data storage
- **Material 3**: Latest Material Design guidelines
- **Dependency Injection**: Manual DI with AppDependencies

## Contributing

Contributions are welcome! Here are some ways you can help:

1. **Report bugs**: Open an issue describing the bug and how to reproduce it
2. **Suggest features**: Open an issue with your feature suggestion
3. **Submit PRs**: Fork the repo, make your changes, and submit a pull request

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions focused and small

### Testing

Before submitting a PR:

1. Test the app on different Android versions
2. Verify overlay permissions work correctly
3. Test notification scheduling functionality
4. Ensure NTP synchronization works

## License

See [LICENSE](LICENSE) file for details.

## NTP Servers

The app includes several pre-configured NTP servers:

- `time.google.com` (Google)
- `pool.ntp.org` (NTP Pool Project)
- `time.cloudflare.com` (Cloudflare)
- Custom servers can be added

## Technical Highlights

### Efficient Notifications

The notification system uses AlarmManager for precise scheduling instead of polling, resulting in:
- Minimal battery consumption
- Accurate notification timing
- No background processing overhead

### Time Synchronization

- Multi-server NTP synchronization for reliability
- Automatic retry with exponential backoff
- Network state monitoring
- Configurable sync intervals

### Performance Optimizations

- Main thread optimization utilities
- Lazy initialization of heavy components
- Efficient state management with Flow
- Minimal recomposition in Compose UI

---

**Note**: This app is designed for precise time tracking and event scheduling. For best results, ensure a stable internet connection for NTP synchronization.
