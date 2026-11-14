package com.hisabh.app.domain.parser

import com.hisabh.app.data.model.TransactionType
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

object HdfcSmsParser {

    // Regex for DLT sender headers: e.g. AD-HDFCBK-T, VK-HDFCBK-S, BZ-HDFCBN-T
    private val DLT_SENDER_PATTERN = Pattern.compile("^[A-Z]{2}-HDFC[A-Z0-9]{2}-[TS]$", Pattern.CASE_INSENSITIVE)

    private val KNOWN_HDFC_HEADERS = listOf(
        "AD-HDFCBK-T", "AD-HDFCBN-S", "VM-HDFCBK-T", "VK-HDFCBK-T",
        "BZ-HDFCBN-S", "BW-HDFCBK-S", "HDFCBK", "HDFCBN"
    )

    private val NOISE_WORDS = listOf(
        "otp", "failed", "declined", "requested", "insufficient", "mandate", "code"
    )

    // Regex Patterns for Amounts: captures Rs. 150.00, Rs.150, INR 150.00
    private val AMOUNT_PATTERN = Pattern.compile(
        "(?:Rs\\.?|INR)\\s*([0-9]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    // Regex for 12-digit UPI / Bank Reference number
    private val UTR_PATTERN = Pattern.compile(
        "(?:Ref(?:\\s*No\\.?)?|UPI\\s*Ref(?:\\s*No\\.?)?)\\s*[:#]?\\s*([0-9]{12})",
        Pattern.CASE_INSENSITIVE
    )

    // Generic fallback for any standalone 12-digit number following Ref / UTR
    private val GENERIC_12_DIGIT_PATTERN = Pattern.compile("\\b([0-9]{12})\\b")

    // Date Patterns commonly found in HDFC SMS: dd/MM/yy, dd-MM-yy, dd-MM-yyyy
    private val DATE_PATTERNS = listOf(
        SimpleDateFormat("dd/MM/yy", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yy", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH)
    )

    // Specific debit matchers
    // Pattern 1: "Sent Rs.150.00 from HDFC Bank A/C x1234 To CHAI TAPRI On 12/11/26 Ref 631894021123"
    private val SENT_TO_PATTERN = Pattern.compile(
        "Sent\\s+(?:Rs\\.?|INR)\\s*([0-9.]+)\\s+from.*?To\\s+(.*?)\\s+On\\s+([0-9/\\-]+)(?:.*?Ref(?:\\s*No\\.?)?\\s*([0-9]{12}))?",
        Pattern.CASE_INSENSITIVE
    )

    // Pattern 2: "Rs. 240.00 debited from a/c *1234 on 12-11-26 to VPA zomato@okaxis (UPI Ref No 631894021123)"
    private val DEBITED_TO_PATTERN = Pattern.compile(
        "(?:Rs\\.?|INR)\\s*([0-9.]+)\\s+debited.*?on\\s+([0-9/\\-]+).*?(?:to|towards)\\s+(?:VPA\\s+)?(.*?)(?:\\s*\\(|\\s+On|\\s+Ref|$)",
        Pattern.CASE_INSENSITIVE
    )

    // Pattern 3: "Rs. 500.00 credited to a/c *1234 on 12-11-26 by VPA sender@okicici (UPI Ref No 631894021123)"
    private val CREDITED_BY_PATTERN = Pattern.compile(
        "(?:Rs\\.?|INR)\\s*([0-9.]+)\\s+credited.*?on\\s+([0-9/\\-]+).*?(?:by|from)\\s+(?:VPA\\s+)?(.*?)(?:\\s*\\(|\\s+Ref|$)",
        Pattern.CASE_INSENSITIVE
    )

    // Pattern 4: "Received Rs.500.00 in your a/c x1234 on 12/11/26 from John Doe Ref 631894021123"
    private val RECEIVED_FROM_PATTERN = Pattern.compile(
        "Received\\s+(?:Rs\\.?|INR)\\s*([0-9.]+).*?on\\s+([0-9/\\-]+).*?from\\s+(?:VPA\\s+)?(.*?)(?:\\s+Ref|$)",
        Pattern.CASE_INSENSITIVE
    )

    fun isHdfcSender(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val cleanSender = sender.trim()
        if (KNOWN_HDFC_HEADERS.any { cleanSender.equals(it, ignoreCase = true) }) {
            return true
        }
        return DLT_SENDER_PATTERN.matcher(cleanSender).matches()
    }

    fun isNoiseMessage(body: String): Boolean {
        val lower = body.lowercase()
        return NOISE_WORDS.any { lower.contains(it) }
    }

    fun parse(smsBody: String, fallbackTimestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        if (isNoiseMessage(smsBody)) {
            return null
        }

        val cleanBody = smsBody.replace("\n", " ").trim()

        // 1. Determine Transaction Type
        val isDebit = cleanBody.contains("debited", ignoreCase = true) ||
                cleanBody.contains("Sent Rs", ignoreCase = true) ||
                cleanBody.contains("Sent INR", ignoreCase = true) ||
                cleanBody.contains("paid", ignoreCase = true)

        val isCredit = cleanBody.contains("credited", ignoreCase = true) ||
                cleanBody.contains("Received Rs", ignoreCase = true) ||
                cleanBody.contains("Received INR", ignoreCase = true) ||
                cleanBody.contains("deposited", ignoreCase = true)

        if (!isDebit && !isCredit) {
            return null
        }

        val txType = if (isDebit) TransactionType.DEBIT else TransactionType.CREDIT

        // 2. Extract Amount
        var amount: Double? = null
        val amountMatcher = AMOUNT_PATTERN.matcher(cleanBody)
        if (amountMatcher.find()) {
            amount = amountMatcher.group(1)?.toDoubleOrNull()
        }
        if (amount == null || amount <= 0.0) {
            return null
        }

        // 3. Extract Merchant & Timestamp
        var merchantRaw: String? = null
        var parsedTimestamp: Long? = null

        // Try Pattern 1: Sent ... To ... On ... Ref ...
        val sentMatcher = SENT_TO_PATTERN.matcher(cleanBody)
        if (sentMatcher.find()) {
            merchantRaw = sentMatcher.group(2)
            parsedTimestamp = parseDate(sentMatcher.group(3))
        }

        // Try Pattern 2: Debited ... to VPA ...
        if (merchantRaw.isNullOrBlank()) {
            val debitMatcher = DEBITED_TO_PATTERN.matcher(cleanBody)
            if (debitMatcher.find()) {
                parsedTimestamp = parseDate(debitMatcher.group(2))
                merchantRaw = debitMatcher.group(3)
            }
        }

        // Try Pattern 3: Credited ... by VPA ...
        if (merchantRaw.isNullOrBlank()) {
            val creditMatcher = CREDITED_BY_PATTERN.matcher(cleanBody)
            if (creditMatcher.find()) {
                parsedTimestamp = parseDate(creditMatcher.group(2))
                merchantRaw = creditMatcher.group(3)
            }
        }

        // Try Pattern 4: Received ... from ...
        if (merchantRaw.isNullOrBlank()) {
            val recMatcher = RECEIVED_FROM_PATTERN.matcher(cleanBody)
            if (recMatcher.find()) {
                parsedTimestamp = parseDate(recMatcher.group(2))
                merchantRaw = recMatcher.group(3)
            }
        }

        // Fallback for merchant extraction if not matched yet
        if (merchantRaw.isNullOrBlank()) {
            merchantRaw = fallbackExtractMerchant(cleanBody, txType)
        }

        val finalMerchant = cleanMerchant(merchantRaw)
        val finalTimestamp = parsedTimestamp ?: fallbackTimestamp

        // 4. Extract UTR Number (12-digit) or SHA-256 fallback
        val utrNumber = extractUtr(cleanBody) ?: generateFallbackHash(amount, finalTimestamp, smsBody)

        return ParsedTransaction(
            amount = amount,
            txType = txType,
            merchantRaw = finalMerchant,
            utrNumber = utrNumber,
            timestamp = finalTimestamp,
            rawSms = smsBody
        )
    }

    private fun extractUtr(smsBody: String): String? {
        val matcher = UTR_PATTERN.matcher(smsBody)
        if (matcher.find()) {
            return matcher.group(1)
        }
        // General search for 12 digits after "Ref" or "UPI"
        val refIndex = smsBody.indexOf("Ref", ignoreCase = true)
        if (refIndex != -1) {
            val substring = smsBody.substring(refIndex)
            val genericMatcher = GENERIC_12_DIGIT_PATTERN.matcher(substring)
            if (genericMatcher.find()) {
                return genericMatcher.group(1)
            }
        }
        return null
    }

    private fun cleanMerchant(raw: String): String {
        var clean = raw.trim()
        // Strip leading VPA / to / by
        clean = clean.replace(Regex("^(?:VPA\\s+|to\\s+|by\\s+|from\\s+)", RegexOption.IGNORE_CASE), "")
        // Remove trailing punctuation and unwanted tokens
        clean = clean.replace(Regex("[,;.]+$"), "")
            .replace(Regex("\\s*\\(UPI Ref.*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*Ref.*$", RegexOption.IGNORE_CASE), "")
            .trim()
        return clean.ifBlank { "HDFC Transaction" }
    }

    private fun fallbackExtractMerchant(body: String, txType: TransactionType): String {
        val keywords = if (txType == TransactionType.DEBIT) listOf("to VPA", "to", "towards") else listOf("by VPA", "from")
        for (kw in keywords) {
            val idx = body.indexOf(kw, ignoreCase = true)
            if (idx != -1) {
                val sub = body.substring(idx + kw.length).trim()
                val endIdx = listOf(
                    sub.indexOf(" on ", ignoreCase = true),
                    sub.indexOf(" Ref", ignoreCase = true),
                    sub.indexOf(" (", ignoreCase = true),
                    sub.indexOf(" via ", ignoreCase = true)
                ).filter { it > 0 }.minOrNull() ?: sub.length
                val result = sub.substring(0, endIdx).trim()
                if (result.isNotBlank()) return result
            }
        }
        return if (txType == TransactionType.DEBIT) "Merchant" else "HDFC Direct Deposit"
    }

    private fun parseDate(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        val clean = dateStr.trim().replace(".", "-")
        for (format in DATE_PATTERNS) {
            try {
                val parsed = format.parse(clean)
                if (parsed != null) {
                    return parsed.time
                }
            } catch (_: Exception) {
                // Try next pattern
            }
        }
        return null
    }

    fun generateFallbackHash(amount: Double, timestamp: Long, rawSms: String): String {
        val input = "%.2f_%d_%s".format(Locale.US, amount, timestamp, rawSms.trim())
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

