package com.startup.focuno.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.startup.focuno.R
import com.startup.focuno.domain.usecase.ExamCountdown
import com.startup.focuno.ui.theme.FocunoTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Exam name and date, used in Settings and during setup. The name is typed locally and saved when the
 * keyboard closes, a date is picked or the screen is left, so a slow save can never overwrite letters typed
 * after it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamEditor(name: String, date: LocalDate?, onSave: (name: String, date: LocalDate?) -> Unit) {
    var text by remember(name) { mutableStateOf(name) }
    var picking by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    // Leaving the screen with the keyboard still open must not lose the name.
    val latestText by rememberUpdatedState(text)
    val latestName by rememberUpdatedState(name)
    val latestDate by rememberUpdatedState(date)
    DisposableEffect(Unit) {
        onDispose { if (latestText != latestName) onSave(latestText, latestDate) }
    }
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.exam_name_hint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (!it.isFocused && text != name) onSave(text, date) },
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { picking = true }) {
                Text(date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: stringResource(R.string.exam_pick_date))
            }
            Spacer(Modifier.weight(1f))
            ExamCountdown.daysLeft(date, LocalDate.now())?.let { days ->
                Text(
                    pluralStringResource(R.plurals.exam_days_left, days.toInt(), days.toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    color = FocunoTheme.colors.productive,
                )
            }
        }
    }
    if (picking) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picking = false
                    pickerState.selectedDateMillis?.let { onSave(text, Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    picking = false
                    onSave(text, null)
                }) { Text(stringResource(R.string.exam_clear)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
