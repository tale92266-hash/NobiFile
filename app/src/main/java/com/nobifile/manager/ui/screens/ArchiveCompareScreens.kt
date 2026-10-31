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
import com.nobifile.manager.data.formatSize
import com.nobifile.manager.ops.ArchiveOps
import com.nobifile.manager.ui.components.EmptyState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(navController: NavController, path: String) {
    val context = LocalContext.current
    val file = File(path)
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<com.nobifile.manager.ops.ArchiveEntry>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var showExtractChoice by remember { mutableStateOf(false) }

    LaunchedEffect(path) {
        scope.launch(Dispatchers.IO) {
            try {
                val list = ArchiveOps.listArchive(file)
                withContext(Dispatchers.Main) { entries = list; loading = false }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { error = e.message; loading = false }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name, maxLines = 1) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
                actions = {
                    TextButton(onClick = { showExtractChoice = true }) { Text("Extract") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Text("Archive: ${file.name} (${formatSize(file.length())})", style = MaterialTheme.typography.bodyMedium)
            Text("Supported here: ZIP, TAR, TAR.GZ, GZIP. RAR and 7Z are not supported in this version and are never claimed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (message != null) {
                Spacer(Modifier.height(8.dp))
                Text(message!!, style = MaterialTheme.typography.bodyMedium)
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            if (error != null) Text("Could not read archive: $error", color = MaterialTheme.colorScheme.error)
            if (entries.isEmpty() && !loading && error == null) EmptyState("Archive is empty", "No entries were found in this archive.")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                items(entries) { e ->
                    ListItem(
                        headlineContent = { Text(e.name) },
                        supportingContent = { Text(if (e.isDirectory) "Folder" else formatSize(e.size)) },
                        leadingContent = { Icon(if (e.isDirectory) Icons.Rounded.Folder else Icons.Rounded.InsertDriveFile, contentDescription = null) }
                    )
                    Divider()
                }
            }
            if (showExtractChoice) {
                AlertDialog(
                    onDismissRequest = { showExtractChoice = false },
                    title = { Text("Extract archive") },
                    text = { Text("Extract to a new folder next to the archive, or extract here (same folder)? Existing files with the same name will be overwritten only after this confirmation.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showExtractChoice = false
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val dest = File(file.parentFile, file.nameWithoutExtension)
                                    ArchiveOps.extractArchive(file, dest) {}
                                    withContext(Dispatchers.Main) { message = "Extracted to ${dest.name}" }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { message = "Extract failed: ${e.message}" }
                                }
                            }
                        }) { Text("New folder") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showExtractChoice = false
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val dest = file.parentFile ?: return@launch
                                    ArchiveOps.extractArchive(file, dest) {}
                                    withContext(Dispatchers.Main) { message = "Extracted here." }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { message = "Extract failed: ${e.message}" }
                                }
                            }
                        }) { Text("Extract here") }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(navController: NavController, leftPath: String, rightPath: String) {
    val left = File(leftPath)
    val right = File(rightPath)
    var lines by remember { mutableStateOf<List<com.nobifile.manager.ops.TextCompare.DiffLine>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(leftPath, rightPath) {
        scope.launch(Dispatchers.IO) {
            try {
                val l = left.readLines()
                val r = right.readLines()
                val d = com.nobifile.manager.ops.TextCompare.diff(l, r)
                withContext(Dispatchers.Main) { lines = d }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { error = e.message }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare files") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Text("Left: ${left.name}", style = MaterialTheme.typography.bodyMedium)
            Text("Right: ${right.name}", style = MaterialTheme.typography.bodyMedium)
            Text("Lines starting with − are only in the left file, + only in the right file. Folder comparison is not included in this screen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (error != null) Text("Could not compare: $error", color = MaterialTheme.colorScheme.error)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(lines) { d ->
                    val color = when (d.kind) {
                        '-' -> MaterialTheme.colorScheme.error
                        '+' -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text("${d.kind} ${d.text}", color = color, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
            Text("${lines.size} diff lines shown.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
