package com.nobifile.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.nobifile.manager.ui.screens.AppShell
import com.nobifile.manager.ui.theme.NobiFileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as NobiFileApplication
            val theme by app.prefs.theme.collectAsState(initial = "system")
            val accent by app.prefs.accent.collectAsState(initial = "blue")
            NobiFileTheme(themeMode = theme, accent = accent) {
                AppShell()
            }
        }
    }
}
