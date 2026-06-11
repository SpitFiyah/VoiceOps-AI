package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceOpsDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    // Mandi queries
    @Query("SELECT * FROM mandi_prices ORDER BY cropName ASC")
    fun getAllMandiPrices(): Flow<List<MandiPriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMandiPrice(mandiPrice: MandiPriceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMandiPrices(mandiPrices: List<MandiPriceEntity>)

    @Query("DELETE FROM mandi_prices")
    suspend fun clearAllMandiPrices()

    // Inventory queries
    @Query("SELECT * FROM inventory ORDER BY itemName ASC")
    fun getAllInventory(): Flow<List<InventoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(items: List<InventoryEntity>)

    @Query("DELETE FROM inventory")
    suspend fun clearAllInventory()

    @Query("SELECT * FROM inventory WHERE itemName = :name LIMIT 1")
    suspend fun getInventoryItemByName(name: String): InventoryEntity?

    @Query("UPDATE transactions SET isSynced = 1 WHERE isSynced = 0")
    suspend fun syncTransactionsLocally()

    @Query("UPDATE inventory SET isSynced = 1 WHERE isSynced = 0")
    suspend fun syncInventoryLocally()
}
