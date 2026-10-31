# NobiFile

Professional developer file manager for Android. Native Kotlin + Jetpack Compose + Material 3.

- Package: `com.nobifile.manager`
- Version: 1.0.0 (versionCode 1) • Build label: V1
- Stack: Gradle 8.11.1, AGP 8.9.2, Kotlin 2.2.21, compileSdk 35, minSdk 30, targetSdk 35
- App UI text is English throughout.

## What it does

Browse Internal Storage and SAF-picked folders, copy/cut/paste with conflict handling,
rename, Trash/restore, create files and folders, search, categories via MediaStore,
storage analysis, image/video/audio/PDF/text-code previews, ZIP/TAR/GZIP archives,
tabs, dual-pane, batch rename, file comparison, developer workspace (read-only Git
metadata), connection diagnostics, and full Settings. No WebView for core UI.
No root access. No Android restriction bypass.

## Build

Local builds use the default debug signing key. GitHub Actions builds use a fixed
signing key from Secrets only (see BUILD_REPORT.md after the first Actions run).
This VM has no Android SDK, so compilation must be verified via GitHub Actions.

```
./gradlew :app:assembleDebug
```

## Layout

- `app/src/main/java/com/nobifile/manager/` — UI, data, storage, operations, icons
- `docs/` — implementation plan, status, architecture, storage, integration, testing, build reports
- `SPEC.md` — approved product spec

## Honest limits (v1)

- SAF subfolder navigation is limited to the picked tree root listing; file-backed
  browsing is the primary path.
- Video preview plays/pauses only; full speed/subtitle control is via Open With.
- PDF preview renders pages without text search/selection; full readers via Open With.
- Git screen is read-only metadata only; it does not compute modified/untracked lists.
- Nobi Code / Nobi Terminal direct open-in-app is NOT claimed until verified on
  device; Diagnostics reports only what PackageManager can verify.
- Secure Folder is omitted entirely in v1 — no fake hidden-equals-encrypted folder.
- RAR/7Z archives are not supported and are never claimed.
