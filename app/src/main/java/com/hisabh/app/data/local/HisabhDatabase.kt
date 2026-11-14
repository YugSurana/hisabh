package com.hisabh.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hisabh.app.data.local.dao.MerchantRuleDao
import com.hisabh.app.data.local.dao.TransactionDao
import com.hisabh.app.data.local.entity.MerchantRuleEntity
import com.hisabh.app.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        MerchantRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HisabhDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun merchantRuleDao(): MerchantRuleDao

    companion object {
        @Volatile
        private var INSTANCE: HisabhDatabase? = null

        fun getInstance(context: Context): HisabhDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HisabhDatabase::class.java,
                    "hisabh_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

