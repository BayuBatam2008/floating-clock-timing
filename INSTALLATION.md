# Installation Guide

This guide will help you install Floating Clock Timing on your Android device.

## Requirements

- Android 8.0 (Oreo) or higher
- Minimum 50MB free storage space
- Internet connection for NTP synchronization

## Download

### Option 1: Latest Stable Release (Recommended)

1. Go to [Releases](https://github.com/BayuBatam2008/floating-clock-timing/releases/latest)
2. Download `floating-clock-timing-{version}-release.apk`

### Option 2: Latest Development Build

1. Go to [Actions](https://github.com/BayuBatam2008/floating-clock-timing/actions)
2. Click on the latest successful workflow run (green checkmark)
3. Scroll down to **Artifacts**
4. Download `floating-clock-timing-debug-{sha}`

## Installation Steps

### Step 1: Enable Unknown Sources

Before installing, you need to allow installation from unknown sources:

**For Android 8.0 - 12:**
1. Go to **Settings** → **Security**
2. Enable **Unknown Sources** or **Install unknown apps**
3. Select your browser/file manager
4. Toggle **Allow from this source**

**For Android 13+:**
1. When you try to install the APK, you'll be prompted automatically
2. Tap **Settings**
3. Toggle **Allow from this source**

### Step 2: Install APK

1. Open the downloaded APK file
2. Tap **Install**
3. Wait for installation to complete
4. Tap **Open** or find the app in your app drawer

### Step 3: Grant Permissions

The app requires the following permissions:

1. **Display over other apps** (Required)
   - Needed for floating clock overlay
   - Go to: Settings → Apps → Floating Clock Timing → Display over other apps
   - Toggle ON

2. **Notifications** (Recommended)
   - For event reminders
   - Android will prompt when you schedule an event
   - Tap **Allow**

3. **Schedule exact alarms** (Recommended)
   - For precise event notifications
   - Android will prompt automatically
   - Tap **Allow**

### Step 4: Initial Setup

1. Open the app
2. Go to **Sync** tab
3. Tap **Sync Now** to synchronize time
4. Configure your preferences in **Style** tab
5. Start using the floating clock!

## Updating

### Manual Update

1. Download the new version APK
2. Install it (it will replace the old version)
3. Your settings and events will be preserved

### Check for Updates

- Watch the repository for new releases
- Enable **Watch** → **Releases only** on GitHub

## Troubleshooting

### Installation Failed

**Error: "App not installed"**
- Make sure you have enough storage space
- Uninstall the old version if updating
- Clear cache of your file manager/browser

**Error: "Package conflicts with existing package"**
- Uninstall the old version first
- Reinstall the new version

### Permission Issues

**Overlay not showing:**
1. Go to Settings → Apps → Floating Clock Timing
2. Check **Display over other apps** is enabled
3. Restart the app

**Notifications not working:**
1. Go to Settings → Apps → Floating Clock Timing → Notifications
2. Enable all notification categories
3. Check **Schedule exact alarms** permission

### App Crashes

1. Clear app data: Settings → Apps → Floating Clock Timing → Storage → Clear Data
2. Reinstall the app
3. Report the issue on GitHub with crash logs

## Uninstallation

1. Go to Settings → Apps → Floating Clock Timing
2. Tap **Uninstall**
3. Confirm uninstallation

## Security Notes

- This app is open source - you can review the code
- APK files are built using GitHub Actions
- No data is collected or sent to external servers
- NTP synchronization only contacts NTP servers you configure

## Getting Help

- **Issues**: [GitHub Issues](https://github.com/BayuBatam2008/floating-clock-timing/issues)
- **Discussions**: [GitHub Discussions](https://github.com/BayuBatam2008/floating-clock-timing/discussions)
- **Documentation**: [README](https://github.com/BayuBatam2008/floating-clock-timing/blob/main/README.md)

## Version Information

Check your installed version:
1. Open the app
2. Go to Settings (if available) or check About section
3. Compare with latest release version

## Tips

- **Battery Optimization**: Disable battery optimization for better overlay performance
  - Settings → Battery → Battery optimization → All apps → Floating Clock Timing → Don't optimize

- **Autostart**: Enable autostart permission if available (MIUI, ColorOS, etc.)
  - Settings → Apps → Autostart → Enable for Floating Clock Timing

- **Background Restrictions**: Disable background restrictions
  - Settings → Apps → Floating Clock Timing → Battery → Unrestricted
