package com.diary.construction.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens the dialer or WhatsApp. No permissions are needed: the other app does the work. */
object Contact {

    fun call(context: Context, phone: String) {
        if (phone.isBlank()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Opens a WhatsApp chat with [phone] and an optional ready message. Returns false if WhatsApp is missing. */
    fun whatsApp(context: Context, phone: String, message: String = ""): Boolean {
        val number = toInternational(phone)
        if (number.isEmpty()) return false
        val uri = Uri.parse("https://wa.me/$number" + if (message.isNotEmpty()) "?text=${Uri.encode(message)}" else "")
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            val intent = Intent(Intent.ACTION_VIEW, uri).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
                // try the next one
            }
        }
        return false
    }

    /** "0300-1234567" -> "923001234567". Numbers that already have a country code are kept. */
    fun toInternational(phone: String): String {
        var digits = phone.filter { it.isDigit() }
        if (phone.trim().startsWith("+")) return digits
        if (digits.startsWith("00")) return digits.drop(2)
        if (digits.startsWith("0") && digits.length == 11) digits = "92" + digits.drop(1)
        return digits
    }
}
