# GitHub Actions CI/CD Documentation

This repository uses GitHub Actions for automated building and releasing of the Android application.

## Workflows

### 1. Android CI Build (`android-build.yml`)

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` branch
- Manual workflow dispatch

**What it does:**
- Builds both Debug and Release APK files
- Renames APK files with commit SHA for easy identification
- Uploads APK files as artifacts (available for 30 days)
- Generates build summary with download links
- Creates build info file with commit details

**Artifacts produced:**
- `floating-clock-timing-debug-{sha}.apk` - Debug build
- `floating-clock-timing-release-{sha}.apk` - Release build (unsigned)
- `build-info-{sha}.txt` - Build information

**How to download builds:**
1. Go to the [Actions](https://github.com/BayuBatam2008/floating-clock-timing/actions) tab
2. Click on the latest successful workflow run
3. Scroll down to the **Artifacts** section
4. Download the APK you need

### 2. Release Build (`release.yml`)

**Triggers:**
- Push of version tags (e.g., `v1.0.0`, `v2.1.3`)
- Manual workflow dispatch

**What it does:**
- Builds production-ready Release and Debug APK files
- Generates changelog from git commits
- Creates a GitHub Release with:
  - Release notes
  - Changelog
  - APK files ready for download
- Uploads artifacts (available for 90 days)

**How to create a release:**

1. **Tag your commit:**
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

2. **Wait for the workflow to complete:**
   - Check the Actions tab for progress
   - Workflow will automatically create a GitHub Release

3. **Download from Releases:**
   - Go to [Releases](https://github.com/BayuBatam2008/floating-clock-timing/releases)
   - Find your release version
   - Download the APK files

## Build Artifacts

### File Naming Convention

**CI Builds:**
- `floating-clock-timing-debug-{short-sha}.apk`
- `floating-clock-timing-release-{short-sha}.apk`

**Release Builds:**
- `floating-clock-timing-{version}-debug.apk`
- `floating-clock-timing-{version}-release.apk`

### APK Types

- **Debug APK**: 
  - Larger file size
  - Contains debugging information
  - Not optimized
  - Suitable for testing

- **Release APK**: 
  - Smaller file size
  - Optimized and minified
  - No debugging info
  - Recommended for production use

## Version Tagging Guidelines

Follow semantic versioning: `vMAJOR.MINOR.PATCH`

- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes

**Examples:**
- `v1.0.0` - Initial release
- `v1.1.0` - Added new feature
- `v1.1.1` - Bug fix
- `v2.0.0` - Breaking changes

## Manual Workflow Dispatch

Both workflows can be triggered manually:

1. Go to the **Actions** tab
2. Select the workflow (Android CI Build or Release Build)
3. Click **Run workflow**
4. Choose the branch
5. Click **Run workflow** button

## Build Environment

- **OS**: Ubuntu Latest
- **Java**: JDK 17 (Temurin distribution)
- **Build Tool**: Gradle (with caching enabled)
- **Build Timeout**: Default (6 hours max)

## Artifact Retention

- **CI Builds**: 30 days
- **Release Builds**: 90 days

After retention period, artifacts are automatically deleted but releases remain available.

## Troubleshooting

### Build Failed

1. Check the workflow logs in Actions tab
2. Look for error messages in the build steps
3. Common issues:
   - Gradle build errors (check `build.gradle.kts`)
   - Missing dependencies
   - Kotlin compilation errors
   - Resource conflicts

### No Artifacts Available

1. Ensure the workflow completed successfully (green checkmark)
2. Check if the artifact retention period has expired
3. Verify you're looking at the correct workflow run

### Release Not Created

1. Verify tag format is correct (`v*.*.*`)
2. Check workflow permissions (needs `contents: write`)
3. Review workflow logs for errors

## Security Notes

- Release APKs are **unsigned** by default
- For signed releases, you need to:
  1. Add keystore file to repository secrets
  2. Add signing configuration to `build.gradle.kts`
  3. Update workflow to sign APK

## Monitoring Builds

**Build Status Badges** are available in the README:
- Android CI Build status
- Release Build status
- Latest release version

## Contributing

When contributing:
1. Create a feature branch
2. Make your changes
3. Push to your branch
4. Create a Pull Request
5. CI will automatically build and test your changes
6. Artifacts will be available for testing

## Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android Build with Gradle](https://developer.android.com/build)
- [Semantic Versioning](https://semver.org/)
