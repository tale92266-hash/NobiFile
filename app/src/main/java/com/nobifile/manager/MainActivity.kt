package com.nobifile.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
                // Root Surface guarantees the Compose background always matches the
                // scheme (never the bare window background), so text contrast is
                // correct in light, dark and AMOLED modes.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    AppShell()
                }
            }
        }
    }
}
