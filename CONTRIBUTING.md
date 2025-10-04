# Contributing to Floating Clock Timing

Thank you for your interest in contributing to Floating Clock Timing! This document provides guidelines and information to help you contribute effectively.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Architecture](#project-architecture)
- [Coding Standards](#coding-standards)
- [Submitting Changes](#submitting-changes)
- [Reporting Bugs](#reporting-bugs)
- [Feature Requests](#feature-requests)

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Focus on what is best for the project
- Show empathy towards other contributors

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/floating-clock-timing.git
   cd floating-clock-timing
   ```
3. **Create a branch** for your changes:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Development Setup

### Prerequisites

- Android Studio (latest stable version)
- JDK 17 or higher
- Android SDK with API level 26-36
- Kotlin 1.9+

### Building the Project

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Build the project: `Build > Make Project`
4. Run on emulator or device: `Run > Run 'app'`

### Testing

- Run unit tests: `./gradlew test`
- Run instrumented tests: `./gradlew connectedAndroidTest`
- Test on multiple Android versions (API 26+)

## Project Architecture

### Directory Structure

```
app/src/main/java/com/floatingclock/timing/
├── data/              # Data layer (repositories, data sources)
├── notification/      # Notification management
├── overlay/           # Floating overlay system
├── ui/               # UI layer (screens, ViewModels)
├── utils/            # Utility classes and helpers
└── MainActivity.kt   # Application entry point
```

### Key Patterns

- **MVVM**: Model-View-ViewModel architecture
- **Repository Pattern**: Data abstraction layer
- **Single Source of Truth**: State flows from ViewModels
- **Unidirectional Data Flow**: UI events → ViewModel → State → UI

### Important Components

1. **EventNotificationManager**: Handles alarm scheduling and notifications
2. **FloatingClockService**: Manages the overlay window lifecycle
3. **TimeSyncManager**: Coordinates NTP time synchronization
4. **PreferencesRepository**: Manages user settings with DataStore

## Coding Standards

### Kotlin Style Guide

Follow the [official Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html):

- Use 4 spaces for indentation
- Use meaningful variable names
- Prefer `val` over `var` when possible
- Use trailing commas in multi-line declarations

### Naming Conventions

- **Classes**: PascalCase (e.g., `EventNotificationManager`)
- **Functions**: camelCase (e.g., `scheduleEventNotification`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `NOTIFICATION_ID`)
- **Variables**: camelCase (e.g., `eventTimeMillis`)

### Code Quality

- **Keep functions small**: Each function should do one thing well
- **Add comments**: Explain the "why", not the "what"
- **Avoid magic numbers**: Use named constants
- **Handle edge cases**: Check for null, empty, and error states
- **Use Kotlin features**: Extension functions, data classes, sealed classes

### Compose Best Practices

- **State hoisting**: Lift state to the appropriate level
- **Recomposition optimization**: Use `remember`, `derivedStateOf`, `key`
- **Side effects**: Use correct effect handlers (`LaunchedEffect`, `DisposableEffect`)
- **Preview annotations**: Add `@Preview` for all composables

### Example Code

```kotlin
/**
 * Schedules a notification for an event.
 *
 * @param eventName The name of the event
 * @param eventTimeMillis Event time in milliseconds since epoch
 */
fun scheduleEventNotification(eventName: String, eventTimeMillis: Long) {
    val notificationTime = eventTimeMillis - REMINDER_OFFSET_MILLIS
    
    if (notificationTime <= System.currentTimeMillis()) {
        Log.w(TAG, "Event time has passed, skipping notification")
        return
    }
    
    // Schedule the notification...
}
```

## Submitting Changes

### Pull Request Process

1. **Update your branch** with the latest main:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Commit your changes** with clear messages:
   ```bash
   git commit -m "feat: add new notification sound option"
   ```

3. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

4. **Create a Pull Request** on GitHub

### Commit Message Guidelines

Use conventional commit format:

- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `style:` - Code style changes (formatting, etc.)
- `refactor:` - Code refactoring
- `perf:` - Performance improvements
- `test:` - Adding or updating tests
- `chore:` - Maintenance tasks

Examples:
```
feat: add customizable notification sound
fix: resolve crash when overlay permission denied
docs: update README with new features
refactor: simplify event scheduling logic
```

### Pull Request Checklist

- [ ] Code follows the project's coding standards
- [ ] All tests pass
- [ ] New features include tests
- [ ] Documentation is updated (if needed)
- [ ] No warnings in Android Studio
- [ ] Tested on multiple Android versions
- [ ] Screenshots/videos for UI changes (if applicable)
- [ ] PR description explains the changes

## Reporting Bugs

### Before Submitting

1. Check if the bug has already been reported
2. Test on the latest version
3. Verify it's not a device-specific issue

### Bug Report Template

```markdown
**Describe the bug**
A clear description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '...'
3. See error

**Expected behavior**
What you expected to happen.

**Screenshots**
If applicable, add screenshots.

**Device information:**
 - Device: [e.g., Pixel 6]
 - Android Version: [e.g., Android 13]
 - App Version: [e.g., 1.0]

**Additional context**
Any other relevant information.
```

## Feature Requests

We welcome feature suggestions! Please:

1. **Search existing issues** to avoid duplicates
2. **Describe the problem** you're trying to solve
3. **Propose a solution** if you have one in mind
4. **Explain the benefits** to users

### Feature Request Template

```markdown
**Is your feature request related to a problem?**
A clear description of the problem.

**Describe the solution you'd like**
What you want to happen.

**Describe alternatives you've considered**
Alternative solutions or features.

**Additional context**
Mockups, examples, or other context.
```

## Areas for Contribution

Looking for ideas? Here are some areas that could use help:

### Features
- [ ] Multiple event support with different notification times
- [ ] Custom notification sounds
- [ ] Widget support
- [ ] Landscape mode optimization
- [ ] Tablet UI optimization
- [ ] Export/import events
- [ ] Recurring events

### Improvements
- [ ] Better error handling
- [ ] Accessibility improvements
- [ ] Performance optimization
- [ ] Battery usage optimization
- [ ] UI/UX enhancements
- [ ] More NTP server options
- [ ] Localization (translations)

### Documentation
- [ ] Code documentation
- [ ] User guide
- [ ] Video tutorials
- [ ] Architecture diagrams
- [ ] API documentation

### Testing
- [ ] Unit tests coverage
- [ ] Integration tests
- [ ] UI tests
- [ ] Performance tests

## Questions?

If you have questions:

1. Check the README.md
2. Search existing issues
3. Open a new issue with the "question" label

## Thank You!

Your contributions make this project better for everyone. We appreciate your time and effort! 🎉
