package com.nobifile.manager.ui.screens

import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.nobifile.manager.ui.navigation.navigateSafe
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
    // Database reads happen off the composition, with safe fallbacks, so a
    // locked or failed database can never crash Home or the drawer.
    var recents by remember { mutableStateOf(emptyList<com.nobifile.manager.data.RecentItem>()) }
    var bookmarks by remember { mutableStateOf(emptyList<com.nobifile.manager.data.BookmarkItem>()) }
    var summary by remember { mutableStateOf(StorageRepository.storageSummary(Environment.getExternalStorageDirectory().absolutePath)) }
    var imageCount by remember { mutableStateOf(-1) }
    var videoCount by remember { mutableStateOf(-1) }
    var audioCount by remember { mutableStateOf(-1) }
    val ops by app.operations.operations.collectAsState()
    val activeOp = ops.lastOrNull { it.status == "RUNNING" }
    val allFiles = Environment.isExternalStorageManager()
    var backPressedAt by remember { mutableStateOf(0L) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        val now = System.currentTimeMillis()
        if (now - backPressedAt < 2000) {
            (context as? android.app.Activity)?.finish()
        } else {
            backPressedAt = now
            scope.launch { snackbarHostState.showSnackbar("Press Back again to exit NobiFile.") }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try { recents = app.database.recents() } catch (e: Exception) { recents = emptyList() }
            try { bookmarks = app.database.bookmarks() } catch (e: Exception) { bookmarks = emptyList() }
            val img = StorageRepository.mediaCount(context, "image")
            val vid = StorageRepository.mediaCount(context, "video")
            val aud = StorageRepository.mediaCount(context, "audio")
            withContext(Dispatchers.Main) { imageCount = img; videoCount = vid; audioCount = aud }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("NobiFile", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, contentDescription = "Menu") } },
                actions = {
                    IconButton(onClick = { navController.navigateSafe(Dest.Search.route) }) { Icon(Icons.Rounded.Search, contentDescription = "Search") }
                    IconButton(onClick = { navController.navigateSafe(Dest.Settings.route) }) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Storage", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = if (summary.totalBytes > 0) summary.usedBytes.toFloat() / summary.totalBytes.toFloat() else 0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("${formatSize(summary.freeBytes)} free of ${formatSize(summary.totalBytes)}", color = MaterialTheme.colorScheme.onSurface)
                        Text("Used: ${formatSize(summary.usedBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!allFiles) {
                            Spacer(Modifier.height(8.dp))
                            Text("All-files access is not granted. Browsing is limited to permitted locations. Grant access in Storage settings for full browsing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${activeOp.kind}: ${activeOp.detail}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("${activeOp.doneFiles}/${activeOp.totalFiles} files • ${formatSize(activeOp.doneBytes)} / ${formatSize(activeOp.totalBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            TextButton(onClick = { navController.navigateSafe(Dest.Operations.route) }) { Text("View") }
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Categories", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { navController.navigateSafe(Dest.Categories.route) },
                        label = { Text("Images ${if (imageCount >= 0) "($imageCount)" else ""}") },
                        leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = { navController.navigateSafe(Dest.Categories.route) },
                        label = { Text("Videos ${if (videoCount >= 0) "($videoCount)" else ""}") },
                        leadingIcon = { Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = { navController.navigateSafe(Dest.Categories.route) },
                        label = { Text("Audio ${if (audioCount >= 0) "($audioCount)" else ""}") },
                        leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Two short rows, never one overflowing row: the V1 single Row
                // squeezed the third button into a tall empty outline.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { navController.navigateSafe(Dest.Explorer.create(StorageRepository.internalRoot().absolutePath)) },
                        modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Internal Storage") }
                    OutlinedButton(
                        onClick = { navController.navigateSafe(Dest.Storage.route) },
                        modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Rounded.Analytics, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Analyzer") }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { navController.navigateSafe(Dest.Categories.route) },
                        modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Rounded.Category, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("All categories") }
                    OutlinedButton(
                        onClick = { navController.navigateSafe(Dest.Developer.route) },
                        modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Developer") }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Recent files", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
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
                        scope.launch(Dispatchers.IO) {
                            try { app.database.clearRecents() } catch (e: Exception) { }
                            withContext(Dispatchers.Main) { recents = emptyList() }
                        }
                    }) { Text("Clear recent history") }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Favorites", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
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
                                scope.launch(Dispatchers.IO) {
                                    try { app.database.removeBookmark(b.id) } catch (e: Exception) { }
                                    withContext(Dispatchers.Main) {
                                        try { bookmarks = app.database.bookmarks() } catch (e: Exception) { }
                                    }
                                }
                            }) { Text("Remove") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Divider()
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("NobiFile ${BuildInfo.VERSION_NAME} • Build ${BuildInfo.LABEL}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { navController.navigateSafe(Dest.Diagnostics.route) }) { Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Diagnostics") }
                    OutlinedButton(onClick = { navController.navigateSafe(Dest.Trash.route) }) { Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Trash") }
                    OutlinedButton(onClick = { navController.navigateSafe(Dest.Settings.route) }) { Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Settings") }
                }
            }
        }
    }
}
