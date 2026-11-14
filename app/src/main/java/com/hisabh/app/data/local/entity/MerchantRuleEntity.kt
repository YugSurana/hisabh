package com.hisabh.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey
    val merchantRaw: String,
    val assignedCategory: String,
    val customDisplayName: String
)

