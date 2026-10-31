package com.nobifile.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nobifile.manager.NobiFileApplication
import com.nobifile.manager.data.formatSize
import com.nobifile.manager.ui.components.EmptyState
import com.nobifile.manager.ui.components.formatDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationsScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as NobiFileApplication
    val ops by app.operations.operations.collectAsState()
    var history by remember { mutableStateOf(runCatching { app.database.operationHistory() }.getOrDefault(emptyList())) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Operations") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
                actions = {
                    TextButton(onClick = { app.operations.cancelCurrent() }) { Text("Cancel current") }
                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) { withContext(Dispatchers.Main) { history = app.database.operationHistory() } }
                    }) { Text("Refresh") }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Active and recent operations", style = MaterialTheme.typography.titleMedium) }
            if (ops.isEmpty()) {
                item { EmptyState("No operations yet", "Copy, move, delete and other operations will appear here with progress.") }
            } else {
                items(ops.reversed()) { op ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${op.kind} — ${op.status}", style = MaterialTheme.typography.titleSmall)
                            Text(op.detail, style = MaterialTheme.typography.bodySmall)
                            Text("${op.doneFiles}/${op.totalFiles} files • ${formatSize(op.doneBytes)} / ${formatSize(op.totalBytes)}", style = MaterialTheme.typography.bodySmall)
                            if (op.status == "RUNNING") LinearProgressIndicator(progress = if (op.totalBytes > 0) op.doneBytes.toFloat() / op.totalBytes.toFloat() else 0f, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                            if (op.error != null) Text("Error: ${op.error}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item { Text("Operation history", style = MaterialTheme.typography.titleMedium) }
            if (history.isEmpty()) {
                item { Text("No history yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(history) { h ->
                    ListItem(
                        headlineContent = { Text("${h.kind}: ${h.detail}") },
                        supportingContent = { Text("${h.result} • ${formatDate(h.time)}") },
                        leadingContent = { Icon(Icons.Rounded.History, contentDescription = null) }
                    )
                    Divider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as NobiFileApplication
    var entries by remember { mutableStateOf(runCatching { app.database.trashEntries() }.getOrDefault(emptyList())) }
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Trash is managed by NobiFile. Items moved here keep their original location. Restoring moves them back. Emptying Trash deletes them permanently. Files deleted from SAF locations do not appear here — they were deleted permanently at the time.", style = MaterialTheme.typography.bodyMedium)
            if (message != null) {
                Spacer(Modifier.height(8.dp))
                Text(message!!, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            if (entries.isEmpty()) {
                EmptyState("Trash is empty", "Deleted items moved to Trash will appear here.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    items(entries, key = { it.id }) { e ->
                        ListItem(
                            headlineContent = { Text(e.name) },
                            supportingContent = { Text("From: ${e.originalPath}\nDeleted: ${formatDate(e.deletedAt)}") },
                            leadingContent = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                            trailingContent = {
                                Row {
                                    TextButton(onClick = {
                                        app.operations.restoreTrash(e) { ok, msg ->
                                            message = msg
                                            if (ok) entries = app.database.trashEntries()
                                        }
                                    }) { Text("Restore") }
                                    TextButton(onClick = {
                                        app.operations.permanentDeleteTrash(e) { ok, msg ->
                                            message = msg
                                            if (ok) entries = app.database.trashEntries()
                                        }
                                    }) { Text("Delete") }
                                }
                            }
                        )
                        Divider()
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    scope.launch(Dispatchers.IO) {
                        val copy = app.database.trashEntries()
                        copy.forEach { File(it.trashPath).let { f -> if (f.isDirectory) f.deleteRecursively() else f.delete() } }
                        app.database.clearTrash()
                        withContext(Dispatchers.Main) { entries = emptyList(); message = "Trash emptied." }
                    }
                }) { Text("Empty Trash") }
            }
        }
    }
}
