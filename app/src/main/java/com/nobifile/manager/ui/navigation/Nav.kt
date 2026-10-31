package com.nobifile.manager.ui.navigation

import java.io.File

sealed class Dest(val route: String) {
    object Home : Dest("home")
    object Explorer : Dest("explorer?path={path}&kind={kind}&uri={uri}") {
        fun create(path: String, kind: String = "file", uri: String = ""): String {
            val parts = mutableListOf("path=${android.net.Uri.encode(path)}", "kind=$kind")
            if (uri.isNotBlank()) parts.add("uri=${android.net.Uri.encode(uri)}")
            return "explorer?" + parts.joinToString("&")
        }
    }
    object Search : Dest("search")
    object Storage : Dest("storage")
    object Categories : Dest("categories")
    object Developer : Dest("developer")
    object Diagnostics : Dest("diagnostics")
    object Operations : Dest("operations")
    object Trash : Dest("trash")
    object Settings : Dest("settings")
    object Viewer : Dest("viewer?path={path}&kind={kind}") {
        fun create(path: String, kind: String): String = "viewer?path=${android.net.Uri.encode(path)}&kind=$kind"
    }
    object Archive : Dest("archive?path={path}") {
        fun create(path: String): String = "archive?path=${android.net.Uri.encode(path)}"
    }
    object Compare : Dest("compare?left={left}&right={right}") {
        fun create(left: String, right: String): String =
            "compare?left=${android.net.Uri.encode(left)}&right=${android.net.Uri.encode(right)}"
    }
}

/** Human-readable folder title. Never shows a bare "0" for Internal Storage. */
fun folderDisplayName(path: String): String {
    val clean = path.trimEnd('/')
    return when (clean) {
        "", "/" -> "Device Root"
        "/storage/emulated/0" -> "Internal Storage"
        "/storage/emulated" -> "Storage"
        "/storage" -> "Storage"
        else -> File(clean).name.ifBlank { clean }
    }
}

/** Navigation that can never crash the app on a bad route: falls back to Home. */
fun androidx.navigation.NavController.navigateSafe(route: String) {
    try {
        navigate(route)
    } catch (e: Exception) {
        try { navigate(Dest.Home.route) } catch (ignored: Exception) { }
    }
}
