package com.startup.focuno.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.export.DataExporter
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.model.AppSettings
import com.startup.focuno.domain.model.NudgeSensitivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    private val exporter: DataExporter,
) : ViewModel() {

    /** Result of the last export, shown once as a short message: true saved, false failed. */
    private val _exportResult = MutableStateFlow<Boolean?>(null)
    val exportResult: StateFlow<Boolean?> = _exportResult.asStateFlow()

    fun export(uri: Uri) {
        viewModelScope.launch {
            _exportResult.value = try {
                exporter.export(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }


    val state: StateFlow<AppSettings> = store.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setDailyGoalMinutes(minutes: Int) {
        viewModelScope.launch { store.setDailyGoalMinutes(minutes) }
    }

    fun setStudyGoalMinutes(minutes: Int) {
        viewModelScope.launch { store.setStudyGoalMinutes(minutes) }
    }

    fun setNudgesEnabled(enabled: Boolean) {
        viewModelScope.launch { store.setNudgesEnabled(enabled) }
    }

    fun setNudgeSensitivity(sensitivity: NudgeSensitivity) {
        viewModelScope.launch { store.setNudgeSensitivity(sensitivity) }
    }
}
