package com.hisabh.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hisabh.app.data.local.entity.MerchantRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: MerchantRuleEntity)

    @Query("SELECT * FROM merchant_rules WHERE merchantRaw = :merchantRaw LIMIT 1")
    suspend fun getRule(merchantRaw: String): MerchantRuleEntity?

    @Query("SELECT * FROM merchant_rules")
    fun getAllRules(): Flow<List<MerchantRuleEntity>>

    @Query("DELETE FROM merchant_rules WHERE merchantRaw = :merchantRaw")
    suspend fun deleteRule(merchantRaw: String)
}

