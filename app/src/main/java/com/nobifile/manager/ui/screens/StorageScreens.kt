package com.nobifile.manager.ui.screens

import android.os.Environment
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
import com.nobifile.manager.data.FileItem
import com.nobifile.manager.data.ViewMode
import com.nobifile.manager.data.formatSize
import com.nobifile.manager.storage.StorageRepository
import com.nobifile.manager.ui.components.EmptyState
import com.nobifile.manager.ui.components.FileRow
import com.nobifile.manager.ui.navigation.Dest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val root = Environment.getExternalStorageDirectory()
    val summary = remember { StorageRepository.storageSummary(root.absolutePath) }
    var large by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var emptyFolders by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var analyzing by remember { mutableStateOf(false) }
    var dirSize by remember { mutableStateOf<Long?>(null) }
    var dirSizeRunning by remember { mutableStateOf(false) }
    var cancelFlag by remember { mutableStateOf(false) }

    fun analyze() {
        analyzing = true
        cancelFlag = false
        scope.launch(Dispatchers.IO) {
            val l = StorageRepository.largeFiles(root, 50L * 1024 * 1024, 50) { cancelFlag }
            val e = StorageRepository.emptyFolders(root, 50) { cancelFlag }
            withContext(Dispatchers.Main) { large = l; emptyFolders = e; analyzing = false }
        }
    }

    LaunchedEffect(Unit) { analyze() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage analyzer") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
                actions = {
                    TextButton(onClick = { cancelFlag = true; analyzing = false }) { Text("Cancel") }
                    TextButton(onClick = { analyze() }) { Text("Re-analyze") }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Internal Storage", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = if (summary.totalBytes > 0) summary.usedBytes.toFloat() / summary.totalBytes.toFloat() else 0f, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text("Total: ${formatSize(summary.totalBytes)}  •  Used: ${formatSize(summary.usedBytes)}  •  Free: ${formatSize(summary.freeBytes)}")
                        Text("Category totals below come from MediaStore indexing, not a full filesystem scan. They may differ from total used space.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Directory size", style = MaterialTheme.typography.titleMedium)
                        Text(if (dirSize != null) "Internal Storage contents: ${formatSize(dirSize!!)} (file sizes only, calculated on request)" else "Not calculated yet. This scans files and can take a while on large storage.")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                dirSizeRunning = true; cancelFlag = false
                                scope.launch(Dispatchers.IO) {
                                    val s = StorageRepository.directorySize(root) { cancelFlag }
                                    withContext(Dispatchers.Main) { dirSize = s; dirSizeRunning = false }
                                }
                            }, enabled = !dirSizeRunning) { Text("Calculate") }
                            OutlinedButton(onClick = { cancelFlag = true; dirSizeRunning = false }) { Text("Cancel") }
                        }
                        if (dirSizeRunning) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
            }
            item { Text("Large files (50 MB or larger)", style = MaterialTheme.typography.titleMedium) }
            if (analyzing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (large.isEmpty() && !analyzing) item { EmptyState("No large files found", "No files of 50 MB or larger were found in the scanned locations.") }
            items(large, key = { it.key }) { item ->
                FileRow(item, ViewMode.LIST, false, false, true, onClick = {
                    val parent = item.file?.parentFile
                    if (parent != null) navController.navigate(Dest.Explorer.create(parent.absolutePath))
                }, onLongClick = {})
            }
            item { Text("Empty folders", style = MaterialTheme.typography.titleMedium) }
            if (emptyFolders.isEmpty() && !analyzing) item { EmptyState("No empty folders found", "No empty folders were found in the scanned locations.") }
            items(emptyFolders, key = { it.key }) { item ->
                FileRow(item, ViewMode.LIST, false, false, true, onClick = {
                    navController.navigate(Dest.Explorer.create(item.displayPath))
                }, onLongClick = {})
            }
            item {
                Text("Cleanup suggestions are informational only. NobiFile never deletes files automatically. Review each file before deleting it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(navController: NavController) {
    val context = LocalContext.current
    var kind by remember { mutableStateOf("image") }
    var items by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load(k: String) {
        kind = k
        loading = true
        scope.launch(Dispatchers.IO) {
            val res = StorageRepository.mediaItems(context, k, 500)
            withContext(Dispatchers.Main) { items = res; loading = false }
        }
    }

    LaunchedEffect(Unit) { load("image") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Text("From MediaStore indexing. If a file is not indexed, it will not appear here — use Internal Storage browsing for the full filesystem.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("image" to "Images", "video" to "Videos", "audio" to "Audio", "document" to "Documents").forEach { (k, label) ->
                    FilterChip(selected = kind == k, onClick = { load(k) }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (items.isEmpty() && !loading) EmptyState("Nothing here", "No $kind files were found via MediaStore.")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                items(items, key = { it.key }) { item ->
                    FileRow(item, ViewMode.LIST, false, false, true, onClick = {
                        val parent = item.file?.parentFile
                        if (parent != null) navController.navigate(Dest.Explorer.create(parent.absolutePath))
                    }, onLongClick = {})
                }
            }
            Text("${items.size} items (showing up to 500)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
