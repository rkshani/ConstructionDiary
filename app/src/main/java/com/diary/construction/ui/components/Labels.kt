package com.diary.construction.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.diary.construction.R
import com.diary.construction.domain.model.AgreementType
import com.diary.construction.domain.model.AppLanguage
import com.diary.construction.domain.model.AreaUnit
import com.diary.construction.domain.model.AttendanceStatus
import com.diary.construction.domain.model.ExpenseCategory
import com.diary.construction.domain.model.LabourPaymentType
import com.diary.construction.domain.model.LabourRole
import com.diary.construction.domain.model.MaterialEntryType
import com.diary.construction.domain.model.MaterialKind
import com.diary.construction.domain.model.MaterialType
import com.diary.construction.domain.model.MaterialUnit
import com.diary.construction.domain.model.ProjectStatus
import com.diary.construction.domain.model.SpendCategory
import com.diary.construction.domain.model.Stage
import com.diary.construction.domain.model.StageStatus
import com.diary.construction.domain.model.StageType
import com.diary.construction.domain.model.ThemeMode

/** String resources for every enum shown in the UI. */

@get:StringRes
val ProjectStatus.labelRes: Int
    get() = when (this) {
        ProjectStatus.PLANNING -> R.string.status_planning
        ProjectStatus.IN_PROGRESS -> R.string.status_in_progress
        ProjectStatus.ON_HOLD -> R.string.status_on_hold
        ProjectStatus.COMPLETED -> R.string.status_completed
    }

@get:StringRes
val AreaUnit.labelRes: Int
    get() = when (this) {
        AreaUnit.MARLA -> R.string.unit_marla
        AreaUnit.KANAL -> R.string.unit_kanal
        AreaUnit.SQ_FT -> R.string.unit_sq_ft
    }

@get:StringRes
val StageType.labelRes: Int
    get() = when (this) {
        StageType.FOUNDATION -> R.string.stage_foundation
        StageType.STRUCTURE -> R.string.stage_structure
        StageType.ROOF -> R.string.stage_roof
        StageType.PLASTER -> R.string.stage_plaster
        StageType.ELECTRIC -> R.string.stage_electric
        StageType.PLUMBING -> R.string.stage_plumbing
        StageType.TILES -> R.string.stage_tiles
        StageType.PAINT -> R.string.stage_paint
        StageType.FINISHING -> R.string.stage_finishing
        StageType.CUSTOM -> R.string.stage_custom
    }

@get:StringRes
val StageStatus.labelRes: Int
    get() = when (this) {
        StageStatus.NOT_STARTED -> R.string.stage_status_not_started
        StageStatus.IN_PROGRESS -> R.string.stage_status_in_progress
        StageStatus.COMPLETED -> R.string.stage_status_completed
    }

@get:StringRes
val MaterialKind.labelRes: Int
    get() = when (this) {
        MaterialKind.BRICK -> R.string.material_brick
        MaterialKind.CEMENT -> R.string.material_cement
        MaterialKind.STEEL -> R.string.material_steel
        MaterialKind.SAND -> R.string.material_sand
        MaterialKind.CRUSH -> R.string.material_crush
        MaterialKind.CUSTOM -> R.string.material_custom
    }

@get:StringRes
val MaterialUnit.labelRes: Int
    get() = when (this) {
        MaterialUnit.PIECE -> R.string.mu_piece
        MaterialUnit.BAG -> R.string.mu_bag
        MaterialUnit.KG -> R.string.mu_kg
        MaterialUnit.TON -> R.string.mu_ton
        MaterialUnit.CFT -> R.string.mu_cft
        MaterialUnit.TROLLEY -> R.string.mu_trolley
        MaterialUnit.TRUCK -> R.string.mu_truck
        MaterialUnit.SQ_FT -> R.string.mu_sq_ft
        MaterialUnit.RUNNING_FT -> R.string.mu_running_ft
        MaterialUnit.LITRE -> R.string.mu_litre
        MaterialUnit.BOX -> R.string.mu_box
        MaterialUnit.OTHER -> R.string.mu_other
    }

@get:StringRes
val MaterialEntryType.labelRes: Int
    get() = when (this) {
        MaterialEntryType.RECEIVED -> R.string.entry_received
        MaterialEntryType.USED -> R.string.entry_used
    }

@get:StringRes
val LabourRole.labelRes: Int
    get() = when (this) {
        LabourRole.MISTRI -> R.string.lr_mistri
        LabourRole.LABOUR -> R.string.lr_labour
        LabourRole.HELPER -> R.string.lr_helper
        LabourRole.ELECTRICIAN -> R.string.lr_electrician
        LabourRole.PLUMBER -> R.string.lr_plumber
        LabourRole.CARPENTER -> R.string.lr_carpenter
        LabourRole.PAINTER -> R.string.lr_painter
        LabourRole.OTHER -> R.string.lr_other
    }

@get:StringRes
val AttendanceStatus.labelRes: Int
    get() = when (this) {
        AttendanceStatus.PRESENT -> R.string.att_present
        AttendanceStatus.HALF_DAY -> R.string.att_half
        AttendanceStatus.ABSENT -> R.string.att_absent
    }

@get:StringRes
val LabourPaymentType.labelRes: Int
    get() = when (this) {
        LabourPaymentType.ADVANCE -> R.string.lp_advance
        LabourPaymentType.PAYMENT -> R.string.lp_payment
        LabourPaymentType.SETTLEMENT -> R.string.lp_settlement
    }

@get:StringRes
val AgreementType.labelRes: Int
    get() = when (this) {
        AgreementType.FIXED -> R.string.agreement_fixed
        AgreementType.PER_SQ_FT -> R.string.agreement_per_sqft
    }

@get:StringRes
val ExpenseCategory.labelRes: Int
    get() = when (this) {
        ExpenseCategory.TRANSPORT -> R.string.ec_transport
        ExpenseCategory.TOOLS -> R.string.ec_tools
        ExpenseCategory.NAQSHA -> R.string.ec_naqsha
        ExpenseCategory.NOC -> R.string.ec_noc
        ExpenseCategory.ELECTRICITY -> R.string.ec_electricity
        ExpenseCategory.WATER -> R.string.ec_water
        ExpenseCategory.CHAI_PANI -> R.string.ec_chai_pani
        ExpenseCategory.RENT -> R.string.ec_rent
        ExpenseCategory.OTHER -> R.string.ec_other
    }

@get:StringRes
val SpendCategory.labelRes: Int
    get() = when (this) {
        SpendCategory.MATERIAL -> R.string.cat_material
        SpendCategory.LABOUR -> R.string.cat_labour
        SpendCategory.CONTRACTOR -> R.string.cat_contractor
        SpendCategory.OTHER -> R.string.cat_other
    }

@get:StringRes
val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

@get:StringRes
val AppLanguage.labelRes: Int
    get() = when (this) {
        AppLanguage.SYSTEM -> R.string.lang_system
        AppLanguage.ENGLISH -> R.string.lang_english
        AppLanguage.URDU -> R.string.lang_urdu
    }

/** Built-in stages use the translated name, custom ones their own name. */
@Composable
@ReadOnlyComposable
fun Stage.displayName(): String =
    if (type == StageType.CUSTOM || name.isNotBlank()) name.ifBlank { stringResource(type.labelRes) } else stringResource(type.labelRes)

@Composable
@ReadOnlyComposable
fun MaterialType.displayName(): String =
    if (kind == MaterialKind.CUSTOM || name.isNotBlank()) name.ifBlank { stringResource(kind.labelRes) } else stringResource(kind.labelRes)
