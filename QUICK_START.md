# Quick Start Guide

Get started with Floating Clock Timing in 5 minutes!

## 🚀 Quick Install

1. **Download APK**: [Latest Release](https://github.com/BayuBatam2008/floating-clock-timing/releases/latest)
2. **Install**: Open the APK file and tap Install
3. **Grant Permissions**: Allow overlay and notifications
4. **Sync Time**: Open app → Sync tab → Tap "Sync Now"
5. **Start Overlay**: Clock tab → Tap the Play button

Done! Your floating clock is now running. 🎉

## 📱 Main Tabs

### 1. Clock Tab
- View synchronized time
- Start/Stop floating overlay
- Enter Picture-in-Picture mode
- Quick add events

### 2. Events Tab
- View all scheduled events
- Create new events with countdown
- Edit or delete events
- Events auto-switch after completion

### 3. Sync Tab
- **NTP Server Selection**
  - Choose from pre-configured servers
  - Add custom NTP servers
  - View last sync status
  
- **Auto-Sync Settings**
  - Enable/disable automatic sync
  - Set sync interval (2-60 minutes)
  - Manual sync button

### 4. Style Tab
- **Font & Display**
  - Font scale (0.6x - 1.6x)
  - Show/hide milliseconds
  
- **Progress Indicator**
  - Enable countdown progress bar
  - Set activation time (1-10 seconds)
  
- **Pulsing Animation**
  - Enable pulsing when event triggers
  - Adjust pulsing speed
  
- **Sound Trigger**
  - Enable countdown beeps
  - Choose count mode (3, 5, or 10)
  
- **Line 2 Display**
  - Date only
  - Target time only
  - Both
  - None

## 🎯 Common Use Cases

### Use Case 1: Event Timer
Perfect for timing presentations, exams, or cooking:

1. Go to **Events** tab
2. Tap **Add Event** button
3. Set your target time
4. Name your event (optional)
5. Tap **Save**
6. Clock will show countdown and pulse when time arrives

### Use Case 2: Always-On Clock
Keep a precise clock always visible:

1. Go to **Sync** tab
2. Enable **Auto-Sync**
3. Set interval to 15-30 minutes
4. Go to **Clock** tab
5. Tap **Play** button
6. Clock stays on top of all apps

### Use Case 3: Picture-in-Picture Mode
Minimal clock during videos/games:

1. Open the app
2. Tap the **Play** button
3. Clock appears in PiP mode
4. Move it anywhere on screen
5. Resize by pinching

## ⚡ Pro Tips

### Battery Optimization
Disable battery optimization for best performance:
- Settings → Battery → Battery Optimization
- Find "Floating Clock Timing"
- Select "Don't optimize"

### Notification Sounds
Customize event notification sounds:
- Settings → Apps → Floating Clock Timing → Notifications
- Select notification category
- Change sound

### Quick Event Creation
From Clock tab:
1. Tap the **+** button (bottom right)
2. Quick modal appears
3. Set time and save
4. No need to go to Events tab

### Multiple NTP Servers
Add backup servers for reliability:
1. Sync tab → Add custom server
2. Enter server address (e.g., `time.google.com`)
3. Tap Add
4. Switch between servers easily

## 🔧 Troubleshooting

### Overlay Not Showing
✅ **Solution**: 
- Go to Settings → Apps → Floating Clock Timing
- Enable "Display over other apps"
- Restart the app

### Events Not Notifying
✅ **Solution**:
- Check notification permissions
- Enable "Schedule exact alarms" permission
- Disable battery optimization

### Time Not Syncing
✅ **Solution**:
- Check internet connection
- Try different NTP server
- Manually tap "Sync Now"

### PiP Mode Not Working
✅ **Solution**:
- Android 8.0+ required
- Enable PiP in app settings
- Try entering from Clock tab

## 📚 Learn More

- **Full Documentation**: [README.md](README.md)
- **Installation Guide**: [INSTALLATION.md](INSTALLATION.md)
- **CI/CD Documentation**: [.github/WORKFLOWS.md](.github/WORKFLOWS.md)
- **Report Issues**: [GitHub Issues](https://github.com/BayuBatam2008/floating-clock-timing/issues)

## 🎨 Customization Examples

### Minimal Clock
- Font scale: 0.8x
- Hide milliseconds
- Line 2: None
- No progress indicator

### Full Detail Clock
- Font scale: 1.2x
- Show milliseconds
- Line 2: Both (Date + Target)
- Progress indicator: 10s

### Event-Focused
- Font scale: 1.0x
- Show milliseconds
- Line 2: Target time only
- Progress indicator: 5s
- Sound trigger: 5 count
- Pulsing: Fast speed

## ❓ FAQ

**Q: Does this drain battery?**
A: Minimal impact with optimization disabled. Auto-sync every 30 minutes recommended.

**Q: Can I use custom NTP servers?**
A: Yes! Add any NTP server in Sync tab.

**Q: Will events work when app is closed?**
A: Yes, with proper permissions. Notifications are scheduled with Android AlarmManager.

**Q: Can I have multiple events?**
A: Yes! Create unlimited events. They auto-switch when triggered.

**Q: Does it work offline?**
A: Clock works offline, but initial NTP sync requires internet.

**Q: How accurate is the time?**
A: Sub-second accuracy with NTP synchronization and drift compensation.

---

**Need Help?** Open an [issue](https://github.com/BayuBatam2008/floating-clock-timing/issues) or start a [discussion](https://github.com/BayuBatam2008/floating-clock-timing/discussions)!
