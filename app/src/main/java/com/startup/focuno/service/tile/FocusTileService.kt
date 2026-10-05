package com.startup.focuno.service.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.startup.focuno.R
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.domain.model.FocusMode
import com.startup.focuno.service.focus.FocusController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/**
 * Quick Settings tile: tap to start a 25-minute focus timer from anywhere. While a plan runs the tile is
 * lit and shows when it ends; tapping it then does nothing, so it can never end a timer by accident.
 */
class FocusTileService : TileService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun settingsStore(): SettingsStore
        fun focusController(): FocusController
    }

    private val deps by lazy { EntryPointAccessors.fromApplication(applicationContext, Deps::class.java) }

    @Volatile
    private var listening = false

    override fun onStartListening() {
        super.onStartListening()
        listening = true
        scope.launch { refresh() }
    }

    override fun onStopListening() {
        listening = false
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        // A process-wide scope, so closing the shade cannot cancel a start half-way.
        scope.launch {
            val now = System.currentTimeMillis()
            if (deps.settingsStore().settings.first().focusPlan?.isRunning(now) != true) {
                deps.focusController().start(FocusMode.TIMER, QUICK_MINUTES, strict = false, subject = "")
            }
            refresh()
        }
    }

    private suspend fun refresh() {
        val now = System.currentTimeMillis()
        val plan = deps.settingsStore().settings.first().focusPlan?.takeIf { it.isRunning(now) }
        withContext(Dispatchers.Main) {
            if (!listening) return@withContext
            val tile = qsTile ?: return@withContext
            tile.label = getString(R.string.tile_label)
            tile.state = if (plan != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (plan != null) {
                    getString(R.string.tile_until, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(plan.endMs)))
                } else {
                    getString(R.string.tile_start, QUICK_MINUTES)
                }
            }
            tile.updateTile()
        }
    }

    private companion object {
        const val QUICK_MINUTES = 25
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
