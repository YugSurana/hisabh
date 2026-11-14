package com.hisabh.app.ui.viewmodel

import androidx.compose.ui.graphics.Color
import com.hisabh.app.data.local.entity.TransactionEntity
import java.util.Calendar

data class CategorySpend(
    val categoryName: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

data class HisabhUiState(
    val selectedCalendar: Calendar = Calendar.getInstance(),
    val monthTitle: String = "",
    val totalSpent: Double = 0.0,
    val totalReceived: Double = 0.0,
    val netBalance: Double = 0.0,
    val debitCount: Int = 0,
    val creditCount: Int = 0,
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val groupedTransactions: Map<String, List<TransactionEntity>> = emptyMap(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val editingTransaction: TransactionEntity? = null,
    val isScanningSms: Boolean = false,
    val isExporting: Boolean = false,
    val showAddSheet: Boolean = false,
    val showExportDialog: Boolean = false,
    val showOnboardingDialog: Boolean = false,
    val isSmsPermissionMissing: Boolean = false,
    val isBatteryOptimizationActive: Boolean = false,
    val bannerDismissed: Boolean = false,
    val userMessage: String? = null
)

