package com.diary.construction.data.export

import android.content.Context
import android.net.Uri
import com.diary.construction.di.IoDispatcher
import com.diary.construction.domain.model.Report
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes reports to CSV or XLSX at a place the user picked (Storage Access Framework).
 * The XLSX is a minimal Office Open XML file written by hand, so no extra library is needed.
 */
@Singleton
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun writeCsv(uri: Uri, reports: List<Report>): Boolean = withContext(io) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { out -> csv(out, reports) } ?: error("no stream")
        }.isSuccess
    }

    suspend fun writeXlsx(uri: Uri, reports: List<Report>): Boolean = withContext(io) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { out -> xlsx(out, reports) } ?: error("no stream")
        }.isSuccess
    }

    // ------------------------------------------------------------------ CSV

    private fun csv(out: OutputStream, reports: List<Report>) {
        val sb = StringBuilder()
        sb.append('﻿') // BOM so Excel reads Urdu text as UTF-8
        reports.forEach { r ->
            sb.appendCsvLine(listOf(r.title))
            sb.appendCsvLine(listOf(r.projectName, r.period))
            sb.append("\r\n")
            r.sections.forEach { sec ->
                if (sec.title.isNotBlank()) sb.appendCsvLine(listOf(sec.title))
                sec.facts.forEach { (k, v) -> sb.appendCsvLine(listOf(k, v)) }
                if (sec.columns.isNotEmpty()) {
                    sb.appendCsvLine(sec.columns)
                    sec.rows.forEach { sb.appendCsvLine(it) }
                    sec.totalRow?.let { sb.appendCsvLine(it) }
                }
                sb.append("\r\n")
            }
        }
        out.write(sb.toString().toByteArray(Charsets.UTF_8))
    }

    private fun StringBuilder.appendCsvLine(cells: List<String>) {
        cells.forEachIndexed { i, c ->
            if (i > 0) append(',')
            if (c.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
                append('"').append(c.replace("\"", "\"\"")).append('"')
            } else {
                append(c)
            }
        }
        append("\r\n")
    }

    // ------------------------------------------------------------------ XLSX

    private class Sheet(val name: String, val rows: List<Pair<List<String>, Boolean>>, val numeric: List<Set<Int>>)

    private fun buildSheets(reports: List<Report>): List<Sheet> {
        val used = mutableSetOf<String>()
        val sheets = mutableListOf<Sheet>()
        reports.forEach { r ->
            val rows = mutableListOf<Pair<List<String>, Boolean>>()
            val numeric = mutableListOf<Set<Int>>()
            fun add(cells: List<String>, bold: Boolean, nums: Set<Int> = emptySet()) {
                rows += cells to bold
                numeric += nums
            }
            add(listOf(r.title), true)
            add(listOf(r.projectName, r.period), false)
            add(emptyList(), false)
            r.sections.forEach { sec ->
                if (sec.title.isNotBlank()) add(listOf(sec.title), true)
                sec.facts.forEach { (k, v) -> add(listOf(k, v), false, setOf(1)) }
                if (sec.columns.isNotEmpty()) {
                    add(sec.columns, true)
                    sec.rows.forEach { add(it, false, sec.numericColumns) }
                    sec.totalRow?.let { add(it, true, sec.numericColumns) }
                }
                add(emptyList(), false)
            }
            sheets += Sheet(uniqueName(r.title, used), rows, numeric)
        }
        return sheets
    }

    private fun uniqueName(raw: String, used: MutableSet<String>): String {
        val base = raw.replace(Regex("[\\[\\]:*?/\\\\]"), " ").trim().ifBlank { "Sheet" }.take(28)
        var name = base
        var n = 2
        while (!used.add(name.lowercase())) name = "${base.take(25)} ${n++}"
        return name
    }

    private fun xlsx(out: OutputStream, reports: List<Report>) {
        val sheets = buildSheets(reports)
        ZipOutputStream(out).use { zip ->
            fun put(path: String, content: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("[Content_Types].xml", contentTypes(sheets.size))
            put("_rels/.rels", ROOT_RELS)
            put("xl/workbook.xml", workbook(sheets))
            put("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            put("xl/styles.xml", STYLES)
            sheets.forEachIndexed { i, sheet -> put("xl/worksheets/sheet${i + 1}.xml", sheetXml(sheet)) }
        }
    }

    private fun contentTypes(count: Int) = buildString {
        append(XML_HEAD)
        append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
        append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        for (i in 1..count) {
            append("<Override PartName=\"/xl/worksheets/sheet$i.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        }
        append("</Types>")
    }

    private fun workbook(sheets: List<Sheet>) = buildString {
        append(XML_HEAD)
        append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
        append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>")
        sheets.forEachIndexed { i, s -> append("<sheet name=\"${esc(s.name)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>") }
        append("</sheets></workbook>")
    }

    private fun workbookRels(count: Int) = buildString {
        append(XML_HEAD)
        append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
        for (i in 1..count) {
            append("<Relationship Id=\"rId$i\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$i.xml\"/>")
        }
        append("<Relationship Id=\"rId${count + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
        append("</Relationships>")
    }

    private fun sheetXml(sheet: Sheet) = buildString {
        append(XML_HEAD)
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        val maxCols = sheet.rows.maxOfOrNull { it.first.size } ?: 1
        append("<cols>")
        for (c in 1..maxCols) append("<col min=\"$c\" max=\"$c\" width=\"${if (c == 1) 28 else 18}\" customWidth=\"1\"/>")
        append("</cols><sheetData>")
        sheet.rows.forEachIndexed { r, (cells, bold) ->
            val rowNo = r + 1
            append("<row r=\"$rowNo\">")
            cells.forEachIndexed { c, text ->
                val ref = colName(c) + rowNo
                val style = if (bold) " s=\"1\"" else ""
                val number = if (c in sheet.numeric[r]) toNumber(text) else null
                if (number != null) {
                    append("<c r=\"$ref\"$style><v>$number</v></c>")
                } else if (text.isNotEmpty()) {
                    append("<c r=\"$ref\" t=\"inlineStr\"$style><is><t xml:space=\"preserve\">${esc(text)}</t></is></c>")
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    /** "Rs 1,250.50" or "-Rs 300" -> plain number; anything else (units, %) stays text. */
    private fun toNumber(text: String): String? {
        val m = MONEY.matchEntire(text.trim()) ?: return null
        val sign = m.groupValues[1]
        val digits = m.groupValues[2].replace(",", "")
        return if (digits.isEmpty()) null else sign + digits
    }

    private fun colName(index: Int): String {
        var i = index
        val sb = StringBuilder()
        do {
            sb.insert(0, ('A' + i % 26))
            i = i / 26 - 1
        } while (i >= 0)
        return sb.toString()
    }

    private fun esc(s: String) = buildString {
        // Control characters are not allowed in XML.
        s.filter { it >= ' ' || it == '\n' || it == '\t' }.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }

    private companion object {
        val MONEY = Regex("^(-?)[^\\d\\-]{0,5}([\\d,]+(?:\\.\\d+)?)$")
        const val XML_HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        const val ROOT_RELS = XML_HEAD +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
            "</Relationships>"
        const val STYLES = XML_HEAD +
            "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
            "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
            "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>" +
            "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
            "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs>" +
            "</styleSheet>"
    }
}
