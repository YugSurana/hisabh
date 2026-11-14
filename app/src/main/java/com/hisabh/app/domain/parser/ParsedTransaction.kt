package com.hisabh.app.domain.parser

import com.hisabh.app.data.model.TransactionType

data class ParsedTransaction(
    val amount: Double,
    val txType: TransactionType,
    val merchantRaw: String,
    val utrNumber: String,
    val timestamp: Long,
    val rawSms: String
)

