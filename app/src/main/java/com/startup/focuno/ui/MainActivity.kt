package com.startup.focuno.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.startup.focuno.ui.theme.FocunoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var openHealthRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark-only app: force light system-bar icons even when the phone is in light mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (savedInstanceState == null) noteHealthRequest(intent)
        setContent {
            FocunoTheme {
                FocunoApp(openHealthRequest = openHealthRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        noteHealthRequest(intent)
    }

    private fun noteHealthRequest(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_HEALTH, false) == true) openHealthRequest++
    }

    companion object {
        const val EXTRA_OPEN_HEALTH = "com.startup.focuno.OPEN_HEALTH"
    }
}
