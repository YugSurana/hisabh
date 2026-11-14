package com.hisabh.app.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabh.app.data.local.entity.TransactionEntity
import com.hisabh.app.data.model.Category
import com.hisabh.app.data.model.TransactionType
import com.hisabh.app.data.repository.TransactionRepository
import com.hisabh.app.domain.export.ExportManager
import com.hisabh.app.domain.helper.SmsReaderHelper
import com.hisabh.app.ui.theme.*
import com.hisabh.app.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class HisabhViewModel(
    application: Application,
    private val repository: TransactionRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HisabhUiState())
    val uiState: StateFlow<HisabhUiState> = _uiState.asStateFlow()

    private var transactionCollectionJob: Job? = null

    init {
        updateMonthAndLoad()
        checkPermissionsAndBattery()
    }

    fun previousMonth() {
        val cal = _uiState.value.selectedCalendar.clone() as Calendar
        cal.add(Calendar.MONTH, -1)
        _uiState.update { it.copy(selectedCalendar = cal) }
        updateMonthAndLoad()
    }

    fun nextMonth() {
        val cal = _uiState.value.selectedCalendar.clone() as Calendar
        cal.add(Calendar.MONTH, 1)
        _uiState.update { it.copy(selectedCalendar = cal) }
        updateMonthAndLoad()
    }

    fun jumpToCurrentMonth() {
        val cal = Calendar.getInstance()
        _uiState.update { it.copy(selectedCalendar = cal) }
        updateMonthAndLoad()
    }

    private fun updateMonthAndLoad() {
        val cal = _uiState.value.selectedCalendar
        val monthTitle = DateUtils.formatMonthYear(cal)
        val startTime = DateUtils.getStartOfMonth(cal)
        val endTime = DateUtils.getEndOfMonth(cal)

        _uiState.update { it.copy(monthTitle = monthTitle) }

        transactionCollectionJob?.cancel()
        transactionCollectionJob = viewModelScope.launch {
            repository.getTransactionsBetween(startTime, endTime).collectLatest { transactions ->
                computeDashboardMetrics(transactions)
            }
        }
    }

    private fun computeDashboardMetrics(transactions: List<TransactionEntity>) {
        val totalSpent = transactions
            .filter { it.txType == TransactionType.DEBIT.name }
            .sumOf { it.amount }

        val totalReceived = transactions
            .filter { it.txType == TransactionType.CREDIT.name }
            .sumOf { it.amount }

        val netBalance = totalReceived - totalSpent

        val debitCount = transactions.count { it.txType == TransactionType.DEBIT.name }
        val creditCount = transactions.count { it.txType == TransactionType.CREDIT.name }

        // Category breakdown calculation
        val categorySpends = transactions
            .filter { it.txType == TransactionType.DEBIT.name }
            .groupBy { it.category }
            .map { (catName, list) ->
                val sum = list.sumOf { it.amount }
                val pct = if (totalSpent > 0.0) ((sum / totalSpent) * 100).toFloat() else 0f
                CategorySpend(
                    categoryName = catName,
                    amount = sum,
                    percentage = pct,
                    color = getCategoryColor(catName)
                )
            }
            .sortedByDescending { it.amount }

        // Group by Date for chronological stream
        val grouped = transactions.groupBy { DateUtils.getDateGroupHeader(it.timestamp) }

        _uiState.update {
            it.copy(
                totalSpent = totalSpent,
                totalReceived = totalReceived,
                netBalance = netBalance,
                debitCount = debitCount,
                creditCount = creditCount,
                categoryBreakdown = categorySpends,
                groupedTransactions = grouped,
                allTransactions = transactions
            )
        }
    }

    fun scanPastSms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningSms = true) }
            try {
                val cal = _uiState.value.selectedCalendar
                val startTime = DateUtils.getStartOfMonth(cal)
                val parsed = SmsReaderHelper.readHdfcSmsFromInbox(
                    context = getApplication(),
                    sinceTimestamp = startTime
                )
                val count = repository.insertParsedTransactionsBatch(parsed)
                val message = if (count > 0) {
                    "Imported $count HDFC transactions for ${_uiState.value.monthTitle}"
                } else {
                    "No new HDFC SMS found for ${_uiState.value.monthTitle}"
                }
                _uiState.update { it.copy(userMessage = message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Scan failed: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isScanningSms = false) }
            }
        }
    }

    fun openEditTransaction(transaction: TransactionEntity) {
        _uiState.update { it.copy(editingTransaction = transaction) }
    }

    fun openEditTransactionById(id: Long) {
        viewModelScope.launch {
            val tx = repository.getTransactionById(id)
            if (tx != null) {
                _uiState.update { it.copy(editingTransaction = tx) }
            }
        }
    }

    fun closeEditTransaction() {
        _uiState.update { it.copy(editingTransaction = null) }
    }

    fun saveEditedTransaction(
        updated: TransactionEntity,
        applyToAllFuture: Boolean
    ) {
        viewModelScope.launch {
            repository.updateTransaction(updated, applyToAllFuture)
            _uiState.update { it.copy(editingTransaction = null, userMessage = "Transaction updated") }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            _uiState.update { it.copy(editingTransaction = null, userMessage = "Transaction deleted") }
        }
    }

    fun addManualTransaction(
        amount: Double,
        txType: TransactionType,
        merchantName: String,
        category: String,
        note: String?,
        timestamp: Long
    ) {
        viewModelScope.launch {
            val fakeUtr = "MANUAL_" + UUID.randomUUID().toString().take(12).uppercase()
            val entity = TransactionEntity(
                utrNumber = fakeUtr,
                amount = amount,
                txType = txType.name,
                merchantRaw = merchantName.trim(),
                displayName = merchantName.trim(),
                category = category,
                note = note?.trim()?.ifBlank { null },
                timestamp = timestamp,
                isManual = true
            )
            repository.insertTransaction(entity)
            _uiState.update { it.copy(showAddSheet = false, userMessage = "Transaction added successfully") }
        }
    }

    fun showAddSheet(show: Boolean) {
        _uiState.update { it.copy(showAddSheet = show) }
    }

    fun showExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    fun showOnboardingDialog(show: Boolean) {
        _uiState.update { it.copy(showOnboardingDialog = show) }
    }

    fun dismissHealthBanner() {
        _uiState.update { it.copy(bannerDismissed = true) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                ExportManager.exportAndShareCsv(
                    context = context,
                    monthName = _uiState.value.monthTitle,
                    transactions = _uiState.value.allTransactions
                )
                _uiState.update { it.copy(showExportDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "CSV export failed: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                ExportManager.exportAndSharePdf(
                    context = context,
                    monthName = _uiState.value.monthTitle,
                    transactions = _uiState.value.allTransactions
                )
                _uiState.update { it.copy(showExportDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "PDF export failed: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    fun checkPermissionsAndBattery() {
        val context = getApplication<Application>()
        val hasReceiveSms = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val hasReadSms = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val isPermissionMissing = !hasReceiveSms || !hasReadSms

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatteryOptimized = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm != null) {
            !pm.isIgnoringBatteryOptimizations(context.packageName)
        } else false

        _uiState.update {
            it.copy(
                isSmsPermissionMissing = isPermissionMissing,
                isBatteryOptimizationActive = isBatteryOptimized
            )
        }
    }

    private fun getCategoryColor(categoryName: String): androidx.compose.ui.graphics.Color {
        val cat = Category.fromTitle(categoryName)
        return when (cat) {
            Category.FOOD_AND_DINING -> CatFoodColor
            Category.TRAVEL_AND_FUEL -> CatTravelColor
            Category.GROCERIES -> CatGroceriesColor
            Category.BILLS_AND_RECHARGE -> CatBillsColor
            Category.SHOPPING -> CatShoppingColor
            Category.ENTERTAINMENT -> CatEntertainmentColor
            Category.TRANSFER_AND_FRIENDS -> CatTransferColor
            Category.POCKET_MONEY_RECEIVED -> CatIncomeColor
            Category.OTHER -> CatOtherColor
        }
    }
}

