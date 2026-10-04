package com.startup.focuno.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.ui.screens.apps.AppsScreen
import com.startup.focuno.ui.screens.block.BlockScreen
import com.startup.focuno.ui.screens.block.ScheduleEditorSheet
import com.startup.focuno.ui.screens.block.ScheduleEditorViewModel
import com.startup.focuno.ui.screens.focus.FocusScreen
import com.startup.focuno.ui.screens.health.HealthScreen
import com.startup.focuno.ui.screens.insights.InsightsScreen
import com.startup.focuno.ui.screens.onboarding.OnboardingScreen
import com.startup.focuno.ui.screens.settings.PrivacyScreen
import com.startup.focuno.ui.screens.settings.SettingsScreen
import com.startup.focuno.ui.screens.settings.UsageCheckScreen
import com.startup.focuno.ui.theme.DeepVoidPurple
import com.startup.focuno.ui.theme.FocunoTheme

private enum class Tab(val labelRes: Int, val icon: ImageVector) {
    FOCUS(R.string.tab_focus, Icons.Rounded.Timer),
    APPS(R.string.tab_apps, Icons.Rounded.Apps),
    BLOCK(R.string.tab_block, Icons.Rounded.Lock),
    INSIGHTS(R.string.tab_insights, Icons.Rounded.Insights),
}

private const val ROUTE_SETTINGS = 0
private const val ROUTE_PRIVACY = 1
private const val ROUTE_HEALTH = 2
private const val ROUTE_USAGE_CHECK = 3

/** Root of the UI. [openHealthRequest] goes up each time a notification asks to open the health check. */
@Composable
fun FocunoApp(openHealthRequest: Int = 0, appViewModel: AppViewModel = hiltViewModel()) {
    val state by appViewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(DeepVoidPurple)) {
        when (state) {
            AppUiState.Loading -> Unit
            AppUiState.Onboarding -> OnboardingScreen()
            AppUiState.Main -> MainScaffold(openHealthRequest)
        }
    }
}

@Composable
private fun MainScaffold(openHealthRequest: Int) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    // A tiny back stack of full-screen routes, saved as a list so rotation and process death keep the place.
    var stack by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    val route = stack.lastOrNull()

    LaunchedEffect(openHealthRequest) {
        if (openHealthRequest > 0 && stack.lastOrNull() != ROUTE_HEALTH) stack = stack + ROUTE_HEALTH
    }
    BackHandler(enabled = stack.isNotEmpty()) { stack = stack.dropLast(1) }

    val editor: ScheduleEditorViewModel = hiltViewModel()
    val editorState by editor.state.collectAsStateWithLifecycle()

    if (route == null) {
        Scaffold(
            containerColor = DeepVoidPurple,
            bottomBar = {
                NavigationBar(containerColor = FocunoTheme.colors.surfaceElevated) {
                    Tab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = index == tabIndex,
                            onClick = { tabIndex = index },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FocunoTheme.colors.textPrimary,
                                selectedTextColor = FocunoTheme.colors.textPrimary,
                                indicatorColor = FocunoTheme.colors.trackInactive,
                                unselectedIconColor = FocunoTheme.colors.textTertiary,
                                unselectedTextColor = FocunoTheme.colors.textTertiary,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                Crossfade(targetState = tabIndex, label = "tabs") { index ->
                    when (Tab.entries[index]) {
                        Tab.FOCUS -> FocusScreen(
                            onOpenSettings = { stack = stack + ROUTE_SETTINGS },
                            onOpenHealth = { stack = stack + ROUTE_HEALTH },
                        )
                        Tab.APPS -> AppsScreen(
                            onOpenHealth = { stack = stack + ROUTE_HEALTH },
                            onScheduleApp = editor::openForApp,
                        )
                        Tab.BLOCK -> BlockScreen(
                            onAddSchedule = editor::openNew,
                            onEditSchedule = editor::openEdit,
                        )
                        Tab.INSIGHTS -> InsightsScreen()
                    }
                }
            }
        }
    } else {
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            when (route) {
                ROUTE_SETTINGS -> SettingsScreen(
                    onBack = { stack = stack.dropLast(1) },
                    onOpenHealth = { stack = stack + ROUTE_HEALTH },
                    onOpenPrivacy = { stack = stack + ROUTE_PRIVACY },
                    onOpenUsageCheck = { stack = stack + ROUTE_USAGE_CHECK },
                )
                ROUTE_PRIVACY -> PrivacyScreen(onBack = { stack = stack.dropLast(1) })
                ROUTE_HEALTH -> HealthScreen(onBack = { stack = stack.dropLast(1) })
                ROUTE_USAGE_CHECK -> UsageCheckScreen(onBack = { stack = stack.dropLast(1) })
            }
        }
    }

    ScheduleEditorSheet(
        state = editorState,
        onDismiss = editor::dismiss,
        onQuery = editor::setQuery,
        onSelectApp = editor::selectApp,
        onChooseScope = editor::chooseScope,
        onChoosePreset = editor::choosePreset,
        onChooseCustomTime = editor::chooseCustomTime,
        onStepBack = editor::stepBack,
        onShowTimePicker = editor::showTimePicker,
        onHideTimePicker = editor::hideTimePicker,
        onSetTime = editor::setTime,
        onToggleDay = editor::toggleDay,
        onSetStrict = editor::setStrict,
        onSave = editor::save,
        onDelete = editor::delete,
    )
}
