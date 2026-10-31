# NobiFile — Build Report

## Local source (2026-10-31)

- Project created locally at ~/workspace/NobiFile. Gradle stack: 8.11.1 / AGP 8.9.2 /
  Kotlin 2.2.21 / compileSdk 35. Package com.nobifile.manager, versionCode 1,
  versionName 1.0.0, build label V1.
- Launcher icon: generated folder mark (PIL). Foreground xxxhdpi MD5 prefix: 8a594c4fadbf.
  Background centre pixel verified #0B1220. Foreground corners transparent (alpha 0).
- Compilation: NOT VERIFIED LOCALLY (no Android SDK on this VM).
  Static verification PASSED 2026-10-31: 26 Kotlin files, 4105 lines, 0 XML/brace/
  balance/icon/wrapper/secret-check failures (verifier /tmp/verify_nobifile.py).
  GitHub Actions build: PENDING — repo creation and push are separate steps after
  local source completion, per Sam's order (local first, then repo + push).

## GitHub Actions (to be filled after the first run)

- Repo: https://github.com/tale92266-hash/NobiFile (pending)
- Run URL: (pending)
- APK: package, size, SHA-256, MD5, cert fingerprint: (pending — verify with
  keytool -printcert -jarfile and apktool/aapt2 dump before delivery)
- Signing: fixed key in GitHub Secrets only; debug signingConfig reads
  NOBIFILE_* env vars with fallback to default debug signing for local builds.
  No keystore/password/token is committed to the repo or printed in any report.
