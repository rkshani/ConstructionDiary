package com.diary.construction.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.diary.construction.R
import com.diary.construction.domain.usecase.Estimator
import com.diary.construction.ui.components.ChipSelector
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.InfoCard
import com.diary.construction.ui.components.LabelValue
import com.diary.construction.ui.components.NumberField
import java.util.Locale

/** Quick estimator: bricks / cement / sand for a wall, cement / sand / crush / steel for a slab. */
@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(topBar = { AppTopBar(stringResource(R.string.section_calculator), onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.calc_wall)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.calc_slab)) })
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (tab == 0) WallTab() else SlabTab()
                Text(
                    stringResource(R.string.calc_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun num(text: String): Double = text.toDoubleOrNull() ?: 0.0

private fun fmt(value: Double, decimals: Int = 1): String =
    if (value == 0.0) "0" else String.format(Locale.US, "%,.${decimals}f", value)

@Composable
private fun WallTab() {
    var length by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var openings by rememberSaveable { mutableStateOf("") }
    var thickness by rememberSaveable { mutableDoubleStateOf(9.0) }
    var ratio by rememberSaveable { mutableDoubleStateOf(6.0) }
    var wastage by rememberSaveable { mutableStateOf("5") }
    val ft = stringResource(R.string.unit_ft)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(length, { length = it }, stringResource(R.string.calc_length), Modifier.weight(1f), suffix = ft)
        NumberField(height, { height = it }, stringResource(R.string.calc_height), Modifier.weight(1f), suffix = ft)
    }
    Text(stringResource(R.string.calc_thickness))
    ChipSelector(
        options = listOf(4.5, 9.0, 13.5),
        selected = thickness,
        optionLabel = { "${fmt(it)}\"" },
        onSelect = { thickness = it },
    )
    NumberField(openings, { openings = it }, stringResource(R.string.calc_openings), suffix = stringResource(R.string.mu_sq_ft))
    Text(stringResource(R.string.calc_mortar))
    ChipSelector(
        options = listOf(4.0, 5.0, 6.0),
        selected = ratio,
        optionLabel = { "1:${it.toInt()}" },
        onSelect = { ratio = it },
    )
    NumberField(wastage, { wastage = it }, stringResource(R.string.calc_wastage), suffix = "%")

    val r = Estimator.wall(
        Estimator.WallInput(
            lengthFt = num(length),
            heightFt = num(height),
            thicknessIn = thickness,
            openingsSqFt = num(openings),
            sandRatio = ratio,
            wastagePercent = num(wastage),
        ),
    )
    InfoCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Text(stringResource(R.string.calc_you_need), style = MaterialTheme.typography.titleMedium)
        LabelValue(stringResource(R.string.calc_volume), "${fmt(r.volumeCft)} ${stringResource(R.string.mu_cft)}")
        LabelValue(stringResource(R.string.material_brick), "${String.format(Locale.US, "%,d", r.bricks)} ${stringResource(R.string.mu_piece)}", bold = true)
        LabelValue(stringResource(R.string.material_cement), "${fmt(r.cementBags)} ${stringResource(R.string.mu_bag)}", bold = true)
        LabelValue(stringResource(R.string.material_sand), "${fmt(r.sandCft)} ${stringResource(R.string.mu_cft)}", bold = true)
    }
}

@Composable
private fun SlabTab() {
    var length by rememberSaveable { mutableStateOf("") }
    var width by rememberSaveable { mutableStateOf("") }
    var thickness by rememberSaveable { mutableStateOf("5") }
    var mix by rememberSaveable { mutableIntStateOf(0) }
    var steel by rememberSaveable { mutableStateOf("2.2") }
    var wastage by rememberSaveable { mutableStateOf("5") }
    val ft = stringResource(R.string.unit_ft)
    val mixes = listOf(2.0 to 4.0, 1.5 to 3.0, 3.0 to 6.0)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(length, { length = it }, stringResource(R.string.calc_length), Modifier.weight(1f), suffix = ft)
        NumberField(width, { width = it }, stringResource(R.string.calc_width), Modifier.weight(1f), suffix = ft)
    }
    NumberField(thickness, { thickness = it }, stringResource(R.string.calc_slab_thickness), suffix = stringResource(R.string.unit_inch))
    Text(stringResource(R.string.calc_mix))
    ChipSelector(
        options = mixes.indices.toList(),
        selected = mix,
        optionLabel = { i -> "1:${fmt(mixes[i].first)}:${fmt(mixes[i].second)}".replace(".0", "") },
        onSelect = { mix = it },
    )
    NumberField(steel, { steel = it }, stringResource(R.string.calc_steel_rate), supportingText = stringResource(R.string.calc_steel_hint))
    NumberField(wastage, { wastage = it }, stringResource(R.string.calc_wastage), suffix = "%")

    val r = Estimator.slab(
        Estimator.SlabInput(
            lengthFt = num(length),
            widthFt = num(width),
            thicknessIn = num(thickness),
            sandRatio = mixes[mix].first,
            crushRatio = mixes[mix].second,
            steelKgPerCft = num(steel),
            wastagePercent = num(wastage),
        ),
    )
    InfoCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Text(stringResource(R.string.calc_you_need), style = MaterialTheme.typography.titleMedium)
        LabelValue(stringResource(R.string.calc_volume), "${fmt(r.volumeCft)} ${stringResource(R.string.mu_cft)}")
        LabelValue(stringResource(R.string.material_cement), "${fmt(r.cementBags)} ${stringResource(R.string.mu_bag)}", bold = true)
        LabelValue(stringResource(R.string.material_sand), "${fmt(r.sandCft)} ${stringResource(R.string.mu_cft)}", bold = true)
        LabelValue(stringResource(R.string.material_crush), "${fmt(r.crushCft)} ${stringResource(R.string.mu_cft)}", bold = true)
        LabelValue(stringResource(R.string.material_steel), "${fmt(r.steelKg)} ${stringResource(R.string.mu_kg)}", bold = true)
    }
}
