# NobiFile — Testing Report (local static checks only)

Date: 2026-10-31. Environment: VM with no Android SDK, so NO compilation,
no instrumentation test and no on-device test has run yet. Compilation must
be verified via GitHub Actions; on-device behaviour must be verified by Sam.

Static checks run locally (see verification script output kept with the build
report when Actions runs):
- Strict XML parse of every res XML file and the manifest.
- Kotlin brace/paren balance check (quote-aware) for every .kt file.
- Import/reference check: every referenced string/drawable/layout/xml/file_paths
  resource exists; no duplicate resource names; every Kotlin import resolves to
  a project type or a declared dependency.
- Grep for TODO/FIXME/mock/fake/sample/placeholder and for secrets/passwords/
  tokens — must be empty in source (honest-limit comments excepted).
- Launcher icon pixel checks: foreground corners transparent, background centre #0B1220.

Manual test plan for Sam's phone (after the first Actions APK):
- Open app, grant/deny storage, browse Internal Storage + Downloads, nested folders.
- Long-press selection, Select All/Invert, copy/paste, cut/move, rename, Trash/restore,
  permanent delete, create file/folder, conflict dialog (Replace/Skip/Rename).
- Search, categories, storage analyzer, tabs, dual-pane (landscape/tablet if available).
- Viewers: image, video, audio, PDF, text/code. Archives: ZIP create/extract.
- Batch rename preview, compare two text files, Developer Git inspect, Diagnostics.
- Large folder scrolling, Unicode/long names, hidden files, low storage, interrupted copy.
