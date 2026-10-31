package com.nobifile.manager.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nobifile.manager.BuildInfo
import com.nobifile.manager.NobiFileApplication
import com.nobifile.manager.data.PrefsStore
import com.nobifile.manager.storage.StorageRepository
import com.nobifile.manager.ui.navigation.Dest
import com.nobifile.manager.ui.navigation.folderDisplayName
import com.nobifile.manager.ui.navigation.navigateSafe
import kotlinx.coroutines.launch

@Composable
fun AppShell() {
    val context = LocalContext.current
    val app = context.applicationContext as NobiFileApplication
    val onboardingDone by app.prefs.onboardingDone.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    if (!onboardingDone) {
        OnboardingScreen(onDone = {
            scope.launch { app.prefs.setBool(PrefsStore.ONBOARDING_DONE, true) }
        })
        return
    }
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()

    fun closeDrawerAndGo(route: String) {
        drawerScope.launch {
            try { drawerState.close() } catch (e: Exception) { }
            // Navigate only after the drawer has closed: navigating while the
            // drawer animates was a crash source in V1 (sidebar click crash).
            val current = navController.currentDestination?.route ?: ""
            if (current == route) return@launch
            try {
                navController.navigate(route) {
                    popUpTo(Dest.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            } catch (e: Exception) {
                navController.navigateSafe(route)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("NobiFile", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text("Version ${BuildInfo.VERSION_NAME} • ${BuildInfo.LABEL}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Divider()
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = false,
                    onClick = { closeDrawerAndGo(Dest.Home.route) },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Internal Storage") },
                    selected = false,
                    onClick = { closeDrawerAndGo(Dest.Explorer.create(StorageRepository.internalRoot().absolutePath)) },
                    icon = { Icon(Icons.Rounded.Folder, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Downloads") },
                    selected = false,
                    onClick = { closeDrawerAndGo(Dest.Explorer.create(java.io.File(Environment.getExternalStorageDirectory(), "Download").absolutePath)) },
                    icon = { Icon(Icons.Rounded.Download, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Search") },
                    selected = false,
                    onClick = { closeDrawerAndGo(Dest.Search.route) },
                    icon = { Icon(Icons.Rounded.Search, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Categories") },
                    selected = false,
                    onClick = { closeDrawerAndGo(Dest.Categories.route) },
                    icon = { Icon(Icons.Rounded.Category, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Storage analyzer") },
                    selected = false,
                    onClick = { closeDrawerAndGo(Dest.Storage.route) },
                    icon = { Icon(Icons.Rounded.Storage, contentDescription = null) }
                )
                Divider()
                Text("Developer", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("Developer workspace") },
                    selected = false,
                    onClick = { closeDrawerAndGo(Dest.Developer.route) },
                    icon = { Icon(Icons.Rounded.Code, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Diagnostics") },
                    selected = false,
                    onClick = { closeDrawerAndGo(Dest.Diagnostics.route) },
                    icon = { Icon(Icons.Rounded.BugReport, contentDescription = null) }
                )
                Divider()
                NavigationDrawerItem(
                    label = { Text("Operations") },
                    selected = false,
                    onClick = { closeDrawerAndGo(Dest.Operations.route) },
                    icon = { Icon(Icons.Rounded.Sync, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Trash") },
                    selected = false,
                    onClick = { closeDrawerAndGo(Dest.Trash.route) },
                    icon = { Icon(Icons.Rounded.Delete, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { closeDrawerAndGo(Dest.Settings.route) },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "NobiFile • Nobi Code • Nobi Terminal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    ) {
        NavHost(navController = navController, startDestination = Dest.Home.route) {
            composable(Dest.Home.route) {
                HomeScreen(navController, onOpenDrawer = { drawerScope.launch { try { drawerState.open() } catch (e: Exception) { } } })
            }
            composable(
                route = Dest.Explorer.route,
                arguments = listOf(
                    navArgument("path") { type = NavType.StringType; nullable = true; defaultValue = StorageRepository.internalRoot().absolutePath },
                    navArgument("kind") { type = NavType.StringType; nullable = true; defaultValue = "file" },
                    navArgument("uri") { type = NavType.StringType; nullable = true; defaultValue = "" }
                )
            ) { entry ->
                val path = entry.arguments?.getString("path") ?: StorageRepository.internalRoot().absolutePath
                val kind = entry.arguments?.getString("kind") ?: "file"
                val uri = entry.arguments?.getString("uri") ?: ""
                ExplorerScreen(navController, path, kind, uri)
            }
            composable(Dest.Search.route) { SearchScreen(navController) }
            composable(Dest.Storage.route) { StorageScreen(navController) }
            composable(Dest.Categories.route) { CategoriesScreen(navController) }
            composable(Dest.Developer.route) { DeveloperScreen(navController) }
            composable(Dest.Diagnostics.route) { DiagnosticsScreen(navController) }
            composable(Dest.Operations.route) { OperationsScreen(navController) }
            composable(Dest.Trash.route) { TrashScreen(navController) }
            composable(Dest.Settings.route) { SettingsScreen(navController) }
            composable(
                route = Dest.Viewer.route,
                arguments = listOf(
                    navArgument("path") { type = NavType.StringType; nullable = true; defaultValue = "" },
                    navArgument("kind") { type = NavType.StringType; nullable = true; defaultValue = "" }
                )
            ) { entry ->
                ViewerScreen(navController, entry.arguments?.getString("path") ?: "", entry.arguments?.getString("kind") ?: "")
            }
            composable(
                route = Dest.Archive.route,
                arguments = listOf(navArgument("path") { type = NavType.StringType; nullable = true; defaultValue = "" })
            ) { entry ->
                ArchiveScreen(navController, entry.arguments?.getString("path") ?: "")
            }
            composable(
                route = Dest.Compare.route,
                arguments = listOf(
                    navArgument("left") { type = NavType.StringType; nullable = true; defaultValue = "" },
                    navArgument("right") { type = NavType.StringType; nullable = true; defaultValue = "" }
                )
            ) { entry ->
                CompareScreen(navController, entry.arguments?.getString("left") ?: "", entry.arguments?.getString("right") ?: "")
            }
        }
    }
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(0) }
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: Exception) {
                // Permission could not be persisted; the picked folder is still usable for this session via the returned URI.
            }
        }
    }
    // Explicit Surface + explicit text colours: the screen must be readable in
    // light, dark and AMOLED modes without relying on the window background.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Icon(
                Icons.Rounded.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Welcome to NobiFile",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Step ${step + 1} of 3",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            when (step) {
                0 -> {
                    Icon(Icons.Rounded.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "A professional file manager for everyday use and development work. Browse, copy, move, rename, search, preview and manage archives — all on your device.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "NobiFile works alongside Nobi Code and Nobi Terminal. Each app stays independent; they share only folders Android permits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                1 -> {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Storage access",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Android limits which folders an app can see. For full browsing, grant All-files access in system settings. You can also pick individual folders with the Storage Access Framework. If access is denied, NobiFile shows only permitted locations and explains why — it never bypasses Android restrictions and never requests root access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                else -> {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Setup complete",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You can change storage, theme and explorer settings later in Settings. Version ${BuildInfo.VERSION_NAME} • Build ${BuildInfo.LABEL}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (step == 1) {
                Button(onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Grant all-files access") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { treeLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) { Text("Pick a folder (SAF)") }
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) OutlinedButton(onClick = { step-- }) { Text("Back") }
                Button(onClick = { if (step < 2) step++ else onDone() }, modifier = Modifier.weight(1f)) {
                    Text(if (step < 2) "Continue" else "Enter NobiFile")
                }
                if (step < 2) TextButton(onClick = onDone) { Text("Skip") }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "NobiFile • Version ${BuildInfo.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
