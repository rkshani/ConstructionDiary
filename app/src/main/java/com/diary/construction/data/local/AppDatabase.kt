package com.diary.construction.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.diary.construction.data.local.dao.ContractorDao
import com.diary.construction.data.local.dao.ExpenseDao
import com.diary.construction.data.local.dao.LabourDao
import com.diary.construction.data.local.dao.MaterialDao
import com.diary.construction.data.local.dao.PhotoDao
import com.diary.construction.data.local.dao.ProjectDao
import com.diary.construction.data.local.dao.StageDao
import com.diary.construction.data.local.dao.SupplierDao
import com.diary.construction.domain.model.Attendance
import com.diary.construction.domain.model.Contractor
import com.diary.construction.domain.model.ContractorPayment
import com.diary.construction.domain.model.Expense
import com.diary.construction.domain.model.Labour
import com.diary.construction.domain.model.LabourPayment
import com.diary.construction.domain.model.MaterialEntry
import com.diary.construction.domain.model.MaterialType
import com.diary.construction.domain.model.Photo
import com.diary.construction.domain.model.Project
import com.diary.construction.domain.model.Stage
import com.diary.construction.domain.model.Supplier
import com.diary.construction.domain.model.SupplierPayment

/** Enums are stored by name (Room's built-in enum converter). */
@Database(
    entities = [
        Project::class,
        Stage::class,
        MaterialType::class,
        MaterialEntry::class,
        Supplier::class,
        SupplierPayment::class,
        Labour::class,
        Attendance::class,
        LabourPayment::class,
        Contractor::class,
        ContractorPayment::class,
        Expense::class,
        Photo::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun stageDao(): StageDao
    abstract fun materialDao(): MaterialDao
    abstract fun supplierDao(): SupplierDao
    abstract fun labourDao(): LabourDao
    abstract fun contractorDao(): ContractorDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun photoDao(): PhotoDao

    companion object {
        const val NAME = "construction_diary.db"

        /** Adds the built-in materials (eent, cement, sariya, ret, bajri) on first create. */
        val SEED_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                listOf(
                    "BRICK" to "PIECE",
                    "CEMENT" to "BAG",
                    "STEEL" to "KG",
                    "SAND" to "CFT",
                    "CRUSH" to "CFT",
                ).forEach { (kind, unit) ->
                    db.execSQL(
                        "INSERT INTO material_types (name, kind, unit, isBuiltIn) VALUES ('', ?, ?, 1)",
                        arrayOf<Any>(kind, unit),
                    )
                }
            }
        }
    }
}
