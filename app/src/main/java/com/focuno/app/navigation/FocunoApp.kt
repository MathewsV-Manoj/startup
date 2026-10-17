package com.focuno.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focuno.core.domain.usecase.ObserveProfileUseCase
import com.focuno.feature.analytics.AnalyticsRoute
import com.focuno.feature.blocking.BlockingRoute
import com.focuno.feature.focus.FocusRoute
import com.focuno.feature.onboarding.OnboardingRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private object Routes {
    const val ONBOARDING = "onboarding"
    const val FOCUS = "focus"
    const val ANALYTICS = "analytics"
    const val BLOCKING = "blocking"
}

private data class TopDestination(val route: String, val label: String, val icon: ImageVector)

private val topDestinations = listOf(
    TopDestination(Routes.FOCUS, "Focus", Icons.Filled.Timer),
    TopDestination(Routes.ANALYTICS, "Analytics", Icons.Filled.Insights),
    TopDestination(Routes.BLOCKING, "Blocking", Icons.Filled.Lock),
)

@HiltViewModel
class AppViewModel @Inject constructor(
    observeProfile: ObserveProfileUseCase,
) : ViewModel() {
    val onboardingCompleted: StateFlow<Boolean?> = observeProfile()
        .map { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun FocunoApp(viewModel: AppViewModel = hiltViewModel()) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    when (onboardingCompleted) {
        null -> Unit
        false -> OnboardingRoute(onFinished = { })
        true -> MainScaffold()
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                topDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.FOCUS,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.FOCUS) { FocusRoute() }
            composable(Routes.ANALYTICS) { AnalyticsRoute() }
            composable(Routes.BLOCKING) { BlockingRoute() }
        }
    }
}
