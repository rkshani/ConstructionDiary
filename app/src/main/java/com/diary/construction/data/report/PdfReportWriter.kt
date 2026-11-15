package com.diary.construction.data.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.diary.construction.R
import com.diary.construction.core.util.Dates
import com.diary.construction.data.settings.LocaleStore
import com.diary.construction.di.IoDispatcher
import com.diary.construction.domain.model.Report
import com.diary.construction.domain.model.ReportSection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Draws a [Report] into an A4 PDF with android.graphics.pdf.PdfDocument. */
@Singleton
class PdfReportWriter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val pageW = 595
    private val pageH = 842
    private val margin = 36f
    private val contentW = pageW - 2 * margin

    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
        color = Color.rgb(0xE6, 0x51, 0x00)
    }
    private val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = Color.DKGRAY }
    private val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        color = Color.BLACK
    }
    private val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = Color.BLACK }
    private val boldCellPaint = TextPaint(cellPaint).apply { typeface = Typeface.DEFAULT_BOLD }
    private val headerFill = Paint().apply { color = Color.rgb(0xFF, 0xDB, 0xC8) }
    private val zebraFill = Paint().apply { color = Color.rgb(0xF5, 0xF5, 0xF5) }
    private val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }

    private val rowH = 15f
    private val mutex = Mutex()

    private lateinit var doc: PdfDocument
    private var page: PdfDocument.Page? = null
    private lateinit var canvas: Canvas
    private var y = 0f
    private var pageNo = 0
    private var footer = ""

    /** Writes the PDF into cache/reports and returns the file. */
    suspend fun write(report: Report, fileName: String): File = mutex.withLock { withContext(io) { writeLocked(report, fileName) } }

    private fun writeLocked(report: Report, fileName: String): File {
        val ctx = LocaleStore.wrap(context)
        footer = ctx.getString(R.string.app_name) + " · " + report.projectName
        doc = PdfDocument()
        pageNo = 0
        newPage()
        drawHeader(report)
        report.sections.forEach { drawSection(it) }
        finishPage()
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun newPage() {
        pageNo++
        val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create()
        val p = doc.startPage(info)
        page = p
        canvas = p.canvas
        y = margin
    }

    private fun finishPage() {
        val p = page ?: return
        canvas.drawText("$footer   ·   $pageNo", margin, pageH - 18f, subPaint)
        doc.finishPage(p)
        page = null
    }

    private fun ensureSpace(height: Float) {
        if (y + height > pageH - margin - 10f) {
            finishPage()
            newPage()
        }
    }

    private fun drawHeader(r: Report) {
        y += 18f
        canvas.drawText(r.title, margin, y, titlePaint)
        y += 16f
        canvas.drawText(r.projectName, margin, y, sectionPaint)
        y += 14f
        canvas.drawText("${r.period}   ·   ${Dates.format(Dates.now())}", margin, y, subPaint)
        y += 8f
        canvas.drawLine(margin, y, pageW - margin, y, linePaint)
        y += 12f
    }

    private fun drawSection(sec: ReportSection) {
        ensureSpace(40f)
        if (sec.title.isNotBlank()) {
            y += 8f
            canvas.drawText(fit(sec.title, sectionPaint, contentW), margin, y + 10f, sectionPaint)
            y += 18f
        }
        sec.facts.forEach { (label, value) ->
            ensureSpace(rowH)
            canvas.drawText(fit(label, cellPaint, contentW * 0.6f), margin, y + 10f, cellPaint)
            val v = fit(value, boldCellPaint, contentW * 0.4f)
            canvas.drawText(v, pageW - margin - boldCellPaint.measureText(v), y + 10f, boldCellPaint)
            y += rowH
        }
        if (sec.columns.isNotEmpty()) {
            y += 4f
            val widths = columnWidths(sec)
            drawRow(sec.columns, widths, sec.numericColumns, boldCellPaint, headerFill)
            sec.rows.forEachIndexed { i, row ->
                if (y + rowH > pageH - margin - 10f) {
                    finishPage()
                    newPage()
                    drawRow(sec.columns, widths, sec.numericColumns, boldCellPaint, headerFill)
                }
                drawRow(row, widths, sec.numericColumns, cellPaint, if (i % 2 == 1) zebraFill else null)
            }
            sec.totalRow?.let {
                ensureSpace(rowH)
                canvas.drawLine(margin, y, pageW - margin, y, linePaint)
                drawRow(it, widths, sec.numericColumns, boldCellPaint, null)
            }
        }
        y += 8f
    }

    private fun drawRow(cells: List<String>, widths: FloatArray, numeric: Set<Int>, paint: TextPaint, fill: Paint?) {
        ensureSpace(rowH)
        if (fill != null) canvas.drawRect(margin, y, pageW - margin, y + rowH, fill)
        var x = margin
        widths.forEachIndexed { i, w ->
            val text = fit(cells.getOrElse(i) { "" }, paint, w - 6f)
            val tx = if (i in numeric) x + w - 3f - paint.measureText(text) else x + 3f
            canvas.drawText(text, tx, y + 11f, paint)
            x += w
        }
        y += rowH
    }

    /** Widths by the longest text per column, scaled to the page. */
    private fun columnWidths(sec: ReportSection): FloatArray {
        val n = sec.columns.size
        val all = listOf(sec.columns) + sec.rows + listOfNotNull(sec.totalRow)
        val want = FloatArray(n) { i ->
            all.maxOf { row -> boldCellPaint.measureText(row.getOrElse(i) { "" }) }.coerceIn(30f, 220f) + 8f
        }
        val sum = want.sum()
        return FloatArray(n) { want[it] * contentW / sum }
    }

    private fun fit(text: String, paint: TextPaint, width: Float): String =
        TextUtils.ellipsize(text, paint, width.coerceAtLeast(10f), TextUtils.TruncateAt.END).toString()
}
