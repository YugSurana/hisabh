package com.hisabh.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabh.app.data.local.entity.TransactionEntity
import com.hisabh.app.data.model.TransactionType
import com.hisabh.app.ui.screens.components.AddTransactionSheet
import com.hisabh.app.ui.screens.components.CategoryBreakdownBar
import com.hisabh.app.ui.screens.components.EditTransactionSheet
import com.hisabh.app.ui.screens.components.ExportDialog
import com.hisabh.app.ui.screens.components.HealthBanner
import com.hisabh.app.ui.screens.components.MonthSelector
import com.hisabh.app.ui.screens.components.SummaryCards
import com.hisabh.app.ui.screens.components.TransactionListItem
import com.hisabh.app.ui.screens.permissions.OnboardingPermissionDialog
import com.hisabh.app.ui.theme.CatFoodColor
import com.hisabh.app.ui.theme.CatGroceriesColor
import com.hisabh.app.ui.theme.CatShoppingColor
import com.hisabh.app.ui.theme.CatTravelColor
import com.hisabh.app.ui.theme.CreamBackground
import com.hisabh.app.ui.theme.CreamBorder
import com.hisabh.app.ui.theme.CreamCard
import com.hisabh.app.ui.theme.ForestGreenLight
import com.hisabh.app.ui.theme.ForestGreenPrimary
import com.hisabh.app.ui.theme.HisabhTheme
import com.hisabh.app.ui.theme.TextMuted
import com.hisabh.app.ui.theme.TextSecondary
import com.hisabh.app.ui.viewmodel.CategorySpend
import com.hisabh.app.ui.viewmodel.HisabhUiState
import com.hisabh.app.ui.viewmodel.HisabhViewModel

@Composable
fun DashboardScreen(
    viewModel: HisabhViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkPermissionsAndBattery()
        viewModel.scanPastSms()
    }

    fun requestAppPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    DashboardContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onScanClick = {
            if (uiState.isSmsPermissionMissing) {
                requestAppPermissions()
            } else {
                viewModel.scanPastSms()
            }
        },
        onExportClick = { viewModel.showExportDialog(true) },
        onPreviousMonth = { viewModel.previousMonth() },
        onNextMonth = { viewModel.nextMonth() },
        onCurrentMonth = { viewModel.jumpToCurrentMonth() },
        onRequestPermissions = { requestAppPermissions() },
        onDismissBanner = { viewModel.dismissHealthBanner() },
        onAddFabClick = { viewModel.showAddSheet(true) },
        onTransactionClick = { tx -> viewModel.openEditTransaction(tx) },
        modifier = modifier
    )

    // Modal BottomSheets & Dialogs
    uiState.editingTransaction?.let { tx ->
        EditTransactionSheet(
            transaction = tx,
            onDismiss = { viewModel.closeEditTransaction() },
            onSave = { updated, applyRule ->
                viewModel.saveEditedTransaction(updated, applyRule)
            },
            onDelete = { id ->
                viewModel.deleteTransaction(id)
            }
        )
    }

    if (uiState.showAddSheet) {
        AddTransactionSheet(
            onDismiss = { viewModel.showAddSheet(false) },
            onSave = { amount, type, merchant, category, note, timestamp ->
                viewModel.addManualTransaction(amount, type, merchant, category, note, timestamp)
            }
        )
    }

    if (uiState.showExportDialog) {
        ExportDialog(
            monthTitle = uiState.monthTitle,
            transactionCount = uiState.allTransactions.size,
            isExporting = uiState.isExporting,
            onExportCsv = { viewModel.exportCsv(context) },
            onExportPdf = { viewModel.exportPdf(context) },
            onDismiss = { viewModel.showExportDialog(false) }
        )
    }

    if (uiState.showOnboardingDialog) {
        OnboardingPermissionDialog(
            onGrantPermissions = {
                viewModel.showOnboardingDialog(false)
                requestAppPermissions()
            },
            onDismiss = { viewModel.showOnboardingDialog(false) }
        )
    }
}

@Composable
fun DashboardContent(
    uiState: HisabhUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onScanClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onCurrentMonth: () -> Unit = {},
    onRequestPermissions: () -> Unit = {},
    onDismissBanner: () -> Unit = {},
    onAddFabClick: () -> Unit = {},
    onTransactionClick: (TransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CreamBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddFabClick,
                containerColor = ForestGreenPrimary,
                contentColor = CreamBackground,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 12.dp, end = 4.dp)
                    .border(1.dp, ForestGreenLight, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Cash Transaction",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Minimal Top App Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ForestGreenPrimary)
                                .border(1.dp, ForestGreenLight, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₹",
                                color = CreamBackground,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PERSONAL EXPENSE",
                                color = TextSecondary,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Hisabh",
                                color = ForestGreenPrimary,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Scan Past SMS Button
                        IconButton(
                            onClick = onScanClick,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CreamCard)
                                .border(1.dp, CreamBorder, CircleShape)
                        ) {
                            if (uiState.isScanningSms) {
                                CircularProgressIndicator(
                                    color = ForestGreenPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Scan Past SMS",
                                    tint = ForestGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Export Button
                        IconButton(
                            onClick = onExportClick,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ForestGreenPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.IosShare,
                                contentDescription = "Export Statement",
                                tint = CreamBackground,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            // 2. Health & Battery Warning Banner (if needed)
            if (!uiState.bannerDismissed && (uiState.isSmsPermissionMissing || uiState.isBatteryOptimizationActive)) {
                item {
                    HealthBanner(
                        isPermissionMissing = uiState.isSmsPermissionMissing,
                        isBatteryOptimizationActive = uiState.isBatteryOptimizationActive,
                        onRequestPermissions = onRequestPermissions,
                        onDismiss = onDismissBanner
                    )
                }
            }

            // 3. Month Selector Bar (< November 2026 >)
            item {
                MonthSelector(
                    monthTitle = uiState.monthTitle,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onCurrentMonth = onCurrentMonth
                )
            }

            // 4. Hero Summary Cards (Net Savings, Spent, Received)
            item {
                SummaryCards(
                    totalSpent = uiState.totalSpent,
                    totalReceived = uiState.totalReceived,
                    netBalance = uiState.netBalance,
                    debitCount = uiState.debitCount,
                    creditCount = uiState.creditCount
                )
            }

            // 5. Category Breakdown Segmented Bar
            item {
                CategoryBreakdownBar(
                    categorySpends = uiState.categoryBreakdown,
                    totalSpent = uiState.totalSpent
                )
            }

            // 6. Chronological Transaction Stream
            if (uiState.allTransactions.isEmpty()) {
                item {
                    EmptyTransactionState(
                        onScanClick = onScanClick,
                        onAddManual = onAddFabClick
                    )
                }
            } else {
                uiState.groupedTransactions.forEach { (dateHeader, list) ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader.uppercase(),
                                color = TextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${list.size} ${if (list.size == 1) "entry" else "entries"}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    items(list, key = { it.id }) { tx ->
                        TransactionListItem(
                            transaction = tx,
                            onClick = { onTransactionClick(tx) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun EmptyTransactionState(
    onScanClick: () -> Unit,
    onAddManual: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CreamCard)
            .border(1.dp, CreamBorder, RoundedCornerShape(20.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, CreamBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No Transactions Recorded",
                color = ForestGreenPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Import past HDFC SMS from your inbox or log a manual cash payment.",
                color = TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onScanClick,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreenPrimary)
                ) {
                    Text("Scan Past SMS", fontSize = 11.sp, color = ForestGreenPrimary, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onAddManual,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreenPrimary,
                        contentColor = CreamBackground
                    )
                ) {
                    Text("+ Add Cash", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview() {
    HisabhTheme {
        val sampleList = listOf(
            TransactionEntity(
                id = 1L,
                utrNumber = "631894021123",
                amount = 340.00,
                txType = TransactionType.DEBIT.name,
                merchantRaw = "zomato@okaxis",
                displayName = "Zomato",
                category = "Food & Dining",
                note = "Dinner with team",
                timestamp = System.currentTimeMillis()
            ),
            TransactionEntity(
                id = 2L,
                utrNumber = "631894021124",
                amount = 150.00,
                txType = TransactionType.DEBIT.name,
                merchantRaw = "CHAI TAPRI",
                displayName = "CHAI TAPRI",
                category = "Food & Dining",
                note = null,
                timestamp = System.currentTimeMillis() - 3600000
            ),
            TransactionEntity(
                id = 3L,
                utrNumber = "631894029990",
                amount = 65000.00,
                txType = TransactionType.CREDIT.name,
                merchantRaw = "Freelance Client",
                displayName = "Client Payment / Invoice",
                category = "Pocket Money / Received",
                note = "Milestone payment",
                timestamp = System.currentTimeMillis() - 86400000
            )
        )

        val mockState = HisabhUiState(
            monthTitle = "November 2026",
            totalSpent = 24850.00,
            totalReceived = 65000.00,
            netBalance = 40150.00,
            debitCount = 38,
            creditCount = 2,
            categoryBreakdown = listOf(
                CategorySpend("Food & Dining", 10437.0, 42f, CatFoodColor),
                CategorySpend("Groceries", 6461.0, 26f, CatGroceriesColor),
                CategorySpend("Travel & Fuel", 4473.0, 18f, CatTravelColor),
                CategorySpend("Shopping", 3479.0, 14f, CatShoppingColor)
            ),
            groupedTransactions = mapOf(
                "TODAY, 13 NOV" to sampleList.take(2),
                "YESTERDAY, 12 NOV" to sampleList.drop(2)
            ),
            allTransactions = sampleList
        )

        DashboardContent(uiState = mockState)
    }
}
