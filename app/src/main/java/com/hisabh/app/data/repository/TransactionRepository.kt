package com.hisabh.app.data.repository

import com.hisabh.app.data.local.dao.MerchantRuleDao
import com.hisabh.app.data.local.dao.TransactionDao
import com.hisabh.app.data.local.entity.MerchantRuleEntity
import com.hisabh.app.data.local.entity.TransactionEntity
import com.hisabh.app.data.model.Category
import com.hisabh.app.data.model.TransactionType
import com.hisabh.app.domain.parser.ParsedTransaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val merchantRuleDao: MerchantRuleDao
) {

    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetween(startTime, endTime)
    }

    suspend fun getTransactionsBetweenSync(startTime: Long, endTime: Long): List<TransactionEntity> {
        return transactionDao.getTransactionsBetweenSync(startTime, endTime)
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun insertParsedTransaction(parsed: ParsedTransaction): Long {
        val (category, displayName) = resolveCategoryAndName(parsed.merchantRaw, parsed.txType)
        val entity = TransactionEntity(
            utrNumber = parsed.utrNumber,
            amount = parsed.amount,
            txType = parsed.txType.name,
            merchantRaw = parsed.merchantRaw,
            displayName = displayName,
            category = category,
            note = null,
            timestamp = parsed.timestamp,
            isManual = false
        )
        return transactionDao.insertTransaction(entity)
    }

    suspend fun insertParsedTransactionsBatch(parsedList: List<ParsedTransaction>): Int {
        if (parsedList.isEmpty()) return 0
        val entities = parsedList.map { parsed ->
            val (category, displayName) = resolveCategoryAndName(parsed.merchantRaw, parsed.txType)
            TransactionEntity(
                utrNumber = parsed.utrNumber,
                amount = parsed.amount,
                txType = parsed.txType.name,
                merchantRaw = parsed.merchantRaw,
                displayName = displayName,
                category = category,
                note = null,
                timestamp = parsed.timestamp,
                isManual = false
            )
        }
        val inserted = transactionDao.insertTransactions(entities)
        return inserted.count { it != -1L }
    }

    suspend fun updateTransaction(
        transaction: TransactionEntity,
        applyToAllFuture: Boolean
    ) {
        transactionDao.updateTransaction(transaction)

        if (applyToAllFuture) {
            merchantRuleDao.upsertRule(
                MerchantRuleEntity(
                    merchantRaw = transaction.merchantRaw,
                    assignedCategory = transaction.category,
                    customDisplayName = transaction.displayName
                )
            )
            // Retroactively update past matching transactions
            transactionDao.updateMatchingMerchant(
                merchantRaw = transaction.merchantRaw,
                displayName = transaction.displayName,
                category = transaction.category
            )
        }
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun resolveCategoryAndName(
        merchantRaw: String,
        txType: TransactionType
    ): Pair<String, String> {
        // 1. Check existing saved user rule
        val rule = merchantRuleDao.getRule(merchantRaw)
        if (rule != null) {
            return Pair(rule.assignedCategory, rule.customDisplayName)
        }

        // 2. Keyword heuristic inference
        val inferredCategory = Category.inferCategory(merchantRaw, txType).title

        // 3. Clean display name from merchantRaw
        val displayName = cleanMerchantDisplayName(merchantRaw)

        return Pair(inferredCategory, displayName)
    }

    private fun cleanMerchantDisplayName(raw: String): String {
        var clean = raw.trim()
        if (clean.contains("@")) {
            // UPI handle: extract prefix before @ and clean
            val prefix = clean.substringBefore("@")
            // Remove common upi prefixes like bharatpe.9812 or paytm-123
            clean = prefix.replace(Regex("[-_.0-9]+$"), "")
                .replace(Regex("^(paytm|bharatpe|phonepe|gpay)[-_.]?", RegexOption.IGNORE_CASE), "")
                .ifBlank { prefix }
        }
        // Capitalize words nicely
        return clean.split(" ", "_", "-")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }.ifBlank { raw }
    }
}

