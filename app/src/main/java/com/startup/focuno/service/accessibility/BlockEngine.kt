package com.startup.focuno.service.accessibility

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.local.homePackages
import com.startup.focuno.data.model.AppSettings
import com.startup.focuno.data.repository.AppCategoryRepository
import com.startup.focuno.data.repository.BypassRepository
import com.startup.focuno.data.repository.InstalledAppsRepository
import com.startup.focuno.data.repository.ScheduleRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.model.AppCategory
import com.startup.focuno.domain.model.BlockKind
import com.startup.focuno.domain.model.BlockOverlayModel
import com.startup.focuno.domain.model.BlockSchedule
import com.startup.focuno.domain.model.BypassOutcome
import com.startup.focuno.domain.usecase.BypassPolicy
import com.startup.focuno.domain.usecase.ScheduleEvaluator
import com.startup.focuno.ui.screens.overlay.BlockOverlayHost
import com.startup.focuno.ui.screens.overlay.NudgeToast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** What the accessibility service lends the engine: a way to press Home and a place to draw. */
interface BlockHost {
    val overlay: OverlayWindowManager
    fun goHome()
}

/**
 * Decides, from package names alone, whether the app in front should be blocked right now.
 *
 * The foreground app is HELD as explicit state ([currentPackage]). It only changes when the service reports
 * a window change to a different app, never because a query came back empty. That is what stops the block
 * dropping while someone sits still in one app.
 *
 * Privacy: the only thing read from the screen is the package name of the window that changed.
 */
@Singleton
class BlockEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val categoryRepository: AppCategoryRepository,
    private val bypassRepository: BypassRepository,
    private val settingsStore: SettingsStore,
    private val installedApps: InstalledAppsRepository,
    private val usageRepository: UsageTrackingRepository,
    private val nudgeCoordinator: NudgeCoordinator,
    private val nudgeText: NudgeTextFormatter,
) {
    // Logging outlives the service scope on purpose, so "abandoned" is still recorded while shutting down.
    private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val blockModel = MutableStateFlow<BlockOverlayModel?>(null)

    private var scope: CoroutineScope? = null
    private var host: BlockHost? = null
    private var friction: FrictionController? = null

    private var schedules: List<BlockSchedule> = emptyList()
    private var overrides: Map<String, AppCategory> = emptyMap()
    private var lastGranted: Map<String, Long> = emptyMap()
    private var settings: AppSettings = AppSettings()
    private val loaded = BooleanArray(4)

    private var ignoredPackages: Set<String> = emptySet()
    private var neverBlockPackages: Set<String> = emptySet()

    private var currentPackage: String? = null
    private var shownFor: String? = null
    private var screenOn = true
    private var lastEventPackage: String? = null
    private var lastEventAtMs = 0L
    private var lastOverlayShownAtMs = 0L

    fun start(host: BlockHost) {
        stop()
        this.host = host
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        friction = FrictionController(newScope, onEvent = ::logFrictionEvent, onGranted = ::onBypassGranted)
        nudgeCoordinator.reset()
        screenOn = context.getSystemService(PowerManager::class.java).isInteractive
        refreshSystemPackages()

        newScope.launch {
            scheduleRepository.observeAll().collect { schedules = it; loaded[0] = true; reevaluate() }
        }
        newScope.launch {
            categoryRepository.observeOverrides().collect { overrides = it; loaded[1] = true; reevaluate() }
        }
        newScope.launch {
            bypassRepository.observeLastGrantedByPackage().collect { lastGranted = it; loaded[2] = true; reevaluate() }
        }
        newScope.launch {
            settingsStore.settings.collect { settings = it; loaded[3] = true; reevaluate() }
        }
        newScope.launch { seedForegroundPackage() }
        newScope.launch {
            while (isActive) {
                delay(TICK_MS)
                onTick()
            }
        }
    }

    fun stop() {
        hideBlock(abandonFriction = true)
        host?.overlay?.hideAll()
        scope?.cancel()
        scope = null
        friction = null
        host = null
        currentPackage = null
        loaded.fill(false)
    }

    /** Called for every TYPE_WINDOW_STATE_CHANGED. Only the package name is looked at. */
    fun onWindowChanged(packageName: String) {
        val nowElapsed = SystemClock.elapsedRealtime()
        if (packageName in ignoredPackages) return
        // Our own overlay windows report events too. They must not look like the person leaving the app.
        if (packageName == context.packageName &&
            (shownFor != null || nowElapsed - lastOverlayShownAtMs < OWN_EVENT_GRACE_MS)
        ) {
            return
        }
        if (packageName == lastEventPackage && nowElapsed - lastEventAtMs < DEBOUNCE_MS) return
        lastEventPackage = packageName
        lastEventAtMs = nowElapsed

        if (packageName != currentPackage) {
            if (shownFor != null && shownFor != packageName) hideBlock(abandonFriction = true)
            currentPackage = packageName
            nudgeCoordinator.onForegroundChanged(packageName, System.currentTimeMillis())
        }
        reevaluate()
    }

    fun onScreenOff() {
        screenOn = false
        hideBlock(abandonFriction = true)
        host?.overlay?.hideNudge()
    }

    fun onScreenOn() {
        screenOn = true
        refreshSystemPackages()
        scope?.launch {
            if (currentPackage == null) seedForegroundPackage()
            reevaluate()
        }
    }

    private suspend fun seedForegroundPackage() {
        val pkg = usageRepository.currentForegroundPackage() ?: return
        if (currentPackage == null && pkg !in ignoredPackages) {
            currentPackage = pkg
            reevaluate()
        }
    }

    private fun refreshSystemPackages() {
        val imePackages = context.getSystemService(InputMethodManager::class.java)
            .enabledInputMethodList.map { it.packageName }
        ignoredPackages = setOf("com.android.systemui", "android") + imePackages
        neverBlockPackages = ignoredPackages + context.packageName + context.packageManager.homePackages()
    }

    private fun onTick() {
        screenOn = context.getSystemService(PowerManager::class.java).isInteractive
        reevaluate()
        maybeNudge()
    }

    private fun reevaluate() {
        if (!loaded.all { it }) return
        val pkg = currentPackage
        val model = if (pkg != null && screenOn) decide(pkg, System.currentTimeMillis()) else null
        if (model == null) {
            hideBlock(abandonFriction = true)
            return
        }
        if (shownFor == pkg) {
            blockModel.value = model
        } else {
            showBlock(model)
        }
    }

    private fun decide(pkg: String, nowMs: Long): BlockOverlayModel? {
        if (pkg in neverBlockPackages) return null
        val zoned = Instant.ofEpochMilli(nowMs).atZone(ZoneId.systemDefault())
        val window = ScheduleEvaluator.activeWindow(schedules, pkg, zoned)
        val isDistracting = categoryRepository.resolve(pkg, overrides) == AppCategory.DISTRACTING

        val base = when {
            window != null -> BlockOverlayModel(
                packageName = pkg,
                appName = installedApps.labelBlocking(pkg),
                kind = BlockKind.SCHEDULE,
                startMinuteOfDay = window.startMinuteOfDay,
                endMinuteOfDay = window.endMinuteOfDay,
                quickLabel = "",
                endsAtMs = window.endsAtMs,
                strict = window.strict,
                bypassAvailableAtMs = null,
            )
            settings.quickBlockUntilMs > nowMs && isDistracting -> BlockOverlayModel(
                packageName = pkg,
                appName = installedApps.labelBlocking(pkg),
                kind = BlockKind.QUICK_BLOCK,
                startMinuteOfDay = 0,
                endMinuteOfDay = 0,
                quickLabel = settings.quickBlockLabel,
                endsAtMs = settings.quickBlockUntilMs,
                strict = false,
                bypassAvailableAtMs = null,
            )
            else -> return null
        }

        val granted = lastGranted[pkg]
        if (!base.strict && BypassPolicy.isUnlocked(granted, nowMs)) return null
        val availableAt = BypassPolicy.availableAgainAtMs(granted)?.takeIf { it > nowMs }
        return base.copy(bypassAvailableAtMs = availableAt)
    }

    private fun showBlock(model: BlockOverlayModel) {
        val controller = friction ?: return
        val activeHost = host ?: return
        controller.reset()
        shownFor = model.packageName
        blockModel.value = model
        lastOverlayShownAtMs = SystemClock.elapsedRealtime()
        activeHost.overlay.hideNudge()
        activeHost.overlay.showBlock {
            val current by blockModel.collectAsState()
            val frictionState by controller.state.collectAsState()
            current?.let {
                BlockOverlayHost(
                    model = it,
                    friction = frictionState,
                    onBackToFocus = ::backToFocus,
                    onNeedThis = controller::begin,
                    onReasonChange = controller::onReasonChange,
                    onUnlock = controller::unlock,
                )
            }
        }
        logScope.launch { bypassRepository.logBlockHit(model.packageName) }
    }

    private fun hideBlock(abandonFriction: Boolean) {
        if (abandonFriction) friction?.abandon() else friction?.reset()
        if (shownFor != null || host?.overlay?.isBlockShowing == true) {
            host?.overlay?.hideBlock()
        }
        shownFor = null
        blockModel.value = null
    }

    private fun backToFocus() {
        friction?.abandon()
        hideBlock(abandonFriction = false)
        // Forget the blocked app so the next tick cannot put the overlay back before the launcher reports in.
        currentPackage = null
        host?.goHome()
    }

    private fun onBypassGranted(reason: String) {
        val pkg = shownFor ?: return
        val now = System.currentTimeMillis()
        lastGranted = lastGranted + (pkg to now)
        logScope.launch { bypassRepository.logEvent(pkg, BypassOutcome.GRANTED, reason, now) }
        hideBlock(abandonFriction = false)
    }

    private fun logFrictionEvent(outcome: BypassOutcome, reason: String?) {
        val pkg = shownFor ?: return
        logScope.launch { bypassRepository.logEvent(pkg, outcome, reason) }
    }

    private fun maybeNudge() {
        val activeScope = scope ?: return
        if (!screenOn || shownFor != null) return
        val pkg = currentPackage
        val distracting = pkg != null && pkg !in neverBlockPackages &&
            categoryRepository.resolve(pkg, overrides) == AppCategory.DISTRACTING
        activeScope.launch {
            val nudge = nudgeCoordinator.check(pkg, distracting, System.currentTimeMillis(), settings) ?: return@launch
            val activeHost = host ?: return@launch
            if (shownFor != null) return@launch
            lastOverlayShownAtMs = SystemClock.elapsedRealtime()
            activeHost.overlay.showNudge(NUDGE_VISIBLE_MS) {
                NudgeToast(text = nudgeText.format(nudge), onDismiss = { activeHost.overlay.hideNudge() })
            }
        }
    }

    private companion object {
        const val TICK_MS = 10_000L
        const val DEBOUNCE_MS = 500L
        const val OWN_EVENT_GRACE_MS = 1_500L
        const val NUDGE_VISIBLE_MS = 7_000L
    }
}
