# Custom Storage plan — NobiFile (Sam's Material Files comparison)

Sam's brief (2026-10-31, screenshot of Material Files): NobiFile has no way to
add a custom storage, the way Material Files shows an "Add storage" sheet with
Android/data, Android/obb, External storage, FTP server, SFTP server, SMB
server and WebDAV server. He asked whether NobiFile can be built on Material
Files' code with a different UI, direct Terminal + Nobi Code support, Material
Files branding replaced everywhere, NobiCode format icons for every file, image
and video thumbnails, and all of NobiFile's features — rebranded as his app
with his colours and icons.

Decision to confirm with Sam (options in the delivery message): build all of
this as NobiFile, in NobiFile's own code, under the NobiFile name — NOT a copy
of Material Files' code. Material Files is open source (Apache 2.0) and a fork
is legally possible only with its copyright notices and licence kept; Sam's
"har jagah se Material Files hatakar" (strip it everywhere) would break that,
and a fork would also throw away everything built in NobiFile V1–V4 (tabs,
dual-pane, Trash, archives, batch rename, developer workspace, diagnostics,
the fixed signing key and the verified build pipeline). The features below
give Sam the same "Add storage" experience on his own codebase, with his
launcher icon, his NobiCode format icons and his theme colours everywhere.

## What appears where, and what happens on click

- A new "Add storage" entry in the sidebar, above Settings, with a storage
  icon. Tapping it opens a bottom sheet (not a full screen, so the folder
  behind stays visible), titled "Add storage", listing the seven options with
  a leading icon each: Android/data (folder icon), Android/obb (folder icon),
  External storage (SD-card icon), FTP server (cloud icon), SFTP server
  (shield icon), SMB server (computer icon), WebDAV server (globe icon).
  Each row also carries a one-line subtitle explaining what it needs, for
  example "Needs the system folder picker" for Android/data.
- Tapping Android/data or Android/obb opens the Android system folder picker
  (SAF, ACTION_OPEN_DOCUMENT_TREE) pre-positioned at that folder. The user
  grants access once; NobiFile stores the granted URI persistently
  (takePersistableUriPermission) and the new location appears in the sidebar
  as a normal location with the folder tint, browsable, searchable and
  operable exactly like Internal Storage. No root, no bypass, no
  Shizuku/ADB trick: if the system picker refuses, the row explains why in
  plain English and nothing is faked. This is the same mechanism Material
  Files uses for these two rows, and the same mechanism Android itself
  offers to every file manager.
- Tapping External storage lists every mounted volume (primary + SD card /
  USB) with free space, and adds the chosen one to the sidebar.
- Tapping FTP / SFTP / SMB / WebDAV opens a small form sheet: server name
  (shown in the sidebar), host, port (filled with the standard default:
  FTP 21, SFTP 22, SMB 445, WebDAV 80/443), username, password, and for
  SFTP an optional key-file picker. A "Test connection" button checks the
  server before saving, and shows the real error (wrong password, host
  unreachable) instead of a silent failure. Saved servers appear in the
  sidebar with their protocol icon and a coloured dot (green = reachable
  last time, grey = not checked). Credentials are stored in the app's
  private encrypted preferences, never in plain text, never in the repo.
- Inside a remote location, the Explorer keeps the same look: same rows,
  same NobiCode format icons (Android robot for XML, orange HTML icon,
  Javascript icon, and so on), same thumbnails where the server provides
  them, same long-press selection, same Copy/Cut/Paste/Rename/Delete
  (delete goes to a remote-side move-to-trash only where the protocol
  supports it; otherwise the confirm dialog says so honestly). Copying
  between a remote location and Internal Storage downloads/uploads through
  the existing operation queue with progress and cancellation, and the
  source is deleted only after the transfer verifies complete — the same
  safety rule NobiFile already applies to local copy/move.
- Direct Terminal and Nobi Code support, from every location including
  remote ones where a local path exists: the row's "..." menu and the
  selection toolbar keep "Open in Terminal" and "Open in Nobi Code". For a
  local folder these open the partner app at that folder (only if the
  partner app is installed and its intent resolves — Diagnostics already
  reports this truthfully and must keep doing so). For a remote location
  the two actions are shown disabled with the reason ("Remote folders have
  no local path"), never as a fake success.

## Icons, colours, thumbnails

- Every file row keeps the NobiCode format-icon mapping (the resolver Sam
  approved in V2), extended to folder tints by name, exactly as today. The
  new storage rows in the sidebar reuse the same icon set; no new icon
  language is invented for this feature.
- Image and video thumbnails (V4) stay on everywhere, including category
  views and search results. Remote thumbnails are generated only when the
  file has been downloaded for preview; listing a remote folder never
  downloads every file just to draw thumbnails (that would be slow and
  expensive on mobile data — the same restraint Material Files and Solid
  Explorer apply).
- Theme colours stay NobiFile's current Material 3 light/dark schemes; the
  new sheets follow the same surface/elevation and the same English UI text.

## What professional apps do, and where NobiFile differs honestly

- Material Files and Solid Explorer both offer the same seven storage types
  and the same SAF flow for Android/data + Android/obb. NobiFile matches
  that list one-for-one, but keeps its own tab/dual-pane model on top:
  a remote location can sit in a tab, and in dual-pane a remote folder can
  face a local folder for drag-free transfer.
- FTP/SFTP/SMB/WebDAV each need a maintained network library (FTP: Apache
  Commons Net; SFTP: an SSH library; SMB: an SMB client; WebDAV: an HTTP
  DAV client). These are the only heavy dependencies NobiFile would take
  on, and they arrive one protocol at a time, each behind its own storage
  provider behind the existing `StorageRepository` abstraction, so a broken
  protocol can never take down local browsing. This is Phase 2 below; Phase
  1 needs no new dependency at all.

## Phases (build order, each verified before the next)

- Phase 1 — Local + SAF: sidebar "Add storage" sheet, Android/data,
  Android/obb via the system picker with persistent grants, External
  storage volumes, sidebar persistence, Diagnostics entries for each added
  location. No new libraries.
- Phase 2 — Remote, one protocol at a time: FTP first (simplest), then
  SFTP, then SMB, then WebDAV. Each ships with Test connection, sidebar
  presence, browsing, and queue-based transfer. A protocol that fails its
  on-device check is reported, not claimed.
- Phase 3 — Polish: connection-dot reachability, reconnect on wake,
  remote rename/delete honesty per protocol, and the disabled-reason
  labels for Terminal/Nobi Code on remote paths.

## Open question for Sam

- Name: Sam wrote "NobiCode" in the rebrand line. NobiCode is already his
  editor (package com.nobicode.editor). This plan assumes he meant
  NobiFile (the file manager, package com.nobifile.manager) — confirm in
  the delivery message options. If he truly wants a different name, the
  package and signing key decision comes first, before any build.
