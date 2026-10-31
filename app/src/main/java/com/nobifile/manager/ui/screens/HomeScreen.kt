package com.nobifile.manager.ui.screens

import android.content.Intent
import android.os.Environment
import android.provider.Settings
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
import com.nobifile.manager.BuildInfo
import com.nobifile.manager.NobiFileApplication
import com.nobifile.manager.data.formatSize
import com.nobifile.manager.storage.StorageRepository
import com.nobifile.manager.ui.components.EmptyState
import com.nobifile.manager.ui.navigation.Dest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as NobiFileApplication
    val scope = rememberCoroutineScope()
    var recents by remember { mutableStateOf(app.database.recents()) }
    var bookmarks by remember { mutableStateOf(app.database.bookmarks()) }
    var summary by remember { mutableStateOf(StorageRepository.storageSummary(Environment.getExternalStorageDirectory().absolutePath)) }
    var imageCount by remember { mutableStateOf(-1) }
    var videoCount by remember { mutableStateOf(-1) }
    var audioCount by remember { mutableStateOf(-1) }
    val ops by app.operations.operations.collectAsState()
    val activeOp = ops.lastOrNull { it.status == "RUNNING" }
    val allFiles = Environment.isExternalStorageManager()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val img = StorageRepository.mediaCount(context, "image")
            val vid = StorageRepository.mediaCount(context, "video")
            val aud = StorageRepository.mediaCount(context, "audio")
            withContext(Dispatchers.Main) { imageCount = img; videoCount = vid; audioCount = aud }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NobiFile") },
                navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, contentDescription = "Menu") } },
                actions = {
                    IconButton(onClick = { navController.navigate(Dest.Search.route) }) { Icon(Icons.Rounded.Search, contentDescription = "Search") }
                    IconButton(onClick = { navController.navigate(Dest.Settings.route) }) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Storage", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = if (summary.totalBytes > 0) summary.usedBytes.toFloat() / summary.totalBytes.toFloat() else 0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("${formatSize(summary.freeBytes)} free of ${formatSize(summary.totalBytes)}")
                        Text("Used: ${formatSize(summary.usedBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!allFiles) {
                            Spacer(Modifier.height(8.dp))
                            Text("All-files access is not granted. Browsing is limited to permitted locations. Grant access in Storage settings for full browsing.", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    })
                                } catch (e: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                                }
                            }) { Text("Grant all-files access") }
                        }
                    }
                }
            }
            if (activeOp != null) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("${activeOp.kind}: ${activeOp.detail}", style = MaterialTheme.typography.bodyMedium)
                                Text("${activeOp.doneFiles}/${activeOp.totalFiles} files • ${formatSize(activeOp.doneBytes)} / ${formatSize(activeOp.totalBytes)}", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { navController.navigate(Dest.Operations.route) }) { Text("View") }
                        }
                    }
                }
            }
            item {
                Text("Categories", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("Images", imageCount, "image"),
                        Triple("Videos", videoCount, "video"),
                        Triple("Audio", audioCount, "audio")
                    ).forEach { (label, count, kind) ->
                        AssistChip(
                            onClick = { navController.navigate(Dest.Explorer.create(StorageRepository.internalRoot().absolutePath, "file")) },
                            label = { Text("$label ${if (count >= 0) "($count)" else ""}") }
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { navController.navigate(Dest.Explorer.create(StorageRepository.internalRoot().absolutePath)) }) { Text("Internal Storage") }
                    OutlinedButton(onClick = { navController.navigate(Dest.Storage.route) }) { Text("Storage analyzer") }
                    OutlinedButton(onClick = { navController.navigate(Dest.Categories.route) }) { Text("All categories") }
                }
            }
            item { Text("Recent files", style = MaterialTheme.typography.titleMedium) }
            if (recents.isEmpty()) {
                item { EmptyState("No recent files yet", "Files you open in NobiFile will appear here.") }
            } else {
                items(recents) { r ->
                    ListItem(
                        headlineContent = { Text(r.name) },
                        supportingContent = { Text(r.path) },
                        leadingContent = { Icon(Icons.Rounded.History, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Divider()
                }
                item {
                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) { app.database.clearRecents(); withContext(Dispatchers.Main) { recents = emptyList() } }
                    }) { Text("Clear recent history") }
                }
            }
            item { Text("Favorites", style = MaterialTheme.typography.titleMedium) }
            if (bookmarks.isEmpty()) {
                item { EmptyState("No favorites yet", "Long-press a file or folder and choose Add to favorites.") }
            } else {
                items(bookmarks) { b ->
                    ListItem(
                        headlineContent = { Text(b.name) },
                        supportingContent = { Text(b.path) },
                        leadingContent = { Icon(Icons.Rounded.Star, contentDescription = null) },
                        trailingContent = {
                            TextButton(onClick = {
                                scope.launch(Dispatchers.IO) { app.database.removeBookmark(b.id); withContext(Dispatchers.Main) { bookmarks = app.database.bookmarks() } }
                            }) { Text("Remove") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Divider()
                }
            }
            item {
                Text("NobiFile ${BuildInfo.VERSION_NAME} • Build ${BuildInfo.LABEL}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { navController.navigate(Dest.Developer.route) }) { Text("Developer") }
                    OutlinedButton(onClick = { navController.navigate(Dest.Diagnostics.route) }) { Text("Diagnostics") }
                    OutlinedButton(onClick = { navController.navigate(Dest.Trash.route) }) { Text("Trash") }
                }
            }
        }
    }
}
