package com.diary.construction.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.diary.construction.R
import com.diary.construction.domain.model.Project

/** Dropdown to switch the project shown on a tab. */
@Composable
fun ProjectPicker(
    projects: List<Project>,
    selected: Project,
    onSelect: (Project) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownField(
        label = stringResource(R.string.project),
        options = projects,
        selected = selected,
        optionLabel = { it.name },
        onSelect = onSelect,
        modifier = modifier,
    )
}
