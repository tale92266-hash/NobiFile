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

## GitHub Actions — VERIFIED 2026-10-31 (run 41158539455, success)

- Repo: https://github.com/tale92266-hash/NobiFile (HEAD 643a0062, 87 files, workflow present, no keystore files)
- Run URL: https://github.com/tale92266-hash/NobiFile/actions/runs/41158539455 — all 8 steps success (checkout, JDK 17, gradlew, keystore restore, build, cert print, upload).
- Build fixes applied on the way (standing fix rule, no waiting): (1) compose compiler
  plugin version — root build.gradle.kts now declares
  `id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false`
  (run 41157896391 failure); (2) `com.google.android.material:material:1.12.0`
  dependency for the `Theme.Material3.DayNight.NoActionBar` XML parent
  (run 41158061236 failure); (3) signing key regenerated with ONE password for
  store and key — keytool IGNORES a different -keypass for PKCS12 keystores
  ("Different store and key passwords not supported"), so the first key's
  separate key password made AGP fail with "Given final block not properly
  padded" (run 41158213275). The first key (fingerprint 73:67:5B:...) was
  replaced and must never be used; the fixed key below is the only key.
- APK (downloaded from the run artifact `NobiFile-debug-apk`, verified locally):
  package `com.nobifile.manager`, versionCode 1, versionName 1.0.0, label V1,
  MainActivity `com.nobifile.manager.MainActivity`, compileSdk 35, debuggable.
  Size 63,166,866 bytes (full debug APK, 929 entries, 11 dex files — NOT an R8 build).
  SHA-256 `ddc7e456e1f8177713849d8737175fc11679e04b6301721e599fcd0d2d3a613a`,
  MD5 `dc838ec9718a557e9c0c038ff9a3b277`.
- Signing: fixed key in GitHub Secrets only (alias `nobifile`, PKCS12, CN=NobiFile).
  Cert SHA-256 `59:D4:AD:DE:63:73:9A:F6:92:4D:D6:FA:B7:62:45:4B:4E:2E:01:97:DE:2A:D7:DB:19:0F:58:36:F0:3B:B2:C6`
  (verified from the APK signing block certificate, DER-parsed with openssl;
  keytool -printcert does not read v2-only signatures). No keystore/password/
  token is committed to the repo or printed in any report.
- On-device verification: PENDING — Sam's phone test is the verdict (install,
  first open, storage permission, file list, viewers, archives).
- Honesty note: compile is verified; the done-vs-pending list in FEATURE_STATUS.md
  stands — Smart Collections rules UI, custom associations, Secure Folder, RAR/7Z,
  SAF subfolder navigation beyond the picked tree, full Git status, video/PDF extras,
  background audio, and the Nobi Code / Nobi Terminal integrations are NOT done
  and must not be claimed. Diagnostics only reports installed/not-installed and
  intent resolution; files open via the system Open With chooser.

## GitHub Actions — V2 VERIFIED 2026-10-31 (run 41163392822, success)

- Commit f2cf30fb (after V2 source commit e0e47e0f and two icon-import fix commits b643c05f, f2cf30fb). VersionCode 2, versionName 1.0.1, label V2, same fixed signing key (cert SHA-256 59:D4:AD:DE:...:B2:C6, CN=NobiFile).
- APK: 63,281,554 B, 929 entries, 11 dex, SHA-256 3a0f7b4e490704b33eff7ff9ac649af2eb010c5571ce3256e5b22f0366f5de2d, MD5 19ff65cc8a25708a6edd86d8d52902e1. Package/version/compileSdk verified from the binary manifest; cert verified by DER scan of the v2 signing block.
- Build failures on the way (standing fix rule, no waiting): (1) run 41162738698 — `Icons.Rounded.FolderOpen` unresolved in this icons set; (2) run 41163035161 — fully-qualified `Icons.Rounded.Folder` path does not resolve; icon properties live in the `androidx.compose.material.icons.rounded` package and need a star/explicit import (see AGENTS.md lesson). Fix: explicit `import ...rounded.Folder` + short form in FileRow.kt. On-device verification pending — Sam's phone is the verdict.

## GitHub Actions — V3 VERIFIED 2026-10-31 (run 41163988844, success)

- Commit c9ebbd75. VersionCode 3, versionName 1.0.2, label V3, same fixed signing key (cert SHA-256 59:D4:AD:DE:...:B2:C6, CN=NobiFile).
- APK: 63,692,671 B, 930 entries, 11 dex, SHA-256 1ef01e1766dc04056d7e8504fc401d3315b4177f629d0c6e4fd6082a9ec04cae, MD5 d0d23ebb89d92e412fb0c8200249e2be. Launcher foreground (xxxhdpi) MD5 4ca123c8416aa62f8edc15d37967187a matches Sam's supplied artwork; in-app logo drawable present in resources. Package/version/compileSdk verified from the binary manifest; cert verified by DER scan of the v2 signing block.
- Icon work: Sam supplied a composite sheet (checkerboard panel + usage examples). The big folder artwork was cropped from the left panel, the baked checkerboard removed by background-class flood fill (enclosed gloss kept), disconnected label-chip sliver dropped by keeping the largest foreground component, artwork unchanged. On-device verification pending — Sam's phone is the verdict.
