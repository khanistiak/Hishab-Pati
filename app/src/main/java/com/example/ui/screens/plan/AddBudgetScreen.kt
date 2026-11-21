package com.example.ui.screens.plan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.local.entity.BudgetEntity
import com.example.ui.MainViewModel

@Composable
fun AddBudgetScreen(
    viewModel: MainViewModel,
    initialBudget: BudgetEntity? = null,
    defaultIsOverall: Boolean = false,
    defaultCategory: String? = null,
    defaultMonthKey: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    AddEditBudgetScreen(
        viewModel = viewModel,
        initialBudget = initialBudget,
        defaultIsOverall = defaultIsOverall,
        defaultCategory = defaultCategory,
        defaultMonthKey = defaultMonthKey,
        onBack = onBack,
        modifier = modifier
    )
}
