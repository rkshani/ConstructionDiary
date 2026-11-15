package com.diary.construction.core.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Shares app files (reports, exports) through our FileProvider. */
object ShareFiles {

    private fun uriFor(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun sendIntent(context: Context, file: File, mime: String, text: String?): Intent {
        val uri = uriFor(context, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            if (!text.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, text)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** Opens the Android share sheet. */
    fun share(context: Context, file: File, mime: String, chooserTitle: String, text: String? = null) {
        val chooser = Intent.createChooser(sendIntent(context, file, mime, text), chooserTitle)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(chooser) }
    }

    /** Sends straight to WhatsApp (or WhatsApp Business). Returns false if neither is installed. */
    fun shareToWhatsApp(context: Context, file: File, mime: String, text: String? = null): Boolean {
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            val intent = sendIntent(context, file, mime, text).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
                // try the next one
            }
        }
        return false
    }

    /** Opens the file in a viewer app (PDF reader etc.). */
    fun open(context: Context, file: File, mime: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uriFor(context, file), mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}
