package com.diary.construction.ui.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diary.construction.domain.model.Expense
import com.diary.construction.domain.model.ExpenseCategory
import com.diary.construction.domain.model.Project
import com.diary.construction.domain.model.Stage
import com.diary.construction.domain.repository.ExpenseRepository
import com.diary.construction.domain.repository.SettingsRepository
import com.diary.construction.domain.repository.StageRepository
import com.diary.construction.domain.usecase.ObserveSelectedProjectUseCase
import com.diary.construction.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpensesState(
    val projects: List<Project>,
    val project: Project?,
    val stages: List<Stage>,
    val expenses: List<Expense>,
    val filter: ExpenseCategory?,
) {
    val total: Long get() = expenses.sumOf { it.amount }
    val byCategory: List<Pair<ExpenseCategory, Long>>
        get() = expenses.groupBy { it.category }.map { (c, list) -> c to list.sumOf { it.amount } }.sortedByDescending { it.second }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val expenses: ExpenseRepository,
    private val stages: StageRepository,
    private val settings: SettingsRepository,
    selected: ObserveSelectedProjectUseCase,
) : ViewModel() {
    /** Set when opened from a project; 0 when opened as the Expenses tab. */
    private val routeProjectId: Long = savedState.get<Long>(Routes.ARG_PROJECT_ID) ?: 0L
    val canSwitchProject = routeProjectId == 0L

    private val filter = MutableStateFlow<ExpenseCategory?>(null)

    val state: StateFlow<ExpensesState?> = selected(routeProjectId).flatMapLatest { (projects, project) ->
        if (project == null) {
            flowOf(ExpensesState(projects, null, emptyList(), emptyList(), null))
        } else {
            combine(expenses.observeExpenses(project.id), stages.observeStages(project.id), filter) { list, stageList, f ->
                ExpensesState(projects, project, stageList, list.filter { f == null || it.category == f }, f)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setFilter(category: ExpenseCategory?) {
        filter.value = category
    }

    fun selectProject(project: Project) {
        viewModelScope.launch { settings.setSelectedProject(project.id) }
    }
}
