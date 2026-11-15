package com.diary.construction.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.diary.construction.R
import com.diary.construction.core.util.Dates

/** Plain text field with the common setup. */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isError: Boolean = false,
    supportingText: String? = null,
    prefix: String? = null,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        prefix = prefix?.takeIf { it.isNotBlank() }?.let { { Text("$it ") } },
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Amount field (rupees with optional decimals). Only digits and one dot can be typed. */
@Composable
fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    FormTextField(
        value = value,
        onValueChange = { onValueChange(cleanDecimal(it)) },
        label = label,
        modifier = modifier,
        keyboardType = KeyboardType.Decimal,
        isError = isError,
        supportingText = supportingText,
        prefix = LocalAppSettings.current.currency,
    )
}

/** Decimal number field (quantity, area, rate per unit ...). */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    decimals: Boolean = true,
) {
    FormTextField(
        value = value,
        onValueChange = { onValueChange(if (decimals) cleanDecimal(it) else it.filter(Char::isDigit).take(9)) },
        label = label,
        modifier = modifier,
        keyboardType = if (decimals) KeyboardType.Decimal else KeyboardType.Number,
        isError = isError,
        supportingText = supportingText,
        suffix = suffix,
    )
}

@Composable
fun PhoneField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    FormTextField(
        value = value,
        onValueChange = { text -> onValueChange(text.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' }.take(20)) },
        label = label,
        modifier = modifier,
        keyboardType = KeyboardType.Phone,
    )
}

/** Keeps digits and at most one dot, max 2 decimals. */
fun cleanDecimal(text: String): String {
    val sb = StringBuilder()
    var dot = false
    var decimals = 0
    for (c in text) {
        when {
            c.isDigit() && !dot -> if (sb.length < 12) sb.append(c)
            c.isDigit() && dot -> if (decimals < 2) { sb.append(c); decimals++ }
            c == '.' && !dot -> { dot = true; sb.append('.') }
        }
    }
    return sb.toString()
}

/** Text field that runs a click handler instead of opening the keyboard. */
@Composable
fun ClickableField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    LaunchedEffect(source) {
        source.interactions.collect { if (it is PressInteraction.Release) onClick() }
    }
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        leadingIcon = leading,
        trailingIcon = trailing,
        interactionSource = source,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Date field. [value] is local midnight millis or null. */
@Composable
fun DateField(
    label: String,
    value: Long?,
    onValueChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    ClickableField(
        value = value?.let { Dates.format(it) }.orEmpty(),
        label = label,
        onClick = { open = true },
        modifier = modifier,
        leading = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
        trailing = if (allowClear && value != null) {
            {
                IconButton(onClick = { onValueChange(null) }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear))
                }
            }
        } else {
            null
        },
    )
    if (open) {
        DatePickDialog(
            initial = value ?: Dates.today(),
            onPick = { onValueChange(it); open = false },
            onDismiss = { open = false },
        )
    }
}

/** Date picker dialog that works in local midnight millis. */
@Composable
fun DatePickDialog(initial: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = Dates.localToUtcDay(initial))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Dates.utcToLocalDay(it)) } ?: onDismiss()
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

/** Dropdown for picking one value from a list. */
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

/** A row of chips for picking one value (scrolls sideways when long). */
@Composable
fun <T> ChipSelector(
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(optionLabel(option)) },
            )
        }
    }
}

/** "Delete this?" confirmation. */
@Composable
fun ConfirmDeleteDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_delete_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
