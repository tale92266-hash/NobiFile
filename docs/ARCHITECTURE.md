# NobiFile — Architecture

Single Activity (`MainActivity`), Compose Navigation (`AppShell` + `Dest` routes),
one reusable `ExplorerScreen`/`ExplorerPane` driven by `ExplorerViewModel`.
No empty Gradle modules; modular packages under `com.nobifile.manager`:

- `ui/theme` — Material3 schemes (Light/Dark/AMOLED/dynamic + accent).
- `ui/navigation` — route definitions with encoded arguments.
- `ui/components` — `FileRow`, empty/error states (single icon resolver usage).
- `ui/screens` — Home, Explorer (+ViewModel), Search, Storage/Categories, Developer/Diagnostics, Operations/Trash, Settings, Viewers, Archive/Compare, AppShell/Onboarding.
- `data` — `Models`, `AppDatabase` (SQLiteOpenHelper, 10 tables, v1, no Room/KSP to avoid KSP version risk), `PrefsStore` (DataStore Preferences).
- `storage` — `StorageRepository` (File/SAF/MediaStore), `ThumbnailCache` (bounded LRU, sampled decode).
- `ops` — `OperationManager` (clipboard, copy/move/delete/rename/create, Trash, SHA-256, verify-before-delete), `ArchiveOps` (stdlib ZIP/TAR/GZIP), `BatchRename` + `TextCompare`.
- `icons` — `FileIconResolver` (single resolver, vector + chip).

State: StateFlow in ViewModels + DataStore flows in the Activity theme. I/O on
Dispatchers.IO only; no main-thread filesystem I/O. Operations are app-scoped
(StateFlow) so rotation does not lose progress reporting. Errors are surfaced
with plain-English messages and a retry/recovery action; nothing is silently ignored.
