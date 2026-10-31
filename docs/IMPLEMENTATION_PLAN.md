# NobiFile — Implementation Plan

Source: Sam's Master Specification (96-screen inventory) + approved SPEC.md in this folder.
Approach: reusable Compose screens/dialogs/states, NOT 60 Activities. Single :app module,
modular packages. Build order: scaffold first (green build), then core, then discovery,
viewers, archives, advanced, developer, ecosystem, finalization.

## Phases

- Phase 1 Foundation: Gradle stack, theme, navigation, SQLite + DataStore, storage abstraction, permissions/onboarding. DONE in local source (static verification only).
- Phase 2 Core: Home, drawer, Explorer, sort/view, icons, tabs (persisted), dual-pane. DONE in local source.
- Phase 3 Operations: selection, copy/cut/paste, rename, delete/Trash, create, conflicts, queue/history. DONE in local source.
- Phase 4 Search + discovery: global/advanced search, categories, recent/favorites/collections (collections: create/add/view via database; collection UI is via favorites/collections data — see FEATURE_STATUS for the exact surface), storage analyzer. DONE in local source except Smart Collections rules UI (pending, see FEATURE_STATUS).
- Phase 5 Viewers: image, video, audio, PDF, text/code. DONE in local source with stated limits.
- Phase 6 Archives: ZIP/TAR/TAR.GZ/GZIP browse/extract/create with stdlib only. DONE in local source.
- Phase 7 Advanced: tabs, dual-pane, compare, batch rename. DONE in local source. Smart Collections rules UI + custom associations/quick-actions: PARTIAL (data exists, dedicated rules UI pending).
- Phase 8 Developer: project roots, Git read-only, config/logs guidance. DONE in local source.
- Phase 9 Ecosystem: Diagnostics only, honest unverified integration. DONE in local source.
- Phase 10 Finalization: Settings, accessibility, performance, testing, build report. Settings DONE. Compilation/testing via GitHub Actions PENDING (no SDK on VM).

## Auto-restart rule

Sam ordered (2026-10-31 ~03:04 IST): if a build fails in the middle, restart/fix it without waiting for approval. Apply to every Actions failure: read the exact error, fix only the named location first, rebuild, re-verify. Never wait for approval between fix cycles.
