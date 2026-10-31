# NobiFile — Feature Status (truthful)

Verified: static checks (XML parse, brace/import/reference checks, no-secrets grep) + GitHub Actions compile VERIFIED 2026-10-31 (V1 run 41158539455; V2 run 41163392822, success, commit f2cf30fb; see BUILD_REPORT.md). On-device: Sam installed V1 on 2026-10-31 ~08:19 IST and reported 5 bugs (see V2 below). V2 fixes them; V2 on-device verification pending — Sam's phone is the verdict.

## V2 fixes (Sam's on-device report, 2026-10-31 ~08:27 IST)

- Dark-mode text invisible (onboarding black-on-black; status-bar icons wrong contrast): root Surface with scheme background in MainActivity, explicit onBackground/onSurface in every scheme, dynamic scheme text/background pairs pinned to known-good schemes, status/navigation bar contrast set from the active scheme. Onboarding rebuilt as an explicit Surface with step icons and top-aligned layout.
- Empty-area alignment wrong (Home action buttons overflowed into a tall empty outline): Home buttons now sit in two short rows; category chips and footer buttons carry icons; all empty states are centred with an icon.
- Back button closed the app: Explorer BackHandler + top-bar Back now walk up the folder hierarchy; Internal Storage root is the top (never /storage/emulated, /storage or /); only at the root does Back go Home, and Home Back needs a double-press to exit. Explorer title shows "Internal Storage", never "0".
- Crash on sidebar click: drawer now closes before navigating, same-route taps are ignored, all route arguments are nullable with defaults, and navigation uses a crash-proof navigateSafe fallback. Database reads on Home/Search/Developer/Operations/Trash now use runCatching fallbacks instead of crashing composition.
- File-format icons (NobiCode/NobiIDE FileIconProvider semantics): exact-filename priority (AndroidManifest.xml, build.gradle, package.json, Dockerfile, README, .gitignore, .env, ...), then extension. XML = green Android robot (#AAC148, Sam's SVGRepo choice). HTML/JS/CSS/TS/JSON/MD/sh/SQL/dart/rs/go/php/C/C++/C#/SVG/CSV and document/archive/media families each get their own icon + colour. Folders get name-based tints (res amber, src violet, .git red, node_modules green, Android green). Icons added to sidebar (already present, now crash-safe), Settings sections and rows, Home sections, Developer/Diagnostics, Operations/Trash, Storage/Categories.
- Version: V2, versionCode 2, versionName 1.0.1, same fixed signing key.

## Done (implemented and compile-verified)

- Splash/onboarding, Home, drawer, Explorer (list/compact/grid/large-grid/detailed), sort, hidden toggle, breadcrumbs, tabs (persisted), dual-pane (wide screens), selection (all/invert/by type/by extension), copy/cut/paste, rename, Trash/restore/permanent, create file/folder + templates, conflict dialog, operation queue + history, properties + SHA-256 on request, share, copy path, Open With.
- Search (name, cancellable, history, saved-query history only), Categories (MediaStore), Storage analyzer (large/empty/dir-size), Recent, Favorites/Bookmarks, Collections data (create/add/entries in database).
- Viewers: image, video (play/pause), audio (in-app), PDF (render pages), text/code preview (read-only, line numbers, find, wrap, font size).
- Archives: ZIP/TAR/TAR.GZ/GZIP list/extract/create (stdlib only).
- Batch rename with preview, text file comparison, Developer workspace (project roots + read-only Git branch/log), Diagnostics (PackageManager checks only), Settings (theme/AMOLED/accent, explorer, operations, storage, privacy, About with V1 label).
- Launcher icon: generated folder mark, transparent foreground, #0B1220 background from code, per-density + v26 layers. Foreground xxxhdpi MD5: 8a594c4fadbf (first 12 chars; full hash in build verification).

## Pending / omitted (UI omitted, not faked)

- Smart Collections rules UI and custom file associations / quick-action customization screens.
- Secure Folder (omitted entirely; no fake encryption claim).
- RAR/7Z archive support (omitted; never claimed).
- SAF subfolder navigation beyond the picked tree root (file-backed browsing is primary).
- Full Git status (modified/untracked lists not computed; branch/log only, labelled).
- Video speed/subtitles/audio selection and PDF text search/selection (via Open With instead).
- Background audio playback (in-app only; not claimed).
- V2 on-device verification (pending; Sam's phone is the verdict). GitHub Actions compile is verified — see BUILD_REPORT.md.

## Docs

ARCHITECTURE.md, STORAGE_ARCHITECTURE.md, INTEGRATION_PROTOCOL.md, TESTING_REPORT.md, BUILD_REPORT.md in this folder.
