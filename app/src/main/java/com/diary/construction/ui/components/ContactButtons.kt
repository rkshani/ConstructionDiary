package com.diary.construction.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.diary.construction.R
import com.diary.construction.core.util.Contact

/** Small call + WhatsApp icons for list rows. */
@Composable
fun ContactIcons(phone: String, message: String = "") {
    if (phone.isBlank()) return
    val context = LocalContext.current
    val missing = stringResource(R.string.whatsapp_missing)
    Row {
        IconButton(onClick = { Contact.call(context, phone) }) {
            Icon(Icons.Outlined.Call, contentDescription = stringResource(R.string.action_call))
        }
        IconButton(onClick = {
            if (!Contact.whatsApp(context, phone, message)) Toast.makeText(context, missing, Toast.LENGTH_SHORT).show()
        }) {
            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = stringResource(R.string.action_whatsapp))
        }
    }
}

/** Bigger buttons for detail screens. */
@Composable
fun ContactButtons(phone: String, message: String = "", modifier: Modifier = Modifier) {
    if (phone.isBlank()) return
    val context = LocalContext.current
    val missing = stringResource(R.string.whatsapp_missing)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { Contact.call(context, phone) }) {
            Icon(Icons.Outlined.Call, contentDescription = null)
            Text("  " + stringResource(R.string.action_call))
        }
        OutlinedButton(onClick = {
            if (!Contact.whatsApp(context, phone, message)) Toast.makeText(context, missing, Toast.LENGTH_SHORT).show()
        }) {
            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null)
            Text("  " + stringResource(R.string.action_whatsapp))
        }
    }
}
