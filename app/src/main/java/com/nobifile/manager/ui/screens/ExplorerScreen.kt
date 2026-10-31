package com.nobifile.manager.ui.screens

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nobifile.manager.NobiFileApplication
import com.nobifile.manager.data.ConflictChoice
import com.nobifile.manager.data.FileItem
import com.nobifile.manager.data.SortOrder
import com.nobifile.manager.data.ViewMode
import com.nobifile.manager.data.formatSize
import com.nobifile.manager.data.isCodeExtension
import com.nobifile.manager.data.isTextExtension
import com.nobifile.manager.data.mimeForExtension
import com.nobifile.manager.ops.ArchiveOps
import com.nobifile.manager.ops.BatchRename
import com.nobifile.manager.ops.RenameRule
import com.nobifile.manager.storage.StorageRepository
import com.nobifile.manager.ui.components.EmptyState
import com.nobifile.manager.ui.components.ErrorState
import com.nobifile.manager.ui.components.FileRow
import com.nobifile.manager.ui.components.formatDate
import com.nobifile.manager.ui.navigation.Dest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun contentUriFor(context: Context, item: FileItem): Uri? {
    return try {
        when {
            item.file != null -> FileProvider.getUriForFile(context, "com.nobifile.manager.fileprovider", item.file)
            item.provider == "saf" -> Uri.parse(item.displayPath)
            else -> null
        }
    } catch (e: Exception) { null }
}

fun openWithChooser(context: Context, item: FileItem) {
    val uri = contentUriFor(context, item) ?: run {
        Toast.makeText(context, "Cannot open this item here.", Toast.LENGTH_SHORT).show(); return
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, item.mimeType ?: mimeForExtension(item.extension))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        Toast.makeText(context, "No app can open this file.", Toast.LENGTH_SHORT).show()
    }
}

fun shareItem(context: Context, items: List<FileItem>) {
    if (items.isEmpty()) return
    if (items.size == 1) {
        val uri = contentUriFor(context, items[0]) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = items[0].mimeType ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    } else {
        val uris = items.mapNotNull { contentUriFor(context, it) }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }
}

fun copyTextToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copied: $label", Toast.LENGTH_SHORT).show()
}

@Composable
fun ExplorerScreen(
    navController: NavController,
    initialPath: String,
    kind: String,
    uri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as NobiFileApplication
    var tabs by remember { mutableStateOf(listOf(initialPath)) }
    var currentTab by remember { mutableStateOf(0) }
    var dualPane by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tabs) {
        scope.launch(Dispatchers.IO) {
            try {
                app.database.saveTabs(tabs.mapIndexed { i, p ->
                    com.nobifile.manager.data.ExplorerTab(0, File(p).name.ifBlank { "Storage" }, p, i)
                })
            } catch (e: Exception) { }
        }
    }

    Column(modifier.fillMaxSize()) {
        if (tabs.size > 1 || dualPane) {
            ScrollableTabRow(selectedTabIndex = currentTab) {
                tabs.forEachIndexed { index, path ->
                    Tab(
                        selected = index == currentTab,
                        onClick = { currentTab = index },
                        text = { Text(File(path).name.ifBlank { "Storage" }, maxLines = 1) }
                    )
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f)) {
            val wide = maxWidth > 600.dp
            if (dualPane && wide) {
                Row(Modifier.fillMaxSize()) {
                    ExplorerPane(
                        navController = navController,
                        initialPath = tabs.getOrElse(currentTab) { initialPath },
                        kind = kind,
                        uri = uri,
                        paneKey = "pane-a",
                        modifier = Modifier.weight(1f)
                    )
                    Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
                    ExplorerPane(
                        navController = navController,
                        initialPath = File(Environment.getExternalStorageDirectory(), "Download").absolutePath,
                        kind = "file",
                        uri = "",
                        paneKey = "pane-b",
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                ExplorerPane(
                    navController = navController,
                    initialPath = tabs.getOrElse(currentTab) { initialPath },
                    kind = kind,
                    uri = uri,
                    paneKey = "pane-$currentTab",
                    modifier = Modifier.fillMaxSize(),
                    onNewTab = { path ->
                        tabs = tabs + path
                        currentTab = tabs.size - 1
                    },
                    onCloseTab = {
                        if (tabs.size > 1) {
                            val nt = tabs.toMutableList().apply { removeAt(currentTab) }
                            tabs = nt
                            currentTab = (currentTab - 1).coerceAtLeast(0)
                        }
                    },
                    dualPane = dualPane,
                    onToggleDualPane = { dualPane = !dualPane }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerPane(
    navController: NavController,
    initialPath: String,
    kind: String,
    uri: String,
    paneKey: String,
    modifier: Modifier = Modifier,
    onNewTab: ((String) -> Unit)? = null,
    onCloseTab: (() -> Unit)? = null,
    dualPane: Boolean = false,
    onToggleDualPane: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as NobiFileApplication
    val application = context.applicationContext as Application
    val vm: ExplorerViewModel = viewModel(
        key = paneKey + initialPath,
        factory = ExplorerViewModelFactory(application, initialPath, kind, uri)
    )
    val items by vm.items.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val currentPath by vm.currentPath.collectAsState()
    val sort by vm.sort.collectAsState()
    val viewMode by vm.viewMode.collectAsState()
    val showHidden by vm.showHidden.collectAsState()
    val selection by vm.selection.collectAsState()
    val selectionMode = selection.isNotEmpty()
    val showExtensions by app.prefs.showExtensions.collectAsState(initial = true)
    val clipboard by app.operations.clipboard.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var sheetItem by remember { mutableStateOf<FileItem?>(null) }
    var renameItem by remember { mutableStateOf<FileItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showCreateMenu by remember { mutableStateOf(false) }
    var createKind by remember { mutableStateOf<String?>(null) }
    var createName by remember { mutableStateOf("") }
    var deleteItems by remember { mutableStateOf<List<FileItem>?>(null) }
    var detailsItem by remember { mutableStateOf<FileItem?>(null) }
    var detailsHash by remember { mutableStateOf<String?>(null) }
    var batchItems by remember { mutableStateOf<List<FileItem>?>(null) }
    var conflictPending by remember { mutableStateOf<Triple<List<FileItem>, File, Boolean>?>(null) }

    LaunchedEffect(initialPath, kind) { vm.loadPath(initialPath, pushHistory = false) }

    fun handleOpen(item: FileItem) {
        if (selectionMode) { vm.toggleSelect(item); return }
        if (item.isDirectory) { vm.openItem(item); return }
        app.database.recordRecent(item.name, item.displayPath)
        val ext = item.extension
        when {
            ArchiveOps.isArchive(item.name) -> navController.navigate(Dest.Archive.create(item.displayPath))
            ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp") || ext == "mp4" || ext == "mkv" || ext == "webm" || ext == "mov" || ext in setOf("mp3", "wav", "flac", "ogg", "m4a") || ext == "pdf" || isTextExtension(ext) ->
                navController.navigate(Dest.Viewer.create(item.displayPath, ext))
            else -> openWithChooser(context, item)
        }
    }

    fun doPaste(sources: List<FileItem>, destDir: File, isMove: Boolean, choice: ConflictChoice) {
        val resolve: (File, File) -> ConflictChoice = { _, _ -> choice }
        val cb: (Boolean, String) -> Unit = { ok, msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
            vm.load()
        }
        if (isMove) app.operations.moveItems(sources, destDir, resolve, cb) else app.operations.copyItems(sources, destDir, resolve, cb)
    }

    fun requestPaste() {
        val clip = clipboard ?: return
        val destDir = File(currentPath)
        val sources = clip.items
        scope.launch(Dispatchers.IO) {
            val hasConflict = sources.any { File(destDir, it.file?.name ?: it.name).exists() }
            withContext(Dispatchers.Main) {
                if (hasConflict) conflictPending = Triple(sources, destDir, clip.isMove)
                else doPaste(sources, destDir, clip.isMove, ConflictChoice.RENAME)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("${selection.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { vm.clearSelection() }) { Icon(Icons.Rounded.Close, contentDescription = "Clear selection") }
                    },
                    actions = {
                        IconButton(onClick = { vm.selectAll() }) { Icon(Icons.Rounded.SelectAll, contentDescription = "Select all") }
                        IconButton(onClick = { vm.invertSelection() }) { Icon(Icons.Rounded.SwapHoriz, contentDescription = "Invert selection") }
                        IconButton(onClick = {
                            val sel = vm.selectedItems()
                            app.operations.setClipboard(sel, false)
                            vm.clearSelection()
                            scope.launch { snackbarHostState.showSnackbar("Copied ${sel.size} items. Navigate to a folder and paste.") }
                        }) { Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy") }
                        IconButton(onClick = {
                            val sel = vm.selectedItems()
                            app.operations.setClipboard(sel, true)
                            vm.clearSelection()
                            scope.launch { snackbarHostState.showSnackbar("Cut ${sel.size} items. Navigate to a folder and paste.") }
                        }) { Icon(Icons.Rounded.ContentCut, contentDescription = "Cut") }
                        IconButton(onClick = { shareItem(context, vm.selectedItems()) }) { Icon(Icons.Rounded.Share, contentDescription = "Share") }
                        IconButton(onClick = { deleteItems = vm.selectedItems() }) { Icon(Icons.Rounded.Delete, contentDescription = "Delete") }
                        IconButton(onClick = { showMoreMenu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More actions") }
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            DropdownMenuItem(text = { Text("Batch rename") }, onClick = { showMoreMenu = false; batchItems = vm.selectedItems() })
                            DropdownMenuItem(text = { Text("Compare (select exactly 2 files)") }, onClick = {
                                showMoreMenu = false
                                val sel = vm.selectedItems().filter { !it.isDirectory }
                                if (sel.size == 2) navController.navigate(Dest.Compare.create(sel[0].displayPath, sel[1].displayPath))
                                else scope.launch { snackbarHostState.showSnackbar("Select exactly 2 files to compare.") }
                            })
                            DropdownMenuItem(text = { Text("Add to favorites") }, onClick = {
                                showMoreMenu = false
                                vm.selectedItems().forEach { app.database.addBookmark(it.name, it.displayPath, if (it.isDirectory) "folder" else "file") }
                                vm.clearSelection()
                                scope.launch { snackbarHostState.showSnackbar("Added to favorites.") }
                            })
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(File(currentPath).name.ifBlank { "Storage" }, maxLines = 1)
                            Text(currentPath, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (!vm.navigateUp()) navController.popBackStack()
                        }) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Dest.Search.route) }) { Icon(Icons.Rounded.Search, contentDescription = "Search") }
                        IconButton(onClick = { showSortMenu = true }) { Icon(Icons.Rounded.Sort, contentDescription = "Sort") }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(text = { Text("Name A-Z") }, onClick = { vm.setSort(SortOrder.NAME_ASC); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Name Z-A") }, onClick = { vm.setSort(SortOrder.NAME_DESC); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Newest first") }, onClick = { vm.setSort(SortOrder.DATE_NEW); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Oldest first") }, onClick = { vm.setSort(SortOrder.DATE_OLD); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Largest first") }, onClick = { vm.setSort(SortOrder.SIZE_LARGE); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Smallest first") }, onClick = { vm.setSort(SortOrder.SIZE_SMALL); showSortMenu = false })
                            DropdownMenuItem(text = { Text("By type") }, onClick = { vm.setSort(SortOrder.TYPE); showSortMenu = false })
                        }
                        IconButton(onClick = { showMoreMenu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More") }
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            DropdownMenuItem(text = { Text(if (viewMode == ViewMode.LIST) "Grid view" else "List view") }, onClick = {
                                vm.setViewMode(if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST); showMoreMenu = false
                            })
                            DropdownMenuItem(text = { Text("Detailed view") }, onClick = { vm.setViewMode(ViewMode.DETAILED); showMoreMenu = false })
                            DropdownMenuItem(text = { Text(if (showHidden) "Hide hidden files" else "Show hidden files") }, onClick = {
                                vm.setShowHidden(!showHidden); showMoreMenu = false
                            })
                            DropdownMenuItem(text = { Text(if (dualPane) "Single pane" else "Dual pane") }, onClick = {
                                onToggleDualPane?.invoke(); showMoreMenu = false
                            })
                            if (onNewTab != null) DropdownMenuItem(text = { Text("Open in new tab") }, onClick = { onNewTab(currentPath); showMoreMenu = false })
                            if (onCloseTab != null) DropdownMenuItem(text = { Text("Close tab") }, onClick = { onCloseTab(); showMoreMenu = false })
                            DropdownMenuItem(text = { Text("Refresh") }, onClick = { vm.load(); showMoreMenu = false })
                            DropdownMenuItem(text = { Text("Storage details") }, onClick = { navController.navigate(Dest.Storage.route); showMoreMenu = false })
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(onClick = { showCreateMenu = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Create")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (clipboard != null) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (clipboard!!.isMove) "Cut: ${clipboard!!.items.size} items — tap Paste here" else "Copied: ${clipboard!!.items.size} items — tap Paste here",
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { requestPaste() }) { Text("Paste") }
                        TextButton(onClick = { app.operations.clearClipboard() }) { Text("Cancel") }
                    }
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error != null) {
                ErrorState(error!!, onRetry = { vm.load() })
            } else if (items.isEmpty() && !loading) {
                EmptyState("This folder is empty", "Create a file or folder, or paste items here.")
            } else {
                if (viewMode == ViewMode.GRID || viewMode == ViewMode.LARGE_GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (viewMode == ViewMode.LARGE_GRID) 2 else 3),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items, key = { it.key }) { item ->
                            FileRow(item, viewMode, selection.contains(item.key), selectionMode, showExtensions, onClick = { handleOpen(item) }, onLongClick = { vm.selectOnly(item) })
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize()) {
                        items(items, key = { it.key }) { item ->
                            FileRow(item, viewMode, selection.contains(item.key), selectionMode, showExtensions,
                                onClick = {
                                    if (selectionMode) vm.toggleSelect(item) else handleOpen(item)
                                },
                                onLongClick = {
                                    if (selectionMode) vm.toggleSelect(item) else { vm.selectOnly(item); sheetItem = item }
                                })
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${items.size} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { vm.selectAll() }) { Text("Select all") }
            }
        }
    }

    // Create menu dialog
    if (showCreateMenu) {
        AlertDialog(
            onDismissRequest = { showCreateMenu = false },
            title = { Text("Create new") },
            text = {
                Column {
                    listOf("Folder" to "folder", "Text file" to "txt", "Markdown file" to "md", "Kotlin file" to "kt", "Python file" to "py", "Shell script" to "sh", "JSON file" to "json", "HTML file" to "html").forEach { (label, kind2) ->
                        TextButton(onClick = {
                            showCreateMenu = false
                            createKind = kind2
                            createName = ""
                        }, modifier = Modifier.fillMaxWidth()) { Text(label, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCreateMenu = false }) { Text("Cancel") } }
        )
    }

    createKind?.let { kind2 ->
        AlertDialog(
            onDismissRequest = { createKind = null },
            title = { Text(if (kind2 == "folder") "New folder" else "New file") },
            text = {
                OutlinedTextField(
                    value = createName,
                    onValueChange = { createName = it },
                    label = { Text(if (kind2 == "folder") "Folder name" else "File name (with extension)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val dir = File(currentPath)
                    val name = createName.trim()
                    createKind = null
                    if (kind2 == "folder") {
                        app.operations.createFolder(dir, name) { _, msg -> scope.launch { snackbarHostState.showSnackbar(msg) }; vm.load() }
                    } else {
                        val finalName = if (name.contains(".")) name else "$name.$kind2"
                        val template = when (kind2) {
                            "md" -> "# Title\n\n"
                            "kt" -> "fun main() {\n    println(\"Hello\")\n}\n"
                            "py" -> "print(\"Hello\")\n"
                            "sh" -> "#!/bin/sh\n"
                            "json" -> "{}\n"
                            "html" -> "<!DOCTYPE html>\n<html>\n<body>\n</body>\n</html>\n"
                            else -> ""
                        }
                        app.operations.createFile(dir, finalName, template) { _, msg -> scope.launch { snackbarHostState.showSnackbar(msg) }; vm.load() }
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { createKind = null }) { Text("Cancel") } }
        )
    }

    // Rename dialog
    renameItem?.let { item ->
        AlertDialog(
            onDismissRequest = { renameItem = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text("New name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = renameText.trim()
                    renameItem = null
                    app.operations.renameItem(item, target) { _, msg -> scope.launch { snackbarHostState.showSnackbar(msg) }; vm.load() }
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { renameItem = null }) { Text("Cancel") } }
        )
    }

    // Delete confirmation
    deleteItems?.let { list ->
        AlertDialog(
            onDismissRequest = { deleteItems = null },
            title = { Text("Delete ${list.size} items?") },
            text = { Text("Items will be moved to Trash where possible and can be restored. Items on SAF locations are deleted permanently. This cannot be undone for permanent deletes.") },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = list
                    deleteItems = null
                    vm.clearSelection()
                    app.operations.deleteItems(toDelete, useTrash = true) { _, msg -> scope.launch { snackbarHostState.showSnackbar(msg) }; vm.load() }
                }) { Text("Move to Trash") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val toDelete = list
                        deleteItems = null
                        vm.clearSelection()
                        app.operations.deleteItems(toDelete, useTrash = false) { _, msg -> scope.launch { snackbarHostState.showSnackbar(msg) }; vm.load() }
                    }) { Text("Delete permanently") }
                    TextButton(onClick = { deleteItems = null }) { Text("Cancel") }
                }
            }
        )
    }

    // Conflict dialog
    conflictPending?.let { (sources, destDir, isMove) ->
        AlertDialog(
            onDismissRequest = { conflictPending = null },
            title = { Text("Files already exist") },
            text = { Text("Some files already exist in this folder. Choose how to handle all conflicts.") },
            confirmButton = {
                TextButton(onClick = {
                    conflictPending = null
                    doPaste(sources, destDir, isMove, ConflictChoice.REPLACE)
                }) { Text("Replace all") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        conflictPending = null
                        doPaste(sources, destDir, isMove, ConflictChoice.SKIP)
                    }) { Text("Skip") }
                    TextButton(onClick = {
                        conflictPending = null
                        doPaste(sources, destDir, isMove, ConflictChoice.RENAME)
                    }) { Text("Rename incoming") }
                }
            }
        )
    }

    // Details dialog
    detailsItem?.let { item ->
        AlertDialog(
            onDismissRequest = { detailsItem = null; detailsHash = null },
            title = { Text(item.name) },
            text = {
                Column {
                    Text("Location: ${item.displayPath}")
                    Text("Type: ${item.mimeType ?: if (item.isDirectory) "Folder" else "File"}")
                    Text("Size: ${if (item.isDirectory) "Folder" else formatSize(item.size)}")
                    Text("Modified: ${formatDate(item.lastModified)}")
                    Text("Provider: ${item.provider}  •  Read: ${item.canRead}  •  Write: ${item.canWrite}")
                    if (detailsHash != null) Text("SHA-256: $detailsHash", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    if (!item.isDirectory && item.file != null) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            detailsHash = "Calculating…"
                            app.operations.sha256(item.file) { h -> detailsHash = h }
                        }) { Text("Calculate SHA-256") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { detailsItem = null; detailsHash = null }) { Text("Close") } }
        )
    }

    // Batch rename dialog
    batchItems?.let { list ->
        var find by remember { mutableStateOf("") }
        var replace by remember { mutableStateOf("") }
        var prefix by remember { mutableStateOf("") }
        var suffix by remember { mutableStateOf("") }
        val preview = BatchRename.preview(list.map { it.name }, RenameRule(find = find, replace = replace, prefix = prefix, suffix = suffix))
        AlertDialog(
            onDismissRequest = { batchItems = null },
            title = { Text("Batch rename (${list.size})") },
            text = {
                Column {
                    OutlinedTextField(find, { find = it }, label = { Text("Find") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(replace, { replace = it }, label = { Text("Replace with") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(prefix, { prefix = it }, label = { Text("Prefix") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(suffix, { suffix = it }, label = { Text("Suffix") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("Preview:", style = MaterialTheme.typography.labelMedium)
                    preview.take(5).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (preview.size > 5) Text("… and ${preview.size - 5} more", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val targets = list.zip(preview)
                    batchItems = null
                    vm.clearSelection()
                    scope.launch(Dispatchers.IO) {
                        var done = 0
                        for ((item, newName) in targets) {
                            val f = item.file ?: continue
                            if (newName == item.name) continue
                            val t = File(f.parentFile, newName)
                            if (!t.exists() && f.renameTo(t)) done++
                        }
                        withContext(Dispatchers.Main) {
                            scope.launch { snackbarHostState.showSnackbar("Renamed $done of ${targets.size}") }
                            vm.load()
                        }
                    }
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { batchItems = null }) { Text("Cancel") } }
        )
    }

    // Context bottom sheet
    sheetItem?.let { item ->
        ModalBottomSheet(onDismissRequest = { sheetItem = null }) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(item.displayPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                val actions = mutableListOf<Pair<String, () -> Unit>>()
                actions.add("Open" to { sheetItem = null; handleOpen(item) })
                actions.add("Open with…" to { sheetItem = null; openWithChooser(context, item) })
                actions.add("Copy" to { sheetItem = null; app.operations.setClipboard(listOf(item), false); vm.clearSelection() })
                actions.add("Cut" to { sheetItem = null; app.operations.setClipboard(listOf(item), true); vm.clearSelection() })
                actions.add("Rename" to { sheetItem = null; renameText = item.name; renameItem = item })
                actions.add("Delete" to { sheetItem = null; deleteItems = listOf(item) })
                actions.add("Share" to { sheetItem = null; shareItem(context, listOf(item)) })
                actions.add("Details" to { sheetItem = null; detailsItem = item })
                actions.add("Add to favorites" to {
                    sheetItem = null
                    app.database.addBookmark(item.name, item.displayPath, if (item.isDirectory) "folder" else "file")
                    scope.launch { snackbarHostState.showSnackbar("Added to favorites.") }
                })
                actions.add("Copy path" to { sheetItem = null; copyTextToClipboard(context, "Path", item.displayPath) })
                if (ArchiveOps.isArchive(item.name)) {
                    actions.add("Extract here" to {
                        sheetItem = null
                        scope.launch(Dispatchers.IO) {
                            try {
                                val f = item.file ?: throw IllegalStateException("Archive not accessible as a file.")
                                val dest = File(currentPath, f.nameWithoutExtension)
                                ArchiveOps.extractArchive(f, dest) {}
                                withContext(Dispatchers.Main) { scope.launch { snackbarHostState.showSnackbar("Extracted to ${dest.name}") }; vm.load() }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { scope.launch { snackbarHostState.showSnackbar("Extract failed: ${e.message}") } }
                            }
                        }
                    })
                    actions.add("View archive" to { sheetItem = null; navController.navigate(Dest.Archive.create(item.displayPath)) })
                }
                if (!item.isDirectory) {
                    actions.add("Compress to ZIP" to {
                        sheetItem = null
                        scope.launch(Dispatchers.IO) {
                            try {
                                val f = item.file ?: throw IllegalStateException("File not accessible.")
                                val dest = File(currentPath, "${f.nameWithoutExtension}.zip")
                                ArchiveOps.createZip(listOf(f), dest, File(currentPath))
                                withContext(Dispatchers.Main) { scope.launch { snackbarHostState.showSnackbar("Created ${dest.name}") }; vm.load() }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { scope.launch { snackbarHostState.showSnackbar("Compress failed: ${e.message}") } }
                            }
                        }
                    })
                }
                actions.forEach { (label, action) ->
                    TextButton(onClick = action, modifier = Modifier.fillMaxWidth()) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
