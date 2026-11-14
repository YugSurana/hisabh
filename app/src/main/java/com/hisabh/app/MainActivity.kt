package com.hisabh.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.hisabh.app.data.local.HisabhDatabase
import com.hisabh.app.data.repository.TransactionRepository
import com.hisabh.app.domain.helper.NotificationHelper
import com.hisabh.app.ui.screens.DashboardScreen
import com.hisabh.app.ui.theme.HisabhTheme
import com.hisabh.app.ui.viewmodel.HisabhViewModel
import com.hisabh.app.ui.viewmodel.HisabhViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: HisabhViewModel by viewModels {
        val db = HisabhDatabase.getInstance(applicationContext)
        val repo = TransactionRepository(db.transactionDao(), db.merchantRuleDao())
        HisabhViewModelFactory(application, repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge support
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Check if opened via interactive quick-tag notification deep-link
        handleNotificationDeepLink(intent)

        // Check first install onboarding: if SMS permission missing, prompt onboarding dialog
        checkFirstRunOnboarding()

        setContent {
            HisabhTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationDeepLink(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissionsAndBattery()
    }

    private fun handleNotificationDeepLink(intent: Intent?) {
        if (intent == null) return
        val transactionId = intent.getLongExtra(NotificationHelper.EXTRA_EDIT_TRANSACTION_ID, -1L)
        if (transactionId > 0L) {
            viewModel.openEditTransactionById(transactionId)
        }
    }

    private fun checkFirstRunOnboarding() {
        val hasReceiveSms = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val hasReadSms = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasReceiveSms || !hasReadSms) {
            viewModel.showOnboardingDialog(true)
        }
    }
}

