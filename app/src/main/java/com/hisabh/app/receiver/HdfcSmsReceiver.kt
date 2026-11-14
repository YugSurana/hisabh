package com.hisabh.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.hisabh.app.data.local.HisabhDatabase
import com.hisabh.app.data.model.TransactionType
import com.hisabh.app.data.repository.TransactionRepository
import com.hisabh.app.domain.helper.NotificationHelper
import com.hisabh.app.domain.parser.HdfcSmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HdfcSmsReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                val db = HisabhDatabase.getInstance(context)
                val repository = TransactionRepository(db.transactionDao(), db.merchantRuleDao())

                for (sms in messages) {
                    val sender = sms.originatingAddress
                    val body = sms.messageBody
                    val timestamp = sms.timestampMillis

                    if (!HdfcSmsParser.isHdfcSender(sender)) {
                        continue
                    }

                    if (HdfcSmsParser.isNoiseMessage(body)) {
                        continue
                    }

                    val parsed = HdfcSmsParser.parse(body, timestamp) ?: continue

                    // Insert into Room
                    val insertedId = repository.insertParsedTransaction(parsed)

                    // If successfully inserted (not duplicate) and is DEBIT, show interactive notification
                    if (insertedId > 0 && parsed.txType == TransactionType.DEBIT) {
                        val (_, displayName) = repository.resolveCategoryAndName(
                            parsed.merchantRaw,
                            parsed.txType
                        )
                        NotificationHelper.showQuickTagNotification(
                            context = context,
                            transactionId = insertedId,
                            amount = parsed.amount,
                            merchantName = displayName
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

