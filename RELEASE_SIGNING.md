# Signed Release APK & GitHub Actions Guide

Mobile Code Studio builds and verifies a signed release APK (`MobileCodeStudio-release.apk`) via `.github/workflows/android-release.yml`.

## 1. Create One Permanent Release Keystore (Once Only)

Run this **once** on your local machine and store the `.jks` file in a secure backup location (never commit it to Git):

```bash
keytool -genkeypair -v \
  -keystore mobile-code-studio-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias mobilecodestudio
```

Reuse this exact keystore and alias for all future updates so Myket and Android preserve update compatibility.

## 2. Encode Keystore to Base64

- **Linux / macOS:**
  ```bash
  base64 -w 0 mobile-code-studio-release.jks > keystore.b64.txt
  ```
- **Windows (PowerShell):**
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("mobile-code-studio-release.jks")) | Set-Content keystore.b64.txt
  ```

## 3. Configure GitHub Actions Secrets

In your GitHub repository, open **Settings → Secrets and variables → Actions** and add these four repository secrets:

| Secret Name | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Contents of `keystore.b64.txt` (single-line Base64 string) |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore store password |
| `ANDROID_KEY_ALIAS` | Key alias (e.g., `mobilecodestudio`) |
| `ANDROID_KEY_PASSWORD` | Key password |

Delete `keystore.b64.txt` after saving the secrets.

## 4. Trigger a Signed Build & GitHub Release

- **Workflow artifact build:** Push to `main`/`master` or trigger **Build Signed Release APK** via `workflow_dispatch`. Download `MobileCodeStudio-release.apk` from the **MobileCodeStudio-release-apk** artifact.
- **Versioned GitHub Release:** Push a version tag (e.g., `v1.0.0`):
  ```bash
  git tag v1.0.0
  git push origin v1.0.0
  ```
  The workflow runs `assembleRelease`, verifies the signature with `apksigner verify`, and attaches `MobileCodeStudio-release.apk` with version and build metadata to the GitHub Release.
