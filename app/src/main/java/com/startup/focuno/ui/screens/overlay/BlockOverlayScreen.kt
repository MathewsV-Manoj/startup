package com.startup.focuno.ui.screens.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.startup.focuno.R
import com.startup.focuno.domain.model.BlockKind
import com.startup.focuno.domain.model.BlockScope
import com.startup.focuno.domain.model.BlockOverlayModel
import com.startup.focuno.domain.usecase.BypassPolicy
import com.startup.focuno.service.accessibility.FrictionState
import com.startup.focuno.ui.components.AppIcon
import com.startup.focuno.ui.components.durationText
import com.startup.focuno.ui.components.formatClock
import com.startup.focuno.ui.components.shortVideoName
import com.startup.focuno.ui.theme.DeepVoidPurple
import com.startup.focuno.ui.theme.FocunoTheme
import kotlinx.coroutines.delay

@Composable
private fun rememberNowMs(): Long {
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(1_000)
            value = System.currentTimeMillis()
        }
    }
    return now
}

/** Stateless block screen: calm, explains itself, offers a way home first and a costly way through second. */
@Composable
fun BlockOverlayScreen(
    model: BlockOverlayModel,
    friction: FrictionState,
    nowMs: Long,
    onBackToFocus: () -> Unit,
    onNeedThis: () -> Unit,
    onReasonChange: (String) -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepVoidPurple)
            .systemBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(model.packageName, size = 72.dp)
            Spacer(Modifier.height(20.dp))
            val isFeed = model.scope == BlockScope.SHORT_VIDEO
            Text(
                text = when {
                    isFeed -> stringResource(R.string.block_title_feed, shortVideoName(model.packageName))
                    model.kind == BlockKind.DAILY_LIMIT -> stringResource(R.string.block_title_limit, model.appName)
                    model.kind == BlockKind.TIME_BUDGET -> stringResource(R.string.block_title_budget)
                    else -> stringResource(R.string.block_title, model.appName)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = FocunoTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (model.kind) {
                    BlockKind.SCHEDULE -> if (model.startMinuteOfDay == model.endMinuteOfDay) {
                        stringResource(R.string.when_all_day)
                    } else {
                        "${formatClock(model.startMinuteOfDay)}–${formatClock(model.endMinuteOfDay)}"
                    }
                    BlockKind.QUICK_BLOCK -> model.quickLabel.ifBlank { stringResource(R.string.block_source_quick) }
                    BlockKind.DAILY_LIMIT -> stringResource(R.string.block_source_limit, durationText(model.limitMinutes * 60_000L))
                    BlockKind.TIME_BUDGET -> stringResource(R.string.block_source_budget, durationText(model.limitMinutes * 60_000L))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = FocunoTheme.colors.textTertiary,
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = durationText((model.endsAtMs - nowMs).coerceAtLeast(0), roundUp = true),
                style = MaterialTheme.typography.displayMedium,
                color = FocunoTheme.colors.productive,
            )
            Text(
                stringResource(
                    if (model.kind == BlockKind.DAILY_LIMIT || model.kind == BlockKind.TIME_BUDGET) R.string.block_until_midnight else R.string.block_left,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = FocunoTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onBackToFocus,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = if (isFeed) {
                        stringResource(R.string.block_leave_feed, shortVideoName(model.packageName))
                    } else {
                        stringResource(R.string.block_back_to_focus)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(Modifier.height(12.dp))
            BypassSection(model, friction, nowMs, onNeedThis, onReasonChange, onUnlock)
        }
    }
}

@Composable
private fun BypassSection(
    model: BlockOverlayModel,
    friction: FrictionState,
    nowMs: Long,
    onNeedThis: () -> Unit,
    onReasonChange: (String) -> Unit,
    onUnlock: () -> Unit,
) {
    if (model.strict) {
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = FocunoTheme.colors.textTertiary, modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.block_strict_note), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.textTertiary)
        }
        return
    }

    when (friction) {
        FrictionState.Idle -> {
            val cooldownEnd = model.bypassAvailableAtMs
            val coolingDown = cooldownEnd != null && cooldownEnd > nowMs
            TextButton(
                onClick = onNeedThis,
                enabled = !coolingDown,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(stringResource(R.string.block_i_need_this), color = FocunoTheme.colors.textTertiary)
            }
            if (coolingDown && cooldownEnd != null) {
                Text(
                    text = stringResource(R.string.block_cooldown, durationText(cooldownEnd - nowMs, roundUp = true)),
                    style = MaterialTheme.typography.bodySmall,
                    color = FocunoTheme.colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        is FrictionState.Countdown -> {
            Text(
                text = stringResource(R.string.friction_calm_line),
                style = MaterialTheme.typography.bodyMedium,
                color = FocunoTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { 1f - friction.secondsLeft / BypassPolicy.FRICTION_COUNTDOWN_SECONDS.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = FocunoTheme.colors.productive,
                trackColor = FocunoTheme.colors.trackInactive,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(50)) {
                Text(pluralStringResource(R.plurals.friction_wait_seconds, friction.secondsLeft, friction.secondsLeft))
            }
        }

        is FrictionState.Reason -> {
            Text(
                text = stringResource(R.string.friction_reason_prompt, model.appName),
                style = MaterialTheme.typography.bodyMedium,
                color = FocunoTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = friction.text,
                onValueChange = onReasonChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                supportingText = {
                    Text(stringResource(R.string.friction_reason_counter, friction.text.trim().length, BypassPolicy.MIN_REASON_LENGTH))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FocunoTheme.colors.textPrimary,
                    unfocusedTextColor = FocunoTheme.colors.textPrimary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = FocunoTheme.colors.textTertiary,
                    cursorColor = MaterialTheme.colorScheme.secondary,
                    focusedSupportingTextColor = FocunoTheme.colors.textTertiary,
                    unfocusedSupportingTextColor = FocunoTheme.colors.textTertiary,
                ),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onUnlock,
                enabled = BypassPolicy.isReasonValid(friction.text),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text(stringResource(R.string.friction_unlock))
            }
        }
    }
}

/** Live version: reads state from the engine and keeps its own clock for the countdowns. */
@Composable
fun BlockOverlayHost(
    model: BlockOverlayModel,
    friction: FrictionState,
    onBackToFocus: () -> Unit,
    onNeedThis: () -> Unit,
    onReasonChange: (String) -> Unit,
    onUnlock: () -> Unit,
) {
    FocunoTheme {
        BlockOverlayScreen(
            model = model,
            friction = friction,
            nowMs = rememberNowMs(),
            onBackToFocus = onBackToFocus,
            onNeedThis = onNeedThis,
            onReasonChange = onReasonChange,
            onUnlock = onUnlock,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0417)
@Composable
private fun BlockOverlayPreview() {
    FocunoTheme {
        BlockOverlayScreen(
            model = BlockOverlayModel(
                packageName = "com.instagram.android",
                appName = "Instagram",
                kind = BlockKind.SCHEDULE,
                startMinuteOfDay = 22 * 60,
                endMinuteOfDay = 6 * 60,
                quickLabel = "",
                endsAtMs = System.currentTimeMillis() + 3_600_000,
                strict = false,
                bypassAvailableAtMs = null,
            ),
            friction = FrictionState.Reason("need to reply to a friend"),
            nowMs = System.currentTimeMillis(),
            onBackToFocus = {}, onNeedThis = {}, onReasonChange = {}, onUnlock = {},
        )
    }
}
