package com.diary.construction.domain.model

/*
 * Enum names are saved in the database and in settings. Never rename an entry;
 * only add new ones at the end.
 */

enum class ProjectStatus { PLANNING, IN_PROGRESS, ON_HOLD, COMPLETED }

enum class AreaUnit {
    MARLA, KANAL, SQ_FT;

    /** Square feet in one unit. 1 marla = 225 sq ft, 1 kanal = 20 marla. */
    val sqFt: Long
        get() = when (this) {
            MARLA -> 225
            KANAL -> 4500
            SQ_FT -> 1
        }
}

enum class StageType { FOUNDATION, STRUCTURE, ROOF, PLASTER, ELECTRIC, PLUMBING, TILES, PAINT, FINISHING, CUSTOM }

enum class StageStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

/** Built-in materials: eent, cement, sariya, ret, bajri. CUSTOM = user made. */
enum class MaterialKind { BRICK, CEMENT, STEEL, SAND, CRUSH, CUSTOM }

enum class MaterialUnit { PIECE, BAG, KG, TON, CFT, TROLLEY, TRUCK, SQ_FT, RUNNING_FT, LITRE, BOX, OTHER }

/** RECEIVED = bought / delivered to site (adds stock and cost). USED = consumed on site. */
enum class MaterialEntryType { RECEIVED, USED }

enum class LabourRole { MISTRI, LABOUR, HELPER, ELECTRICIAN, PLUMBER, CARPENTER, PAINTER, OTHER }

enum class AttendanceStatus {
    PRESENT, HALF_DAY, ABSENT;

    /** Share of the daily wage, in percent. */
    val wagePercent: Int
        get() = when (this) {
            PRESENT -> 100
            HALF_DAY -> 50
            ABSENT -> 0
        }
}

enum class LabourPaymentType { ADVANCE, PAYMENT, SETTLEMENT }

enum class AgreementType { FIXED, PER_SQ_FT }

enum class ExpenseCategory { TRANSPORT, TOOLS, NAQSHA, NOC, ELECTRICITY, WATER, CHAI_PANI, RENT, OTHER }

enum class UserRole { OWNER, THEKEDAR }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppLanguage(val tag: String?) { SYSTEM(null), ENGLISH("en"), URDU("ur") }

enum class SpendCategory { MATERIAL, LABOUR, CONTRACTOR, OTHER }
