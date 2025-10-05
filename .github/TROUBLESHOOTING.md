# CI/CD Troubleshooting Guide

Common issues and solutions for GitHub Actions workflows and APK installation.

## APK Installation Issues

### 1. App not installed as package appears to be invalid (Android 15)

**Error Message:**
```
App not installed
App not installed as package appears to be invalid.
```

**Affects:** Android 15 (and some Android 14 devices)

**Cause:**
- APK is not properly signed
- Missing or invalid digital signature
- Android 15 enforces stricter signature verification

**Solution:**

The app now includes proper signing configuration. Download the latest release APK from:
- GitHub Releases: https://github.com/BayuBatam2008/floating-clock-timing/releases

If building from source:
```bash
# Clean and rebuild with signing
./gradlew clean assembleRelease

# APK location
app/build/outputs/apk/release/app-release.apk
```

**Verify APK signature:**
```bash
jarsigner -verify -verbose app-release.apk
```

**For developers:** See [APK Signing Guide](.github/SIGNING.md) for detailed signing instructions.

---

## Common Build Errors

### 1. Keystore file not found for signing config

**Error Message:**
```
Execution failed for task ':app:validateSigningDebug'.
> Keystore file '/home/runner/.android/debug.keystore' not found for signing config 'debug'.
```

**Affects:** GitHub Actions CI/CD builds

**Cause:**
- GitHub Actions runner doesn't have debug.keystore file
- Signing config was trying to use non-existent keystore

**Solution:**

✅ **Already Fixed!** The build configuration now:
- Checks if keystore file exists before applying signing config
- Allows unsigned builds in CI (Android auto-signs debug builds)
- Uses debug keystore if available locally

**Technical Details:**
```kotlin
// Conditional signing configuration
val debugKeystoreFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
if (debugKeystoreFile.exists()) {
    signingConfig = signingConfigs.getByName("debug")
}
```

**Note:** Debug APKs built in CI are unsigned but installable. Release APKs should use proper keystore (see [SIGNING.md](SIGNING.md)).

---

### 2. Unable to access jarfile gradle-wrapper.jar

**Error Message:**
```
Error: Unable to access jarfile /home/runner/work/.../gradle/wrapper/gradle-wrapper.jar
```

**Cause:**
- `gradle-wrapper.jar` is in `.gitignore`
- File not committed to repository

**Solution:**
```bash
# Remove gradle-wrapper.jar from .gitignore
# Then regenerate and commit wrapper
./gradlew wrapper
git add gradle/wrapper/gradle-wrapper.jar
git commit -m "Add Gradle wrapper JAR for CI/CD"
git push
```

**Note:** The `gradle-wrapper.jar` file MUST be committed for CI/CD to work!

---

### 3. Permission Denied on gradlew

**Error Message:**
```
Permission denied: ./gradlew
```

**Cause:**
- `gradlew` doesn't have execute permissions

**Solution:**
Already fixed in workflows with:
```yaml
- name: Grant execute permission for gradlew
  run: chmod +x gradlew
```

---

### 4. Gradle Version Mismatch

**Error Message:**
```
Minimum supported Gradle version is 8.13. Current version is 8.9.
Try updating the 'distributionUrl' property in gradle-wrapper.properties to 'gradle-8.13-bin.zip'.
```

**Cause:**
- Android Gradle Plugin (AGP) version requires newer Gradle version
- Mismatch between AGP in `build.gradle.kts` and Gradle wrapper version

**Solution:**
```bash
# Update Gradle wrapper to required version
./gradlew wrapper --gradle-version 8.13

# Or manually edit gradle/wrapper/gradle-wrapper.properties
# Change: distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip

# Commit and push changes
git add gradle/wrapper/
git commit -m "Update Gradle to 8.13"
git push
```

**Version Compatibility:**
- AGP 8.13.0 requires Gradle 8.13+
- AGP 8.7.x requires Gradle 8.9+
- AGP 8.4.x requires Gradle 8.6+

---

### 5. Build Failed - Dependency Resolution

**Error Message:**
```
Could not resolve all dependencies
```

**Cause:**
- Network issues
- Invalid dependency version
- Repository not accessible

**Solution:**
1. Check `build.gradle.kts` dependencies
2. Verify repository URLs
3. Re-run workflow (may be transient network issue)

---

### 6. Out of Memory Error

**Error Message:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Cause:**
- Insufficient memory for Gradle build

**Solution:**
Add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
```

Or add to workflow:
```yaml
- name: Build with Gradle
  run: ./gradlew assembleDebug
  env:
    GRADLE_OPTS: -Xmx4g
```

---

### 7. SDK Not Found

**Error Message:**
```
SDK location not found
```

**Cause:**
- `local.properties` contains local paths
- File should not be committed

**Solution:**
Ensure `local.properties` is in `.gitignore`:
```gitignore
local.properties
```

GitHub Actions will use its own SDK path.

---

### 8. Signing Configuration Error

**Error Message:**
```
Signing config is missing
```

**Cause:**
- Release build requires signing configuration
- Keystore not available in CI

**Solution:**
For unsigned releases, workflow already uses:
```yaml
continue-on-error: true
```

For signed releases, add secrets:
1. Go to Settings → Secrets → Actions
2. Add keystore as base64:
   ```bash
   base64 -i keystore.jks > keystore.base64
   ```
3. Add secrets:
   - `KEYSTORE_BASE64`
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`

---

### 9. Workflow Not Triggering

**Possible Causes:**

1. **Workflow file in wrong location**
   - Must be in `.github/workflows/`
   - Must have `.yml` or `.yaml` extension

2. **Branch protection**
   - Check if branch requires status checks
   - Workflow may need approval

3. **Workflow disabled**
   - Check Actions tab → Workflows
   - Enable if disabled

4. **Push to wrong branch**
   - Workflow triggers on specific branches
   - Check `on.push.branches` in workflow file

---

### 10. Artifact Upload Failed

**Error Message:**
```
Unable to upload artifact
```

**Cause:**
- Artifact too large (>5GB)
- Network timeout
- Invalid file path

**Solution:**
1. Check APK file size
2. Verify file path exists
3. Re-run workflow

---

### 11. Cache Restore Failed

**Error Message:**
```
Failed to restore cache
```

**Cause:**
- Cache key mismatch
- Cache expired (7 days)

**Solution:**
Not critical - workflow will continue:
```yaml
cache: gradle  # Automatically handled
```

If persistent, clear cache manually:
- Settings → Actions → Caches → Delete

---

### 12. Deprecated Warnings

**Warning Message:**
```
Deprecated Gradle features were used
```

**Cause:**
- Using outdated Gradle APIs
- Dependencies using old APIs

**Solution:**
Run locally to see details:
```bash
./gradlew build --warning-mode all
```

Update dependencies in `build.gradle.kts`

---

## Debugging Workflows

### Enable Debug Logging

Add secrets to repository:
- `ACTIONS_STEP_DEBUG` = `true`
- `ACTIONS_RUNNER_DEBUG` = `true`

Then re-run workflow to see detailed logs.

### View Workflow Logs

1. Go to Actions tab
2. Click on workflow run
3. Click on job name
4. Expand each step to view logs

### Download Logs

1. Go to workflow run
2. Click on gear icon (⚙️)
3. Select "Download log archive"

---

## Workflow Best Practices

### 1. Use Caching
```yaml
- uses: actions/setup-java@v4
  with:
    cache: gradle  # Cache Gradle dependencies
```

### 2. Matrix Builds
For multiple API levels:
```yaml
strategy:
  matrix:
    api-level: [28, 29, 30, 31]
```

### 3. Conditional Steps
```yaml
- name: Upload APK
  if: success()  # Only if build succeeded
```

### 4. Timeouts
```yaml
jobs:
  build:
    timeout-minutes: 30  # Prevent hanging builds
```

### 5. Concurrency Control
```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true  # Cancel old runs
```

---

## Performance Optimization

### 1. Gradle Daemon
Already enabled in `gradle.properties`:
```properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
```

### 2. Build Scan
Add to workflow for insights:
```yaml
- name: Build with scan
  run: ./gradlew build --scan
```

### 3. Skip Tasks
For faster builds:
```yaml
run: ./gradlew assembleDebug -x lint -x test
```

---

## Security Best Practices

### 1. Never Commit Secrets
- Keystore files
- API keys
- Passwords
- Tokens

Use GitHub Secrets instead!

### 2. Verify Workflow Permissions
```yaml
permissions:
  contents: write  # Only what's needed
```

### 3. Pin Action Versions
```yaml
# ✅ Good
uses: actions/checkout@v4

# ❌ Avoid
uses: actions/checkout@main
```

### 4. Scan Dependencies
Add to workflow:
```yaml
- name: OWASP Dependency Check
  run: ./gradlew dependencyCheckAnalyze
```

---

## Monitoring Builds

### Build Status Badge
Add to README:
```markdown
[![Build Status](https://github.com/USER/REPO/actions/workflows/android-build.yml/badge.svg)](https://github.com/USER/REPO/actions)
```

### Notifications
Enable in GitHub Settings:
- Settings → Notifications → Actions
- Choose email/web notifications

### Webhooks
For Slack/Discord notifications:
1. Settings → Webhooks
2. Add webhook URL
3. Select events to notify

---

## Getting Help

- **GitHub Actions Docs**: https://docs.github.com/actions
- **Gradle Docs**: https://docs.gradle.org
- **Android Build Docs**: https://developer.android.com/build

---

## Quick Reference

### Re-run Failed Jobs
```bash
# From GitHub UI:
Actions → Failed run → Re-run failed jobs
```

### Cancel Running Workflow
```bash
# From GitHub UI:
Actions → Running workflow → Cancel workflow
```

### Clear All Caches
```bash
# From GitHub UI:
Settings → Actions → Caches → Delete all
```

### Manually Trigger Workflow
```bash
# From GitHub UI:
Actions → Workflow → Run workflow
```

---

Last Updated: 2025-10-05
