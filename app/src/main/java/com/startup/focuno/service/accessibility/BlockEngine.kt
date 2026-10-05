package com.startup.focuno.service.accessibility

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.local.homePackages
import com.startup.focuno.data.model.AppSettings
import com.startup.focuno.data.repository.AppCategoryRepository
import com.startup.focuno.data.repository.BypassRepository
import com.startup.focuno.data.repository.InstalledAppsRepository
import com.startup.focuno.data.repository.LimitRepository
import com.startup.focuno.data.repository.ScheduleRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.model.AppCategory
import com.startup.focuno.domain.model.AppLimit
import com.startup.focuno.domain.model.BlockKind
import com.startup.focuno.domain.model.BlockOverlayModel
import com.startup.focuno.domain.model.BlockSchedule
import com.startup.focuno.domain.model.BlockScope
import com.startup.focuno.domain.model.BypassOutcome
import com.startup.focuno.domain.model.ShortVideoApps
import com.startup.focuno.domain.usecase.BypassPolicy
import com.startup.focuno.domain.usecase.DailyLimitPolicy
import com.startup.focuno.domain.usecase.FocusPauseRule
import com.startup.focuno.domain.usecase.ScheduleEvaluator
import com.startup.focuno.domain.model.FocusMode
import com.startup.focuno.service.focus.FocusController
import com.startup.focuno.ui.screens.overlay.BlockOverlayHost
import com.startup.focuno.ui.screens.overlay.FOCUS_INSTEAD_MINUTES
import com.startup.focuno.ui.screens.overlay.NudgeToast
import com.startup.focuno.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
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

/** What the accessibility service lends the engine. */
interface BlockHost {
    val overlay: OverlayWindowManager
    fun goHome()
    fun goBack()

    /** Package of the window Android says is in front right now (package name only), or null. */
    fun activeWindowPackage(): String?

    /** Whether Reels (Instagram) or Shorts (YouTube) is on screen in [packageName]. */
    fun isShortVideoShowing(packageName: String): Boolean

    /** Tries to leave the short-video feed by tapping the app's Home tab. */
    fun leaveShortVideoFeed(packageName: String): Boolean

    /** Window-content events are only requested while a Reels/Shorts rule needs them, to save battery. */
    fun setContentEventsEnabled(enabled: Boolean)
}

/**
 * Decides, from package names (and, for Reels/Shorts rules, a few screen-part names), whether the app in
 * front should be blocked right now.
 *
 * The foreground app is HELD as explicit state ([currentPackage]). It changes when a window event names a
 * different app, or when a periodic check of the active window disagrees (which repairs any missed event).
 * It never changes because a query came back empty.
 */
@Singleton
class BlockEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val limitRepository: LimitRepository,
    private val categoryRepository: AppCategoryRepository,
    private val bypassRepository: BypassRepository,
    private val settingsStore: SettingsStore,
    private val installedApps: InstalledAppsRepository,
    private val usageRepository: UsageTrackingRepository,
    private val nudgeCoordinator: NudgeCoordinator,
    private val focusController: FocusController,
    private val nudgeText: NudgeTextFormatter,
    private val log: EngineLog,
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
    private val loaded = BooleanArray(5)

    private var limits: Map<String, AppLimit> = emptyMap()
    private val limitUsage = HashMap<String, MeasuredUse>()
    private var budgetUse: MeasuredUse? = null
    private val limitWarned = HashSet<String>()
    private var measuring = false
    private var remeasureAfter = false

    private var ignoredPackages: Set<String> = emptySet()
    private var neverBlockPackages: Set<String> = emptySet()
    private var essentialPackages: Set<String> = emptySet()

    private var currentPackage: String? = null
    private var inShortVideo = false
    private var contentEventsOn = false
    private var shownFor: String? = null
    private var shownScope: BlockScope? = null
    private var screenOn = true
    private var lastEventPackage: String? = null
    private var lastEventAtMs = 0L
    private var lastOverlayShownAtMs = 0L
    private var lastContentCheckAtMs = 0L
    private var pollSuppressedUntilMs = 0L
    private var shortVideoSuppressedUntilMs = 0L
    private var tickCount = 0

    val isRunning: Boolean get() = host != null

    fun usesHost(candidate: BlockHost): Boolean = host === candidate

    fun start(host: BlockHost) {
        stop()
        this.host = host
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        friction = FrictionController(newScope, onEvent = ::logFrictionEvent, onGranted = ::onBypassGranted)
        nudgeCoordinator.reset()
        screenOn = context.getSystemService(PowerManager::class.java).isInteractive
        refreshSystemPackages()
        log.add("Blocker started (screen ${if (screenOn) "on" else "off"})")

        newScope.launch {
            scheduleRepository.observeAll().collect {
                schedules = it
                loaded[0] = true
                updateContentEvents()
                reevaluate()
            }
        }
        newScope.launch {
            categoryRepository.observeOverrides().collect { overrides = it; loaded[1] = true; reevaluate() }
        }
        newScope.launch {
            bypassRepository.observeLastGrantedByPackage().collect { lastGranted = it; loaded[2] = true; reevaluate() }
        }
        newScope.launch {
            settingsStore.settings.collect {
                val budgetChanged = it.budgetMinutes != settings.budgetMinutes
                settings = it
                loaded[3] = true
                if (budgetChanged) measureLimits(force = true)
                reevaluate()
            }
        }
        newScope.launch {
            limitRepository.observeAll().collect { rows ->
                limits = rows.associateBy { it.packageName }
                loaded[4] = true
                measureLimits(force = true)
                reevaluate()
            }
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
        if (host != null) log.add("Blocker stopped")
        hideBlock(abandonFriction = true)
        host?.overlay?.hideAll()
        host?.setContentEventsEnabled(false)
        scope?.cancel()
        scope = null
        friction = null
        host = null
        currentPackage = null
        inShortVideo = false
        contentEventsOn = false
        loaded.fill(false)
        limitUsage.clear()
        budgetUse = null
        measuring = false
        remeasureAfter = false
    }

    /** Called for every window-state change. Only the package name is looked at. */
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

        if (packageName != currentPackage) adoptForeground(packageName, "event")
        reevaluate()
    }

    /** Called for window-content changes, which only arrive while a Reels/Shorts rule is active. */
    fun onContentChanged(packageName: String) {
        if (!contentEventsOn || packageName != currentPackage) return
        val nowElapsed = SystemClock.elapsedRealtime()
        if (nowElapsed - lastContentCheckAtMs < CONTENT_THROTTLE_MS) return
        lastContentCheckAtMs = nowElapsed
        refreshShortVideoState()
        reevaluate()
    }

    fun onScreenOff() {
        screenOn = false
        log.add("Screen off")
        hideBlock(abandonFriction = true)
        host?.overlay?.hideNudge()
    }

    fun onScreenOn() {
        screenOn = true
        log.add("Screen on")
        refreshSystemPackages()
        scope?.launch {
            if (currentPackage == null) seedForegroundPackage()
            reevaluate()
        }
    }

    private fun adoptForeground(packageName: String, source: String) {
        log.add("Foreground: $packageName ($source)")
        if (shownFor != null && shownFor != packageName) hideBlock(abandonFriction = true)
        currentPackage = packageName
        inShortVideo = false
        updateContentEvents()
        nudgeCoordinator.onForegroundChanged(packageName, System.currentTimeMillis())
        if (packageName in limits) measureLimits(force = true)
    }

    private suspend fun seedForegroundPackage() {
        val pkg = host?.activeWindowPackage() ?: usageRepository.currentForegroundPackage() ?: return
        if (currentPackage == null && pkg !in ignoredPackages && pkg != context.packageName) {
            adoptForeground(pkg, "startup")
            reevaluate()
        }
    }

    private fun refreshSystemPackages() {
        val imePackages = context.getSystemService(InputMethodManager::class.java)
            .enabledInputMethodList.map { it.packageName }
        ignoredPackages = setOf("com.android.systemui", "android") + imePackages
        neverBlockPackages = ignoredPackages + context.packageName + context.packageManager.homePackages()
        val dialer = try {
            context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
        } catch (_: SecurityException) {
            null
        }
        essentialPackages = ESSENTIAL_PACKAGES + setOfNotNull(dialer)
    }

    private fun updateContentEvents() {
        val pkg = currentPackage
        val wanted = pkg != null && ShortVideoApps.supports(pkg) &&
            schedules.any { it.enabled && it.scope == BlockScope.SHORT_VIDEO && it.packageName == pkg }
        if (wanted != contentEventsOn) {
            contentEventsOn = wanted
            host?.setContentEventsEnabled(wanted)
            if (!wanted) inShortVideo = false
            log.add("Reels/Shorts watching ${if (wanted) "on" else "off"}")
        }
    }

    private fun refreshShortVideoState() {
        val pkg = currentPackage ?: return
        val showing = SystemClock.elapsedRealtime() >= shortVideoSuppressedUntilMs && host?.isShortVideoShowing(pkg) == true
        if (showing != inShortVideo) {
            inShortVideo = showing
            log.add("Short-video feed ${if (showing) "shown" else "left"} in $pkg")
        }
    }

    /** Repairs a missed window event: if Android says a different app is in front, believe Android. */
    private fun pollActiveWindow() {
        if (SystemClock.elapsedRealtime() < pollSuppressedUntilMs) return
        val active = host?.activeWindowPackage() ?: return
        if (active in ignoredPackages || active == context.packageName) return
        if (active != currentPackage) adoptForeground(active, "poll")
    }

    private fun onTick() {
        screenOn = context.getSystemService(PowerManager::class.java).isInteractive
        if (screenOn) {
            pollActiveWindow()
            if (contentEventsOn) refreshShortVideoState()
        }
        if (screenOn) measureLimits(force = false)
        reevaluate()
        maybeWarnLimit()
        tickCount++
        if (tickCount % NUDGE_EVERY_TICKS == 0) maybeNudge()
    }

    /**
     * Re-reads today's use of every limited app and of all time-eaters together. Without [force] it only
     * runs when the app in front is limited (or is a time-eater while the budget is on) and its last
     * reading is a minute old, which keeps the cost tiny.
     */
    private fun measureLimits(force: Boolean) {
        val packages = limits.filterValues { it.enabled }.keys
        val budget = settings.timeEaterBudget
        if (packages.isEmpty() && budget == null) return
        if (measuring) {
            // An app was just opened while a reading was running; read again straight after, so an open
            // limit is checked against this open and not a minute later.
            if (force) remeasureAfter = true
            return
        }
        if (!force) {
            val pkg = currentPackage ?: return
            val last = when {
                pkg in packages -> limitUsage[pkg]?.atMs ?: 0L
                budget != null && isTimeEater(pkg) -> budgetUse?.atMs ?: 0L
                else -> return
            }
            if (System.currentTimeMillis() - last < LIMIT_REMEASURE_MS) return
        }
        val activeScope = scope ?: return
        measuring = true
        activeScope.launch {
            try {
                val now = System.currentTimeMillis()
                val usage = usageRepository.usageToday()
                packages.forEach { limitUsage[it] = MeasuredUse(usage[it]?.foregroundMs ?: 0L, now, usage[it]?.openCount ?: 0) }
                budgetUse = MeasuredUse(usage.filterKeys(::isTimeEater).values.sumOf { it.foregroundMs }, now)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.add("Could not measure limited apps: ${e.javaClass.simpleName}")
            } finally {
                measuring = false
            }
            reevaluate()
            if (remeasureAfter) {
                remeasureAfter = false
                measureLimits(force = true)
            }
        }
    }

    private fun isTimeEater(pkg: String): Boolean =
        pkg !in neverBlockPackages && categoryRepository.resolve(pkg, overrides) == AppCategory.DISTRACTING

    /** All time-eater use today; keeps counting while any time-eater is in front. */
    private fun budgetUsedMs(nowMs: Long, dayStartMs: Long): Long? {
        val measured = budgetUse ?: return null
        val inFront = screenOn && currentPackage?.let(::isTimeEater) == true
        return DailyLimitPolicy.usedNowMs(measured.usedMs, measured.atMs, nowMs, inFront, dayStartMs)
    }

    /** Opens so far today, from the last reading. A reading from before midnight counts as none. */
    private fun opensToday(pkg: String, dayStartMs: Long): Int =
        limitUsage[pkg]?.takeIf { it.atMs >= dayStartMs }?.opens ?: 0

    private fun usedTodayMs(pkg: String, nowMs: Long, dayStartMs: Long): Long? {
        val measured = limitUsage[pkg] ?: return null
        return DailyLimitPolicy.usedNowMs(measured.usedMs, measured.atMs, nowMs, inFront = pkg == currentPackage && screenOn, dayStartMs = dayStartMs)
    }

    /** One heads-up per app (and one for the time-eater budget) per day, a few minutes before time runs out. */
    private fun maybeWarnLimit() {
        val pkg = currentPackage ?: return
        if (!screenOn || shownFor != null || host == null) return
        val nowMs = System.currentTimeMillis()
        val zoned = Instant.ofEpochMilli(nowMs).atZone(ZoneId.systemDefault())
        val dayStart = zoned.toLocalDate().atStartOfDay(zoned.zone).toInstant().toEpochMilli()
        val day = zoned.toLocalDate()

        val limit = limits[pkg]
        val appUsed = limit?.let { usedTodayMs(pkg, nowMs, dayStart) }
        if (limit != null && appUsed != null && DailyLimitPolicy.shouldWarn(limit, appUsed)) {
            if (limitWarned.add("$day:$pkg")) {
                val minutesLeft = minutesLeft(limit, appUsed)
                showHeadsUp(context.resources.getQuantityString(R.plurals.limit_warning, minutesLeft, minutesLeft, installedApps.labelBlocking(pkg)))
                log.add("Limit heads-up: $pkg, $minutesLeft min left")
            }
            return
        }

        val budget = settings.timeEaterBudget ?: return
        if (!isTimeEater(pkg)) return
        val used = budgetUsedMs(nowMs, dayStart) ?: return
        if (!DailyLimitPolicy.shouldWarn(budget, used) || !limitWarned.add("$day:${AppSettings.BUDGET_KEY}")) return
        val minutesLeft = minutesLeft(budget, used)
        showHeadsUp(context.resources.getQuantityString(R.plurals.budget_warning, minutesLeft, minutesLeft))
        log.add("Budget heads-up: $minutesLeft min left")
    }

    private fun minutesLeft(limit: AppLimit, usedMs: Long): Int = ((DailyLimitPolicy.remainingMs(limit, usedMs) + 59_999L) / 60_000L).toInt()

    private fun showHeadsUp(text: String) {
        val activeHost = host ?: return
        lastOverlayShownAtMs = SystemClock.elapsedRealtime()
        activeHost.overlay.showNudge(NUDGE_VISIBLE_MS) {
            NudgeToast(text = text, onDismiss = { activeHost.overlay.hideNudge() })
        }
    }

    private fun reevaluate() {
        if (!loaded.all { it }) return
        val pkg = currentPackage
        val model = if (pkg != null && screenOn) decide(pkg, System.currentTimeMillis()) else null
        if (model == null) {
            if (shownFor != null) log.add("Unblocked ${shownFor}")
            hideBlock(abandonFriction = true)
            return
        }
        if (shownFor == pkg && shownScope == model.scope) {
            blockModel.value = model
        } else {
            showBlock(model)
        }
    }

    private fun decide(pkg: String, nowMs: Long): BlockOverlayModel? {
        if (pkg in neverBlockPackages) return null
        val zoned = Instant.ofEpochMilli(nowMs).atZone(ZoneId.systemDefault())
        val isDistracting = categoryRepository.resolve(pkg, overrides) == AppCategory.DISTRACTING

        val appWindow = ScheduleEvaluator.activeWindow(schedules, pkg, zoned, BlockScope.APP)
        val dayStart = zoned.toLocalDate().atStartOfDay(zoned.zone).toInstant().toEpochMilli()
        // During a Pomodoro break apps are free; the pause screen counts down to the end of the round.
        val focusEndsAt = settings.focusPlan?.phaseAt(nowMs)?.takeIf { it.focusing }?.endsAtMs
        val pausedByFocus = FocusPauseRule.pauses(pkg, isDistracting, settings.focusLockAll, settings.focusAllowed, essentialPackages)
        val appLimit = limits[pkg]
        val timeUp = appLimit != null && usedTodayMs(pkg, nowMs, dayStart)?.let { DailyLimitPolicy.isReached(appLimit, it) } == true
        val opensUp = appLimit != null && DailyLimitPolicy.opensReached(appLimit, opensToday(pkg, dayStart))
        val usedBudget = settings.timeEaterBudget?.takeIf { budget ->
            val used = if (isDistracting) budgetUsedMs(nowMs, dayStart) else null
            used != null && DailyLimitPolicy.isReached(budget, used)
        }
        val base = when {
            appWindow != null -> buildModel(pkg, BlockKind.SCHEDULE, BlockScope.APP, appWindow.startMinuteOfDay, appWindow.endMinuteOfDay, appWindow.endsAtMs, appWindow.strict, "")
            focusEndsAt != null && pausedByFocus ->
                buildModel(pkg, BlockKind.QUICK_BLOCK, BlockScope.APP, 0, 0, focusEndsAt, settings.quickBlockStrict, settings.quickBlockLabel)
            appLimit != null && (timeUp || opensUp) ->
                buildModel(pkg, BlockKind.DAILY_LIMIT, BlockScope.APP, 0, 0, DailyLimitPolicy.nextMidnightMs(zoned), appLimit.strict, "")
                    .copy(
                        limitMinutes = if (timeUp) appLimit.dailyMinutes else 0,
                        limitOpens = if (opensUp && !timeUp) appLimit.maxOpens else 0,
                    )
            usedBudget != null ->
                buildModel(pkg, BlockKind.TIME_BUDGET, BlockScope.APP, 0, 0, DailyLimitPolicy.nextMidnightMs(zoned), usedBudget.strict, "")
                    .copy(limitMinutes = usedBudget.dailyMinutes)
            inShortVideo -> {
                val feedWindow = ScheduleEvaluator.activeWindow(schedules, pkg, zoned, BlockScope.SHORT_VIDEO) ?: return null
                buildModel(pkg, BlockKind.SCHEDULE, BlockScope.SHORT_VIDEO, feedWindow.startMinuteOfDay, feedWindow.endMinuteOfDay, feedWindow.endsAtMs, feedWindow.strict, "")
            }
            else -> return null
        }

        val granted = lastGranted[pkg]
        if (!base.strict && BypassPolicy.isUnlocked(granted, nowMs)) return null
        val availableAt = BypassPolicy.availableAgainAtMs(granted)?.takeIf { it > nowMs }
        return base.copy(bypassAvailableAtMs = availableAt)
    }

    private fun buildModel(
        pkg: String,
        kind: BlockKind,
        scope: BlockScope,
        startMinute: Int,
        endMinute: Int,
        endsAtMs: Long,
        strict: Boolean,
        quickLabel: String,
    ) = BlockOverlayModel(
        packageName = pkg,
        appName = installedApps.labelBlocking(pkg),
        kind = kind,
        scope = scope,
        startMinuteOfDay = startMinute,
        endMinuteOfDay = endMinute,
        quickLabel = quickLabel,
        endsAtMs = endsAtMs,
        strict = strict,
        bypassAvailableAtMs = null,
    )

    private fun showBlock(model: BlockOverlayModel) {
        val controller = friction ?: return
        val activeHost = host ?: return
        if (shownFor != null) hideBlock(abandonFriction = true)
        controller.reset()
        blockModel.value = model
        lastOverlayShownAtMs = SystemClock.elapsedRealtime()
        activeHost.overlay.hideNudge()
        val drawn = activeHost.overlay.showBlock {
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
                    onFocusInstead = ::focusInstead,
                )
            }
        }
        if (!drawn) {
            // Never let a draw failure turn into "no block at all": leave the app instead.
            log.add("Overlay FAILED for ${model.packageName}, sending to ${if (model.scope == BlockScope.APP) "home" else "back"}")
            blockModel.value = null
            pollSuppressedUntilMs = SystemClock.elapsedRealtime() + POLL_SUPPRESS_MS
            leaveBlocked(model)
            return
        }
        shownFor = model.packageName
        shownScope = model.scope
        log.add("Block shown: ${model.packageName} (${model.scope}, ${model.kind})")
        logScope.launch { bypassRepository.logBlockHit(model.packageName) }
    }

    private fun hideBlock(abandonFriction: Boolean) {
        if (abandonFriction) friction?.abandon() else friction?.reset()
        if (shownFor != null || host?.overlay?.isBlockShowing == true) {
            host?.overlay?.hideBlock()
        }
        shownFor = null
        shownScope = null
        blockModel.value = null
    }

    /** Starts a focus timer and leaves the blocked app, so the urge becomes study time. */
    private fun focusInstead() {
        val activeScope = scope ?: return
        log.add("Focus instead of ${shownFor ?: "app"}")
        activeScope.launch { focusController.start(FocusMode.TIMER, FOCUS_INSTEAD_MINUTES, strict = false, subject = "") }
        backToFocus()
    }

    private fun backToFocus() {
        val model = blockModel.value
        friction?.abandon()
        hideBlock(abandonFriction = false)
        if (model != null) leaveBlocked(model)
    }

    /** Whole-app blocks go Home. Reels/Shorts blocks only leave the feed and keep the rest of the app. */
    private fun leaveBlocked(model: BlockOverlayModel) {
        val activeHost = host ?: return
        pollSuppressedUntilMs = SystemClock.elapsedRealtime() + POLL_SUPPRESS_MS
        if (model.scope == BlockScope.SHORT_VIDEO) {
            inShortVideo = false
            shortVideoSuppressedUntilMs = SystemClock.elapsedRealtime() + POLL_SUPPRESS_MS
            if (!activeHost.leaveShortVideoFeed(model.packageName)) activeHost.goBack()
        } else {
            // Forget the blocked app so the next tick cannot put the overlay back before the launcher reports in.
            currentPackage = null
            updateContentEvents()
            activeHost.goHome()
        }
    }

    private fun onBypassGranted(reason: String) {
        val pkg = shownFor ?: return
        val now = System.currentTimeMillis()
        lastGranted = lastGranted + (pkg to now)
        log.add("Unlocked $pkg for 5 minutes")
        logScope.launch { bypassRepository.logEvent(pkg, BypassOutcome.GRANTED, reason, now) }
        hideBlock(abandonFriction = false)
    }

    private fun logFrictionEvent(outcome: BypassOutcome, reason: String?) {
        val pkg = shownFor ?: return
        log.add("Unlock attempt: $outcome")
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

    private data class MeasuredUse(val usedMs: Long, val atMs: Long, val opens: Int = 0)

    private companion object {
        /**
         * Never paused by Lock mode, so calls (including emergency calls), Settings and permission or install
         * screens always work. The default phone app is added at runtime.
         */
        val ESSENTIAL_PACKAGES = setOf(
            "com.android.settings",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.emergency",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
        )
        const val LIMIT_REMEASURE_MS = 60_000L
        const val TICK_MS = 2_000L
        const val NUDGE_EVERY_TICKS = 5
        const val DEBOUNCE_MS = 500L
        const val CONTENT_THROTTLE_MS = 350L
        const val OWN_EVENT_GRACE_MS = 1_500L
        const val POLL_SUPPRESS_MS = 2_000L
        const val NUDGE_VISIBLE_MS = 7_000L
    }
}
