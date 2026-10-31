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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
        drawerScope.launch { drawerState.close() }
        navController.navigate(route) {
            popUpTo(Dest.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("NobiFile", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
                Text("Version ${BuildInfo.VERSION_NAME} • ${BuildInfo.LABEL}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
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
                Text("Developer", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
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
            }
        }
    ) {
        NavHost(navController = navController, startDestination = Dest.Home.route) {
            composable(Dest.Home.route) {
                HomeScreen(navController, onOpenDrawer = { drawerScope.launch { drawerState.open() } })
            }
            composable(
                route = Dest.Explorer.route,
                arguments = listOf(
                    navArgument("path") { type = NavType.StringType; defaultValue = StorageRepository.internalRoot().absolutePath },
                    navArgument("kind") { type = NavType.StringType; defaultValue = "file" },
                    navArgument("uri") { type = NavType.StringType; defaultValue = "" }
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
                    navArgument("path") { type = NavType.StringType; defaultValue = "" },
                    navArgument("kind") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                ViewerScreen(navController, entry.arguments?.getString("path") ?: "", entry.arguments?.getString("kind") ?: "")
            }
            composable(
                route = Dest.Archive.route,
                arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                ArchiveScreen(navController, entry.arguments?.getString("path") ?: "")
            }
            composable(
                route = Dest.Compare.route,
                arguments = listOf(
                    navArgument("left") { type = NavType.StringType; defaultValue = "" },
                    navArgument("right") { type = NavType.StringType; defaultValue = "" }
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
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Welcome to NobiFile", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        when (step) {
            0 -> {
                Text("A professional file manager for everyday use and development work. Browse, copy, move, rename, search, preview and manage archives — all on your device.")
                Spacer(Modifier.height(12.dp))
                Text("NobiFile works alongside Nobi Code and Nobi Terminal. Each app stays independent; they share only folders Android permits.")
            }
            1 -> {
                Text("Storage access", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Android limits which folders an app can see. For full browsing, grant All-files access in system settings. You can also pick individual folders with the Storage Access Framework. If access is denied, NobiFile shows only permitted locations and explains why — it never bypasses Android restrictions and never requests root access.")
            }
            else -> {
                Text("Setup complete", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("You can change storage, theme and explorer settings later in Settings. Version ${BuildInfo.VERSION_NAME} • Build ${BuildInfo.LABEL}.")
            }
        }
        Spacer(Modifier.height(24.dp))
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (step > 0) OutlinedButton(onClick = { step-- }) { Text("Back") }
            Button(onClick = { if (step < 2) step++ else onDone() }, modifier = Modifier.weight(1f)) {
                Text(if (step < 2) "Continue" else "Enter NobiFile")
            }
            if (step < 2) TextButton(onClick = onDone) { Text("Skip") }
        }
    }
}
