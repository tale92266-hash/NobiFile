# NobiFile — Storage Architecture

Unified `FileItem` identity: name, displayPath, file (nullable), treeUri, documentId,
isDirectory, size, lastModified, hidden, canRead/canWrite, mimeType, provider
(`file` or `saf`). No item pretends to have a path it does not have.

- LocalFileProvider (in `StorageRepository`): `java.io.File` for permitted paths.
  Listing, sorting, search, size analysis and operations use streaming buffered I/O.
- SafDocumentProvider: `DocumentFile` for user-picked trees with persisted URI
  permissions (`takePersistableUriPermission`). Listing only at the picked root in
  v1; subfolder navigation for SAF trees is a known limit (see FEATURE_STATUS).
  Cross-provider moves use copy-then-verify-then-delete, never assumed atomic rename.
- MediaStoreProvider: read-only discovery for categories. Results are labelled as
  indexed discovery and may omit unindexed files; full browsing uses the filesystem.
- Permissions: `MANAGE_EXTERNAL_STORAGE` (All-files access) + media permissions +
  SAF picker. When denied, only permitted locations work and every unavailable
  action explains why. No root, no bypass, no hidden access.
- FileProvider (`com.nobifile.manager.fileprovider`) for secure sharing/opening.
  URI grants are per-intent (`FLAG_GRANT_READ_URI_PERMISSION`).
- Trash: same-volume `.NobiFileTrash` folder + SQLite metadata (original path,
  deleted time). Restore moves back; SAF items are permanent-delete only, labelled.
