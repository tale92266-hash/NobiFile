package com.nobifile.manager.ui.navigation

sealed class Dest(val route: String) {
    object Home : Dest("home")
    object Explorer : Dest("explorer?path={path}&kind={kind}&uri={uri}") {
        fun create(path: String, kind: String = "file", uri: String = ""): String {
            return "explorer?path=${android.net.Uri.encode(path)}&kind=$kind&uri=${android.net.Uri.encode(uri)}"
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
