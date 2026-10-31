package com.nobifile.manager.ui.screens

import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.nobifile.manager.data.PrefsStore
import com.nobifile.manager.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as NobiFileApplication
    val scope = rememberCoroutineScope()
    val theme by app.prefs.theme.collectAsState(initial = "system")
    val accent by app.prefs.accent.collectAsState(initial = "blue")
    val showHidden by app.prefs.showHidden.collectAsState(initial = false)
    val foldersFirst by app.prefs.foldersFirst.collectAsState(initial = true)
    val showExtensions by app.prefs.showExtensions.collectAsState(initial = true)
    val confirmDelete by app.prefs.confirmDelete.collectAsState(initial = true)
    val thumbnails by app.prefs.thumbnails.collectAsState(initial = true)
    val gridColumns by app.prefs.gridColumns.collectAsState(initial = 3)

    fun saveTheme(v: String) = scope.launch { app.prefs.set(PrefsStore.THEME, v) }
    fun saveAccent(v: String) = scope.launch { app.prefs.set(PrefsStore.ACCENT, v) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Text("Theme: $theme", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system", "light", "dark", "amoled").forEach { t ->
                        FilterChip(selected = theme == t, onClick = { saveTheme(t) }, label = { Text(t.replaceFirstChar { it.uppercase() }) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Accent: $accent", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("blue", "violet", "teal", "amber").forEach { a ->
                        FilterChip(selected = accent == a, onClick = { saveAccent(a) }, label = { Text(a.replaceFirstChar { it.uppercase() }) })
                    }
                }
                Text("Dynamic colour is used on Android 12+ when available. AMOLED black uses a pure black background.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Text("File explorer", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Show hidden files")
                    Switch(checked = showHidden, onCheckedChange = { scope.launch { app.prefs.setBool(PrefsStore.SHOW_HIDDEN, it) } })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Folders first")
                    Switch(checked = foldersFirst, onCheckedChange = { scope.launch { app.prefs.setBool(PrefsStore.FOLDERS_FIRST, it) } })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Show file extensions")
                    Switch(checked = showExtensions, onCheckedChange = { scope.launch { app.prefs.setBool(PrefsStore.SHOW_EXTENSIONS, it) } })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Thumbnails")
                    Switch(checked = thumbnails, onCheckedChange = { scope.launch { app.prefs.setBool(PrefsStore.THUMBNAILS, it) } })
                }
                Text("Grid columns: $gridColumns", style = MaterialTheme.typography.bodyMedium)
                Slider(value = gridColumns.toFloat(), onValueChange = { scope.launch { app.prefs.setInt(PrefsStore.GRID_COLUMNS, it.toInt()) } }, valueRange = 2f..5f, steps = 2)
            }
            item {
                Text("File operations", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Confirm before deleting")
                    Switch(checked = confirmDelete, onCheckedChange = { scope.launch { app.prefs.setBool(PrefsStore.CONFIRM_DELETE, it) } })
                }
                Text("Conflicts always ask before overwriting. Cross-provider moves are verified before the original is deleted.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Text("Storage", style = MaterialTheme.typography.titleMedium)
                Text("All-files access: ${if (Environment.isExternalStorageManager()) "Granted" else "Not granted"}", style = MaterialTheme.typography.bodyMedium)
                Text("Persisted SAF locations: ${StorageRepository.persistedTrees(context).size}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        })
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                }) { Text("Open storage permission settings") }
                Text("If access is denied, NobiFile only shows permitted locations and explains why other actions are unavailable. It never bypasses Android restrictions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Text("Privacy", style = MaterialTheme.typography.titleMedium)
                Button(onClick = {
                    scope.launch(Dispatchers.IO) {
                        app.database.clearRecents()
                        app.database.clearSearchHistory()
                    }
                }) { Text("Clear recent files and search history") }
                Text("Bookmarks, collections, project roots and operation history are stored only on this device in the app database. No file contents are stored in the database.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Text("About NobiFile", style = MaterialTheme.typography.titleMedium)
                Text("Version: ${BuildInfo.VERSION_NAME} (${BuildInfo.VERSION_CODE}) • Build: ${BuildInfo.LABEL}", style = MaterialTheme.typography.bodyMedium)
                Text("Package: com.nobifile.manager", style = MaterialTheme.typography.bodyMedium)
                Text("NobiFile is the file manager of the Nobi ecosystem, alongside Nobi Code (editor) and Nobi Terminal (shell). Each app works on its own and shares only folders Android permits it to share.", style = MaterialTheme.typography.bodyMedium)
                Text("Open-source notices: built with AndroidX, Jetpack Compose and Material 3. Archive handling uses only the Android standard library (ZIP/GZIP/TAR). No third-party file-manager UI was copied.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
