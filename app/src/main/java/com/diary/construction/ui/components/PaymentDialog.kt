package com.diary.construction.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.diary.construction.R
import com.diary.construction.core.util.Dates
import com.diary.construction.core.util.Money

/** Result of the payment dialog. */
data class PaymentInput(val amount: Long, val date: Long, val note: String)

/**
 * "Add payment" dialog: amount, date and note. [extra] adds more fields (like a project
 * or payment type picker) above the note.
 */
@Composable
fun PaymentDialog(
    title: String,
    onSave: (PaymentInput) -> Unit,
    onDismiss: () -> Unit,
    initialAmount: Long = 0,
    extra: @Composable () -> Unit = {},
) {
    var amount by rememberSaveable { mutableStateOf(Money.toInput(initialAmount)) }
    var date by rememberSaveable { mutableLongStateOf(Dates.today()) }
    var note by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MoneyField(
                    value = amount,
                    onValueChange = { amount = it; error = false },
                    label = stringResource(R.string.amount),
                    isError = error,
                    supportingText = if (error) stringResource(R.string.error_amount) else null,
                )
                DateField(label = stringResource(R.string.date), value = date, onValueChange = { if (it != null) date = it })
                extra()
                FormTextField(value = note, onValueChange = { note = it }, label = stringResource(R.string.notes))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val paisa = Money.parse(amount) ?: 0
                if (paisa <= 0) {
                    error = true
                } else {
                    onSave(PaymentInput(paisa, date, note.trim()))
                    onDismiss()
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
