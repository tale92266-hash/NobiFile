# NobiFile — Product Spec (for Sam's approval before building)

Status: DRAFT — build starts only after Sam approves this spec + the 5 decisions at the end.
Date: 2026-10-31. Source: Sam's Master Specification (96-screen inventory, Modules A–M, Sections 1–30).
Rule applied: Sam's standing spec rule — detail first, approval first, build after.

## 1. What NobiFile is, and what it is not

NobiFile is a native Android file manager and the shared filesystem layer of the Nobi
ecosystem. Nobi Code edits files, Nobi Terminal runs commands, NobiFile manages
everything on disk. The three apps stay independently installable.

It is NOT a basic file browser, NOT a WebView wrapper, and NOT 60 disconnected
Activities. The master spec's 60+/96 screens are delivered as a small number of
reusable native screens + dialogs + bottom sheets + viewers, each with list/grid,
empty, loading, error and selection states. That is exactly what Section 30 of the
master spec asks for, and it is the only way the app stays fast and maintainable.

App UI text is English throughout. Chat with Sam is Hinglish. Version label is
always visible (Settings > About, and on the splash), and increments every delivery.

## 2. What Sam already has — verified before writing this spec

- Nobi Terminal: package `com.nobiterminal` (verified in decoded manifest, v21).
  It exposes a DocumentsProvider (`com.nobiterminal.documents`), a FileReceiver /
  RUN_COMMAND surface and SEND handling. No verified launcher intent that opens a
  terminal session at a chosen working directory was found in the manifest inspected.
- Nobi Code: package `com.nobicode.editor`, version 1.13.16 (verified in
  `config.xml`). It is a Cordova hybrid app. No verified ACTION_VIEW / project-folder
  intent contract was found in the local source inspected.
- Consequence (honest, per master spec §1.2): NobiFile v1 must NOT claim
  "Open in Nobi Code / Open in Nobi Terminal" works until it is verified on device.
  Phase 9 below is a verification phase, not a promise. The Diagnostics screen
  (§7) reports installed / not-installed and whether an intent resolves, so Sam
  sees the truth instead of a fake integration.

## 3. Proposed stack and build route (proven, not new)

Same stack as SmartDialer2, which already builds cleanly on GitHub Actions:
Kotlin + Compose + Material 3, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.2.21,
compileSdk 35, minSdk 30, targetSdk 35, `gradle-wrapper.jar` master copy included.
Single Activity + Compose Navigation + ViewModel. Room for local metadata only
(bookmarks, recents, collections, tabs, operation history, project roots) — never
file contents. Coroutines + ExecutorService for I/O, never main-thread I/O.

Build route: new public GitHub repo `tale92266-hash/NobiFile`, GitHub Actions,
NEW fixed signing key stored only in GitHub Secrets (never in the repo), complete
debug APK delivery (not a small R8-shrunk build, per Sam's standing order).
Every delivery reports package, versionName/versionCode, APK size + SHA-256, and
the signing-certificate fingerprint, and Sam's on-device test is the verdict.

Local copy rule: full project copy stays in `~/workspace/NobiFile/` (this folder)
with its git history. No direct edits on the phone. Phone sync only when Sam
says so clearly.

## 4. Storage architecture — the decision that decides honesty

One unified model, two providers behind it:

- `LocalFileProvider` — `java.io.File` for paths the app is actually allowed to
  use. Every item carries: display name, URI/path identity, MIME type, size,
  modified date, capability flags (readable / writable / deletable / renamable),
  and provider id. No item pretends to have a path it does not have.
- `SafDocumentProvider` — Storage Access Framework `DocumentFile` for SD card /
  USB / user-picked trees, with persisted URI permissions. Cross-provider
  copy/move is copy-then-verify-then-delete, never an assumed atomic rename.
- `MediaStoreProvider` (read-only discovery) for Categories (Images / Video /
  Audio / Documents) — clearly labelled as indexed discovery, not a full
  filesystem scan, per master spec §10/§15.

Permission model proposed (Decision 2 below): `MANAGE_EXTERNAL_STORAGE`
(All files access) requested with a plain-English in-app explanation, PLUS the
SAF picker for removable storage, PLUS graceful degradation when access is
denied (only app-private + picked trees work, every other action says why it is
unavailable instead of failing silently). Reason: SAF-only would make rename /
move / batch operations across ordinary folders unreliable and would force fake
success states, which the master spec forbids. Sam distributes by sideloaded
APK, not Play Store, so the Play-policy justification problem does not apply
here; if Play distribution is ever wanted, this decision must be revisited.
No root access is requested, ever. No security restriction is bypassed.

## 5. Screen map — how 96 inventory items become reusable screens

- `Splash + Onboarding` (01–05): splash, welcome, permission explanation,
  storage-access setup, setup-complete. First run only; state restored after.
- `Home` (06–11): search bar, storage summary, recent files, frequent folders,
  quick access, categories, bookmarks, active operations chip. One screen,
  sections driven by Room + MediaStore.
- `Explorer` (12–23, 71–77 in part): ONE reusable screen with list / compact /
  grid / large-grid / detailed modes, sort (name, date, size, type, extension),
  folders-first toggle, hidden-files toggle, breadcrumbs, tabs support, and
  filtered states for Large Files, Empty Folders, Duplicates, Recently Modified.
  Nested navigation is a back-stack of Explorer states, not new Activities.
- `Sidebar / Drawer` (24–29): drawer + rail on large screens, custom locations
  added via SAF picker, rename / reorder (drag) / hide / remove shortcut
  (removing a shortcut never deletes the folder), long-press context menu.
- `Operations` (30–42): bottom-sheet context menu, multi-select toolbar,
  Select All / Invert / By Type / By Extension / By Date / By Size, internal
  clipboard (Copy vs Cut, persistent indicator, cut items shown dimmed, original
  kept until a move verifies), rename dialog (single + batch preview),
  delete confirmation (trash vs permanent, clearly labelled), operation queue
  screen (active / waiting / done / failed, progress, speed, ETA when known,
  retry), conflict dialog (Replace / Skip / Rename incoming / Keep both /
  Apply to all — never overwrite without explicit tap), operation history.
- `Create` (43–47): New menu (folder, file, text/markdown/HTML/CSS/JS/JSON/XML/
  Java/Kotlin/Python/shell + custom extension), create-folder and create-file
  dialogs with validation + conflict check, batch folder creation, starter
  templates for code files (empty + minimal skeleton, clearly labelled).
- `Viewers` (48–55): image viewer (zoom/pan/rotate/slideshow), video player,
  audio player, PDF viewer, text viewer (search, wrap, font size, encoding
  where practical, read-only), code preview (syntax highlight + line numbers +
  Open in Nobi Code button — preview only, not an editor), APK info
  (name/package/version/SDKs/permissions/cert when available, install only via
  the system installer), file properties (name, MIME, size, dates, provider,
  capabilities, hash on request only).
- `Archives` (56–60): ZIP/TAR/TAR.GZ/GZIP browsing without full extraction,
  extract dialog (here / new folder / destination, overwrite policy), create
  archive (ZIP/TAR/TAR.GZ). RAR and password claims only if the chosen library
  actually supports them — otherwise the option is absent, not fake. Library
  choice is fixed at Phase 6 after a licence check, not assumed now.
- `Search` (61–65): global + advanced (name, extension, size/date range, type,
  location, hidden), cancellable, results support sort + selection + bulk
  actions, saved filters, local search history with clear.
- `Storage` (66–70): dashboard (total/used/free from `StatFs` on accessible
  volumes), category breakdown (labelled indexed vs filesystem), analyzer
  (large files/folders, old files, duplicate candidates, empty folders),
  async directory-size with progress + cancel, cleanup suggestions that NEVER
  auto-delete.
- `Developer` (71–77): dashboard, registered project roots, Git metadata display
  ONLY where `.git` is readable (branch, modified/untracked file names — no fake
  Git operations without a Git implementation), language/extension filter,
  environment directories, logs viewer, config-file quick access
  (JSON/XML/YAML/TOML/properties/.env names).
- `Ecosystem` (78–84): Nobi Apps Hub, Open with Nobi Code, Open in Nobi
  Terminal, Shared Workspace (project roots accessible to all three apps),
  integration settings, cross-app activity (only what can be reliably observed),
  Connection Diagnostics (installed?, package identity, intent resolves?,
  URI grant available?, protocol version). See §2 honesty note.
- `Settings` (85–96): appearance (Light/Dark/System/AMOLED, dynamic colour
  where available, accent, density), explorer defaults, operations defaults,
  storage locations, search, sidebar, icons/thumbnails, performance
  (cache size, concurrency, indexing), privacy (clear recents/search history),
  integration, About (version, build, credits, licences, diagnostics).

Advanced (master §13, Phase 7): dual-pane, tabs with restore, file/folder
comparison (text side-by-side + tree diff by name/size/date/hash), batch-rename
engine with live preview, smart collections by rule, app-managed Trash with
original-location metadata + restore, saved searches, custom file associations,
custom quick actions. Secure Folder (Keystore + authenticated encryption) is
Phase 7+ only, and only if the crypto design is reviewed — it is never a
"hidden folder" pretending to be encryption.

## 6. Icons — following Sam's standing icon rule, without inventing artwork

No artwork was supplied for NobiFile. Standing rule: transparent foreground,
background/shape/colour from code, remove baked square/frame outlines, never
redesign supplied artwork, replace everywhere. Applied here as:

- ONE consistent folder vector for every folder; state shown only by a small
  badge (hidden / empty / read-only / project), never a different folder drawing.
- `FileIconResolver` (single, used by every screen): vector glyph + coloured
  chip carrying the extension text (e.g. KT, JSON, ZIP). This covers all 80+
  formats in the master spec §6.2 without shipping 80 PNGs that bloat the APK
  and break across densities. Unknown extension = universal icon + extension
  label, never a crash. If Sam later supplies format artwork, it replaces the
  vector glyphs under the same resolver, artwork unchanged.
- Launcher icon: Decision 5 below — Sam supplies artwork, or approves a
  generated folder+N mark built to the same rule (transparent foreground at
  ~56% of the adaptive canvas, per-density v26 layers, solid background from
  code). No launcher icon is shipped on a guess.

## 7. Performance and safety gates (master §14, §17, §20, §27)

No main-thread filesystem I/O. Async/paged directory loading with cancellation
and scroll-position restore. RecyclerView-equivalent lazy lists (Compose
LazyColumn/LazyGrid) with stable keys. Bounded thumbnail cache (memory + disk,
sampled decode, request dedup + cancel, no full-res decode for a thumbnail).
Streaming buffered copy/move with progress, cancel, partial-transfer cleanup,
integrity check where practical, original preserved until a cross-provider move
verifies. Central error handling with plain-English messages + recovery action
(permission denied, not found, storage full, exists, invalid name, read-only,
corrupt archive, disconnected storage, lost URI permission, cancelled, partial).
Back order: dialog > sheet > selection > search > parent folder > exit prompt.
Accessibility: TalkBack labels on every icon-only action, minimum touch targets,
selection state announced. Notifications only for long operations, cancellable
where safe, no permanent foreground service.

## 8. Phases and first deliverable

- Phase 1 Foundation: project, design system, navigation, Room, storage
  abstraction, permission flow, diagnostics skeleton.
- Phase 2 Core: Home, Sidebar, Explorer, sort/view, icons, tabs skeleton.
- Phase 3 Operations: selection, copy/cut/paste, rename, delete/trash,
  create, conflicts, queue, history. **Phase 1–3 together = first APK (MVP).**
  Sam tests browsing + real operations on device before anything else is built.
- Phase 4 Search + Categories/Recent/Favorites/Collections + Storage analyzer.
- Phase 5 Viewers. Phase 6 Archives. Phase 7 Advanced (dual-pane, tabs
  restore, compare, batch rename, trash, smart collections).
- Phase 8 Developer workspace. Phase 9 Ecosystem integration verification
  (see §2). Phase 10 Settings polish, performance, accessibility, full testing
  (Android 11–16, huge folders, Unicode/long names, hidden files, removable
  storage disconnect, low storage, interrupted ops), docs + build report.
  Docs kept live: IMPLEMENTATION_PLAN, SCREEN_INVENTORY, FEATURE_STATUS,
  ARCHITECTURE, STORAGE_ARCHITECTURE, INTEGRATION_PROTOCOL, TESTING_REPORT,
  BUILD_REPORT — updated as work progresses, never marked complete from a
  screen existing alone.

No overnight-finish promise: work continues phase by phase until the build and
quality gates actually pass and Sam verifies on device.

## 9. Decisions needed from Sam (reply by number, change any default)

1. Package + build route: `com.nobifile.manager`, new repo `tale92266-hash/NobiFile`,
   GitHub Actions, NEW fixed signing key in Secrets, complete debug APK — OK?
   If Sam wants a different package name, say it now; renaming later breaks
   install-over updates.
2. Storage permission: All-files access + SAF picker + graceful fallback (see §4) — OK?
3. MVP scope: first APK = Phases 1–3 (browse + real operations), viewers/archives/
   advanced later in that order — OK, or does Sam want viewers/archives in the
   first APK too?
4. Icons: vector + extension-chip resolver (see §6), no 80 PNGs in v1 — OK?
5. Launcher icon: Sam will supply artwork, or approve a generated folder mark
   under the standing icon rule? If supplying, send the image; if generated,
   say "generate karo".
