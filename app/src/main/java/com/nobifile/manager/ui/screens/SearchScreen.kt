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
import com.nobifile.manager.NobiFileApplication
import com.nobifile.manager.data.FileItem
import com.nobifile.manager.data.ViewMode
import com.nobifile.manager.storage.StorageRepository
import com.nobifile.manager.ui.components.EmptyState
import com.nobifile.manager.ui.components.FileRow
import com.nobifile.manager.ui.navigation.Dest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as NobiFileApplication
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf(app.database.searchHistory()) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun doSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
        job?.cancel()
        searching = true
        error = null
        scope.launch(Dispatchers.IO) { app.database.addSearch(q); withContext(Dispatchers.Main) { history = app.database.searchHistory() } }
        job = scope.launch(Dispatchers.IO) {
            try {
                val root = Environment.getExternalStorageDirectory()
                val found = StorageRepository.searchFiles(root, q, showHidden = false, maxResults = 300) { false }
                withContext(Dispatchers.Main) { results = found; searching = false }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { error = e.message ?: "Search failed."; searching = false }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
                actions = {
                    TextButton(onClick = {
                        job?.cancel(); searching = false
                        scope.launch { }
                    }) { Text("Cancel") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("File or folder name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { doSearch() }) { Icon(Icons.Rounded.Search, contentDescription = "Search") }
                }
            )
            Spacer(Modifier.height(8.dp))
            Text("Searches file and folder names under Internal Storage (up to 6 levels deep, 300 results). It does not search file contents.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (searching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            if (history.isNotEmpty() && query.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Recent searches", style = MaterialTheme.typography.titleSmall)
                history.forEach { h ->
                    TextButton(onClick = { query = h.query; doSearch() }) { Text(h.query) }
                }
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) { app.database.clearSearchHistory(); withContext(Dispatchers.Main) { history = emptyList() } }
                }) { Text("Clear search history") }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                if (results.isEmpty() && !searching && query.isNotEmpty()) {
                    item { EmptyState("No results", "Try a shorter name or check the spelling.") }
                }
                items(results, key = { it.key }) { item ->
                    FileRow(
                        item, ViewMode.LIST, false, false, true,
                        onClick = {
                            if (item.isDirectory) navController.navigate(Dest.Explorer.create(item.displayPath))
                            else {
                                app.database.recordRecent(item.name, item.displayPath)
                                if (item.file != null) {
                                    val parent = item.file.parentFile
                                    if (parent != null) navController.navigate(Dest.Explorer.create(parent.absolutePath))
                                }
                            }
                        },
                        onLongClick = {}
                    )
                }
            }
            Text("${results.size} results", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
