package com.nobifile.manager.ui.screens

import android.content.pm.PackageManager
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as NobiFileApplication
    var roots by remember { mutableStateOf(app.database.projectRoots()) }
    var newRootName by remember { mutableStateOf("") }
    var newRootPath by remember { mutableStateOf("") }
    var codeFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var gitInfo by remember { mutableStateOf("No project selected. Register a project root to inspect its Git metadata (read-only).") }
    val scope = rememberCoroutineScope()

    fun inspectGit(path: String) {
        val gitDir = File(path, ".git")
        if (!gitDir.exists()) { gitInfo = "No .git folder found at $path. This is not a Git repository, or Git metadata is not accessible."; return }
        val head = File(gitDir, "HEAD")
        val branch = try {
            val text = head.readText().trim()
            if (text.startsWith("ref:")) text.removePrefix("ref:").trim().removePrefix("refs/heads/") else "Detached HEAD: $text"
        } catch (e: Exception) { "Branch could not be read: ${e.message}" }
        val lastCommit = try {
            val logHead = File(gitDir, "logs/HEAD")
            if (logHead.exists()) {
                val lines = logHead.readLines()
                val last = lines.lastOrNull() ?: ""
                if (last.length > 80) last.substring(0, 80) + "…" else last
            } else "No commit log found in .git/logs/HEAD."
        } catch (e: Exception) { "Commit log could not be read: ${e.message}" }
        gitInfo = "Repository: $path\nBranch: $branch\nLast log entry: $lastCommit\n\nModified/untracked file lists are NOT computed in this version. Do not treat this screen as full Git status."
        scope.launch(Dispatchers.IO) {
            val root = File(path)
            val found = StorageRepository.searchFiles(root, ".kt", showHidden = false, maxResults = 100) { false }
            val code = found.filter { it.extension in setOf("kt", "java", "py", "js", "ts", "json", "xml", "yaml", "yml", "toml") }
            withContext(Dispatchers.Main) { codeFiles = code }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer workspace") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Project roots", style = MaterialTheme.typography.titleMedium)
                Text("Register folders that contain your projects. NobiFile only reads them here; editing happens in Nobi Code and commands in Nobi Terminal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                OutlinedTextField(newRootName, { newRootName = it }, label = { Text("Project name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(newRootPath, { newRootPath = it }, label = { Text("Full folder path, e.g. /storage/emulated/0/Projects/MyApp") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val name = newRootName.trim(); val path = newRootPath.trim()
                    if (name.isNotEmpty() && path.isNotEmpty() && File(path).exists()) {
                        scope.launch(Dispatchers.IO) {
                            app.database.addProjectRoot(name, path)
                            withContext(Dispatchers.Main) { roots = app.database.projectRoots(); newRootName = ""; newRootPath = "" }
                        }
                    }
                }) { Text("Register project") }
            }
            if (roots.isEmpty()) {
                item { EmptyState("No projects registered", "Register a project folder above to inspect it here.") }
            } else {
                items(roots) { root ->
                    ListItem(
                        headlineContent = { Text(root.name) },
                        supportingContent = { Text(root.path) },
                        trailingContent = {
                            Row {
                                TextButton(onClick = { inspectGit(root.path) }) { Text("Inspect") }
                                TextButton(onClick = {
                                    scope.launch(Dispatchers.IO) { app.database.removeProjectRoot(root.id); withContext(Dispatchers.Main) { roots = app.database.projectRoots() } }
                                }) { Text("Remove") }
                            }
                        }
                    )
                    Divider()
                }
            }
            item {
                Text("Git metadata (read-only)", style = MaterialTheme.typography.titleMedium)
                Text(gitInfo, style = MaterialTheme.typography.bodyMedium)
            }
            item { Text("Code and config files in the inspected project (first 100 matches)", style = MaterialTheme.typography.titleMedium) }
            if (codeFiles.isEmpty()) {
                item { Text("No code files listed yet. Tap Inspect on a project above.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(codeFiles, key = { it.key }) { item ->
                    FileRow(item, ViewMode.LIST, false, false, true, onClick = {
                        val parent = item.file?.parentFile
                        if (parent != null) navController.navigate(Dest.Explorer.create(parent.absolutePath))
                    }, onLongClick = {})
                }
            }
            item {
                Text("Logs and configuration", style = MaterialTheme.typography.titleMedium)
                Text("Use Internal Storage browsing to open .log, .json, .xml, .yaml, .toml and .properties files. Text and code previews open from the file list; full editing belongs in Nobi Code.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(navController: NavController) {
    val context = LocalContext.current
    val pm = context.packageManager

    fun checkPackage(pkg: String): String {
        return try {
            pm.getPackageInfo(pkg, 0)
            "Installed: $pkg"
        } catch (e: PackageManager.NameNotFoundException) {
            "Not installed: $pkg"
        } catch (e: Exception) {
            "Check failed for $pkg: ${e.message}"
        }
    }

    fun intentResolves(action: String): Boolean {
        return try {
            val intent = android.content.Intent(action)
            pm.queryIntentActivities(intent, 0).isNotEmpty()
        } catch (e: Exception) { false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connection diagnostics") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Nobi ecosystem — verified checks only", style = MaterialTheme.typography.titleMedium)
                Text("This screen reports what can actually be verified on this device. It does not claim that opening a file in Nobi Code or a folder in Nobi Terminal works until that has been tested on a real device.", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(checkPackage("com.nobicode.editor"), style = MaterialTheme.typography.bodyMedium)
                        Text(checkPackage("com.nobiterminal"), style = MaterialTheme.typography.bodyMedium)
                        Text(checkPackage("com.nobifile.manager"), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("ACTION_VIEW chooser available: ${intentResolves(android.content.Intent.ACTION_VIEW)}", style = MaterialTheme.typography.bodySmall)
                        Text("ACTION_SEND chooser available: ${intentResolves(android.content.Intent.ACTION_SEND)}", style = MaterialTheme.typography.bodySmall)
                        Text("Integration protocol: v1 (diagnostics only, no privileged operations, no commands executed).", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Text("Shared workspace", style = MaterialTheme.typography.titleMedium)
                Text("Projects registered in Developer workspace are the shared workspace roots. All three apps can access the same shared-storage folders only where Android permits that access. SAF-picked folders are private to the app that picked them unless the other app picks the same folder itself.", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                Text("What is NOT verified here", style = MaterialTheme.typography.titleMedium)
                Text("• Opening a specific file directly inside Nobi Code.\n• Opening a terminal session at a specific working directory in Nobi Terminal.\n• Executing any command from NobiFile.\nThese require the receiving app to support a documented intent contract. Until that is verified on device, NobiFile uses the system Open With chooser and file sharing instead of pretending the integration works.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
