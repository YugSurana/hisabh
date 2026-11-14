package com.hisabh.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["utrNumber"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["merchantRaw"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val utrNumber: String,
    val amount: Double,
    val txType: String, // "DEBIT" or "CREDIT"
    val merchantRaw: String,
    val displayName: String,
    val category: String,
    val note: String? = null,
    val timestamp: Long,
    val isManual: Boolean = false
)

