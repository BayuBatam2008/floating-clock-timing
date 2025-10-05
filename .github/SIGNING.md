# APK Signing Guide

This guide explains how to properly sign your Android APK for distribution.

## Current Setup (Development)

Currently, the app uses the **debug keystore** for both debug and release builds:

```kotlin
// app/build.gradle.kts
signingConfigs {
    create("release") {
        storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
    }
}
```

⚠️ **This is suitable for:**
- Testing and development
- Personal use
- Quick distribution to friends

❌ **Not suitable for:**
- Google Play Store distribution
- Production apps
- Apps with sensitive data

---

## Why Signing is Required

### Android 15 Requirements

Android 15 enforces stricter security:
- All APKs **must be signed** to install
- Unsigned APKs show error: "App not installed as package appears to be invalid"
- Debug keystore is acceptable for testing, but not for production

### Signing Benefits

✅ **Verifies app authenticity** - Confirms the app hasn't been tampered with
✅ **Enables app updates** - Same signature required for updates
✅ **Required for distribution** - Google Play and other stores require proper signing
✅ **Protects users** - Prevents malicious modifications

---

## Creating a Production Keystore

### Step 1: Generate Keystore

Open terminal and run:

```bash
keytool -genkey -v \
  -keystore my-release-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias my-key-alias
```

**PowerShell (Windows):**
```powershell
keytool -genkey -v `
  -keystore my-release-key.jks `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -alias my-key-alias
```

### Step 2: Answer Prompts

```
Enter keystore password: [Create a strong password]
Re-enter new password: [Confirm password]
What is your first and last name?
  [CN]: John Doe
What is the name of your organizational unit?
  [OU]: Development
What is the name of your organization?
  [O]: MyCompany
What is the name of your City or Locality?
  [L]: Jakarta
What is the name of your State or Province?
  [ST]: DKI Jakarta
What is the two-letter country code for this unit?
  [C]: ID
Is CN=John Doe, OU=Development, O=MyCompany, L=Jakarta, ST=DKI Jakarta, C=ID correct?
  [no]: yes

Enter key password for <my-key-alias>
  (RETURN if same as keystore password): [Press ENTER or create different password]
```

### Step 3: Secure the Keystore

🔒 **CRITICAL: Keep this file safe!**

```bash
# Create a secure location
mkdir -p ~/.android/keystores

# Move keystore to secure location
mv my-release-key.jks ~/.android/keystores/

# Set restrictive permissions (Linux/Mac)
chmod 600 ~/.android/keystores/my-release-key.jks
```

⚠️ **WARNING:**
- **NEVER commit keystore to Git**
- **Store password in secure location** (password manager)
- **Make encrypted backup** of keystore
- **Losing keystore = Cannot update app**

---

## Configuring Gradle

### Option 1: Using gradle.properties (Recommended)

Create or edit `gradle.properties` in project root:

```properties
# DO NOT COMMIT THESE VALUES TO GIT
# Add gradle.properties to .gitignore

RELEASE_STORE_FILE=~/.android/keystores/my-release-key.jks
RELEASE_STORE_PASSWORD=your_keystore_password
RELEASE_KEY_ALIAS=my-key-alias
RELEASE_KEY_PASSWORD=your_key_password
```

Update `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            // Use properties from gradle.properties
            val keystorePropertiesFile = rootProject.file("gradle.properties")
            if (keystorePropertiesFile.exists()) {
                val properties = java.util.Properties()
                keystorePropertiesFile.inputStream().use { properties.load(it) }
                
                storeFile = file(properties["RELEASE_STORE_FILE"] as String)
                storePassword = properties["RELEASE_STORE_PASSWORD"] as String
                keyAlias = properties["RELEASE_KEY_ALIAS"] as String
                keyPassword = properties["RELEASE_KEY_PASSWORD"] as String
            }
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### Option 2: Using Environment Variables

Update `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("RELEASE_STORE_FILE") ?: "~/.android/debug.keystore")
            storePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: "androiddebugkey"
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: "android"
        }
    }
}
```

Set environment variables:

**Linux/Mac:**
```bash
export RELEASE_STORE_FILE=~/.android/keystores/my-release-key.jks
export RELEASE_STORE_PASSWORD=your_password
export RELEASE_KEY_ALIAS=my-key-alias
export RELEASE_KEY_PASSWORD=your_password
```

**Windows PowerShell:**
```powershell
$env:RELEASE_STORE_FILE="C:\Users\YourName\.android\keystores\my-release-key.jks"
$env:RELEASE_STORE_PASSWORD="your_password"
$env:RELEASE_KEY_ALIAS="my-key-alias"
$env:RELEASE_KEY_PASSWORD="your_password"
```

---

## GitHub Actions Setup

### Store Secrets

1. Go to GitHub repository → Settings → Secrets → Actions
2. Add these secrets:

| Secret Name | Value |
|-------------|-------|
| `KEYSTORE_BASE64` | Base64 encoded keystore file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

### Encode Keystore

**Linux/Mac:**
```bash
base64 -i ~/.android/keystores/my-release-key.jks > keystore.base64
# Copy content of keystore.base64 to KEYSTORE_BASE64 secret
```

**Windows PowerShell:**
```powershell
$bytes = [System.IO.File]::ReadAllBytes("C:\Users\YourName\.android\keystores\my-release-key.jks")
$base64 = [System.Convert]::ToBase64String($bytes)
$base64 | Out-File -FilePath keystore.base64
# Copy content to KEYSTORE_BASE64 secret
```

### Update Workflow

Update `.github/workflows/release.yml`:

```yaml
- name: Decode Keystore
  run: |
    echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > app/keystore.jks

- name: Build Release APK
  run: ./gradlew assembleRelease
  env:
    RELEASE_STORE_FILE: app/keystore.jks
    RELEASE_STORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    RELEASE_KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
    RELEASE_KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
```

---

## Building Signed APK

### Debug Build (Debug Keystore)

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build (Production Keystore)

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

---

## Verifying Signature

### Check APK Signature

```bash
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

Expected output:
```
jar verified.
```

### View Certificate Details

```bash
keytool -list -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

Output shows:
- Signer certificate CN, OU, O, L, ST, C
- Certificate fingerprints (SHA256, SHA1, MD5)
- Valid from/until dates

### Using apksigner (Android SDK)

```bash
# Windows
%ANDROID_SDK%\build-tools\35.0.0\apksigner.bat verify --print-certs app-release.apk

# Linux/Mac
$ANDROID_SDK/build-tools/35.0.0/apksigner verify --print-certs app-release.apk
```

---

## Best Practices

### Security

✅ **DO:**
- Use strong passwords (16+ characters, mixed case, numbers, symbols)
- Store keystore in secure location (NOT in project directory)
- Add keystore files to `.gitignore`
- Keep encrypted backup of keystore
- Use password manager for credentials
- Use different passwords for keystore and key

❌ **DON'T:**
- Commit keystore to Git
- Share keystore publicly
- Use weak passwords
- Store passwords in code
- Email keystore files
- Lose your keystore (you won't be able to update your app!)

### Keystore Management

1. **Backup Strategy:**
   ```bash
   # Create encrypted backup
   gpg --symmetric --cipher-algo AES256 my-release-key.jks
   
   # Store encrypted backup in multiple locations:
   # - Secure cloud storage (Google Drive, Dropbox)
   # - External encrypted drive
   # - Secure password manager (1Password, Bitwarden)
   ```

2. **Password Storage:**
   - Use password manager (1Password, Bitwarden, LastPass)
   - Store in secure notes
   - Include keystore location and key alias

3. **Documentation:**
   - Record keystore creation date
   - Document validity period
   - Note SHA256 fingerprint
   - Keep certificate details

---

## Troubleshooting

### Error: "App not installed as package appears to be invalid"

**Causes:**
- APK is not signed
- Signature verification failed
- Corrupted APK file

**Solution:**
1. Ensure signing config is properly set up
2. Rebuild APK: `./gradlew clean assembleRelease`
3. Verify signature: `jarsigner -verify app-release.apk`

### Error: "Keystore was tampered with, or password was incorrect"

**Cause:** Wrong keystore password

**Solution:**
1. Verify password in `gradle.properties`
2. Re-enter password when prompted
3. If forgotten, you'll need to create new keystore (and can't update existing app)

### Error: "Cannot recover key"

**Cause:** Wrong key password

**Solution:**
1. Check `RELEASE_KEY_PASSWORD` value
2. Verify key alias is correct
3. Try using keystore password if key password was same

---

## Migration from Debug to Production

### Current State

```kotlin
// Using debug keystore (current)
create("release") {
    storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
    storePassword = "android"
    keyAlias = "androiddebugkey"
    keyPassword = "android"
}
```

### Migration Steps

1. **Generate production keystore** (see above)
2. **Update `app/build.gradle.kts`** with production config
3. **Build and test** new signed APK
4. **Distribute** signed APK to users
5. **Update GitHub Actions** with secrets

⚠️ **IMPORTANT:** If you've already distributed app signed with debug keystore:
- Users will need to uninstall old version
- New version with production keystore is treated as different app
- Cannot update in-place due to signature mismatch

---

## Google Play Store Requirements

For Play Store distribution:

1. **App Signing by Google Play (Recommended)**
   - Google manages signing key
   - You upload unsigned APK/AAB
   - Google signs and distributes
   - Easier key management

2. **Manual Signing**
   - You manage signing key
   - Upload signed APK/AAB
   - Full control over keys
   - More responsibility

**Learn more:** https://developer.android.com/studio/publish/app-signing

---

## Resources

- [Android Developers: Sign Your App](https://developer.android.com/studio/publish/app-signing)
- [Gradle: Signing Configs](https://developer.android.com/build/building-cmdline#sign_cmdline)
- [keytool Documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)

---

Last Updated: 2025-10-05
