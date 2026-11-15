package com.diary.construction.domain.model

/** Report kinds shown on the Reports screen. */
enum class ReportType { SUMMARY, STAGES, MATERIAL, LABOUR, SUPPLIERS, CONTRACTORS, EXPENSES }

/** Inclusive date range; null ends mean "no limit". */
data class DateRange(val from: Long? = null, val to: Long? = null) {
    operator fun contains(date: Long): Boolean = (from == null || date >= from) && (to == null || date <= to)
    val isAll: Boolean get() = from == null && to == null
}

/**
 * A finished report as plain text tables. The same object is drawn on screen,
 * written to PDF and exported to CSV / XLSX.
 */
data class Report(
    val title: String,
    val projectName: String,
    val period: String,
    val generatedOn: String,
    val sections: List<ReportSection>,
)

data class ReportSection(
    val title: String,
    /** Label / value lines shown above the table. */
    val facts: List<Pair<String, String>> = emptyList(),
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    /** Optional bold last row. */
    val totalRow: List<String>? = null,
    /** Column indexes that hold numbers (right aligned). */
    val numericColumns: Set<Int> = emptySet(),
)
