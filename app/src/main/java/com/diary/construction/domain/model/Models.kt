package com.diary.construction.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.diary.construction.core.util.Money

/*
 * Domain models. They carry Room annotations only (room-common is plain Kotlin, no Android
 * code), so the same classes are used by the database and the rest of the app.
 *
 * Money = Long paisa. Quantity / area = Long with scale 100. Dates = epoch millis.
 */

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val siteAddress: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val startDate: Long,
    val budget: Long = 0,
    /** Area with scale 100 in [areaUnit]. */
    val area: Long = 0,
    val areaUnit: AreaUnit = AreaUnit.MARLA,
    val status: ProjectStatus = ProjectStatus.IN_PROGRESS,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    /** Area in square feet with scale 100. */
    val areaSqFt: Long get() = area * areaUnit.sqFt
}

@Entity(
    tableName = "stages",
    foreignKeys = [ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class Stage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val type: StageType,
    /** Custom name. Empty for built-in stages (their name comes from strings.xml). */
    val name: String = "",
    val budget: Long = 0,
    val status: StageStatus = StageStatus.NOT_STARTED,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val sortOrder: Int = 0,
    val notes: String = "",
)

@Entity(tableName = "material_types")
data class MaterialType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Custom name. Empty for built-in materials (their name comes from strings.xml). */
    val name: String = "",
    val kind: MaterialKind,
    val unit: MaterialUnit,
    val isBuiltIn: Boolean = false,
)

@Entity(
    tableName = "material_entries",
    foreignKeys = [
        ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MaterialType::class, ["id"], ["materialTypeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Supplier::class, ["id"], ["supplierId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(Stage::class, ["id"], ["stageId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("projectId"), Index("materialTypeId"), Index("supplierId"), Index("stageId")],
)
data class MaterialEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val materialTypeId: Long,
    val entryType: MaterialEntryType = MaterialEntryType.RECEIVED,
    /** Scale 100. */
    val quantity: Long,
    /** Paisa per one unit. 0 for USED entries. */
    val rate: Long = 0,
    /** Paisa. rate x quantity (can be edited). 0 for USED entries. */
    val total: Long = 0,
    val supplierId: Long? = null,
    val stageId: Long? = null,
    val date: Long,
    val billPhotoPath: String? = null,
    val note: String = "",
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    /** What they sell, e.g. "Cement, sariya". */
    val materials: String = "",
    /** Next payment promise date, used by reminders. */
    val paymentDueDate: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "supplier_payments",
    foreignKeys = [
        ForeignKey(Supplier::class, ["id"], ["supplierId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("supplierId"), Index("projectId")],
)
data class SupplierPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val projectId: Long? = null,
    val date: Long,
    val amount: Long,
    val note: String = "",
)

@Entity(
    tableName = "labours",
    foreignKeys = [ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class Labour(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val phone: String = "",
    val role: LabourRole = LabourRole.LABOUR,
    /** Dihari in paisa. */
    val dailyWage: Long,
    val active: Boolean = true,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(Labour::class, ["id"], ["labourId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["labourId", "date"], unique = true), Index("projectId"), Index("date")],
)
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val labourId: Long,
    val projectId: Long,
    /** Local midnight of the day. */
    val date: Long,
    val status: AttendanceStatus,
    /** Daily wage on that day (paisa), so later wage changes do not change old days. */
    val wage: Long,
) {
    val earned: Long get() = wage * status.wagePercent / 100
}

@Entity(
    tableName = "labour_payments",
    foreignKeys = [
        ForeignKey(Labour::class, ["id"], ["labourId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("labourId"), Index("projectId")],
)
data class LabourPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val labourId: Long,
    val projectId: Long,
    val date: Long,
    val amount: Long,
    val type: LabourPaymentType = LabourPaymentType.PAYMENT,
    val note: String = "",
)

@Entity(
    tableName = "contractors",
    foreignKeys = [ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class Contractor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val phone: String = "",
    /** Work given, e.g. "Grey structure", "Electric". */
    val work: String = "",
    val agreementType: AgreementType = AgreementType.FIXED,
    /** FIXED: total amount. PER_SQ_FT: paisa per sq ft. */
    val rate: Long,
    /** Covered area in sq ft, scale 100 (only for PER_SQ_FT). */
    val areaSqFt: Long = 0,
    /** 0..100 */
    val workDonePercent: Int = 0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val agreedAmount: Long
        get() = when (agreementType) {
            AgreementType.FIXED -> rate
            AgreementType.PER_SQ_FT -> Money.multiply(rate, areaSqFt)
        }

    /** Value of the work done so far. */
    val workDoneValue: Long get() = agreedAmount * workDonePercent.coerceIn(0, 100) / 100
}

@Entity(
    tableName = "contractor_payments",
    foreignKeys = [
        ForeignKey(Contractor::class, ["id"], ["contractorId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("contractorId"), Index("projectId")],
)
data class ContractorPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contractorId: Long,
    val projectId: Long,
    val date: Long,
    val amount: Long,
    val note: String = "",
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Stage::class, ["id"], ["stageId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("projectId"), Index("stageId")],
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val category: ExpenseCategory,
    val amount: Long,
    val stageId: Long? = null,
    val date: Long,
    val note: String = "",
    val billPhotoPath: String? = null,
)

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Stage::class, ["id"], ["stageId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("projectId"), Index("stageId"), Index("date")],
)
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val stageId: Long? = null,
    /** Local midnight of the day the photo is for. */
    val date: Long,
    val path: String,
    val caption: String = "",
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
)
