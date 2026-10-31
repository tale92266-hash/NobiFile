# NobiFile — Feature Status (truthful, local source only)

Verified: static checks only (XML parse, brace/import/reference checks, no-secrets grep).
Compilation on GitHub Actions: NOT YET RUN. Nothing here claims the APK builds or installs.

## Done in local source (implemented, not yet compile-verified)

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
- GitHub Actions compile + on-device verification (pending; Sam's phone is the verdict).

## Docs

ARCHITECTURE.md, STORAGE_ARCHITECTURE.md, INTEGRATION_PROTOCOL.md, TESTING_REPORT.md, BUILD_REPORT.md in this folder.
