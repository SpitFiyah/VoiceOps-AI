package com.example.data.repository

import com.example.data.api.GeminiClient
import com.example.data.api.ParsedIntentResponse
import com.example.data.db.InventoryEntity
import com.example.data.db.MandiPriceEntity
import com.example.data.db.TransactionEntity
import com.example.data.db.VoiceOpsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class VoiceOpsRepository(private val dao: VoiceOpsDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allMandiPrices: Flow<List<MandiPriceEntity>> = dao.getAllMandiPrices()
    val allInventory: Flow<List<InventoryEntity>> = dao.getAllInventory()

    suspend fun seedDefaultInventoryIfEmpty() {
        val currentInventory = dao.getAllInventory().firstOrNull()
        if (currentInventory.isNullOrEmpty()) {
            val defaults = listOf(
                InventoryEntity("Potato", 150.0, "kg", System.currentTimeMillis()),
                InventoryEntity("Onion", 120.0, "kg", System.currentTimeMillis()),
                InventoryEntity("Tomato", 80.0, "crates", System.currentTimeMillis()),
                InventoryEntity("Wheat", 210.0, "kg", System.currentTimeMillis()),
                InventoryEntity("Green Chilli", 50.0, "kg", System.currentTimeMillis())
            )
            dao.insertInventoryItems(defaults)
        }
    }

    suspend fun insertTransaction(transaction: TransactionEntity) {
        dao.insertTransaction(transaction)
        applyInventoryMovement(transaction, transaction.isSynced)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        dao.deleteTransaction(transaction)
    }

    suspend fun clearTransactions() {
        dao.clearAllTransactions()
    }

    suspend fun updateRegionalMandiPrices(countryCode: String, currencySymbol: String) {
        dao.clearAllMandiPrices()
        val prices = when (countryCode.uppercase()) {
            "US" -> listOf(
                MandiPriceEntity("Idaho Potatoes", "Boise Ag Exchange", 1.25, "lb", "STABLE", "Idaho", System.currentTimeMillis()),
                MandiPriceEntity("Yellow Onions", "Walla Walla Center", 1.80, "lb", "UP", "Washington", System.currentTimeMillis()),
                MandiPriceEntity("Beefsteak Tomatoes", "Fresno Farmer Feed", 2.40, "lb", "DOWN", "California", System.currentTimeMillis()),
                MandiPriceEntity("Hard Red Wheat", "Chicago Board of Trade", 0.38, "lb", "STABLE", "Illinois", System.currentTimeMillis()),
                MandiPriceEntity("Sweet Corn", "Des Moines Market", 0.75, "unit", "UP", "Iowa", System.currentTimeMillis()),
                MandiPriceEntity("Fresh Spinach", "Salinas Valley", 3.20, "lb", "DOWN", "California", System.currentTimeMillis())
            )
            "DE", "FR", "IT", "ES", "NL" -> listOf(
                MandiPriceEntity("Rotterdam Potatoes", "Rotterdam Ag Central", 1.15, "kg", "STABLE", "Netherlands", System.currentTimeMillis()),
                MandiPriceEntity("French Onions", "Rungis Paris", 1.35, "kg", "UP", "France", System.currentTimeMillis()),
                MandiPriceEntity("Almeria Tomatoes", "Almeria Agro", 1.85, "kg", "DOWN", "Spain", System.currentTimeMillis()),
                MandiPriceEntity("Mill Wheat", "Euronext Paris", 0.25, "kg", "STABLE", "France", System.currentTimeMillis()),
                MandiPriceEntity("Sweet Beets", "Munich Farmer Feed", 0.90, "kg", "UP", "Germany", System.currentTimeMillis()),
                MandiPriceEntity("Zucchini", "Bari Market", 1.50, "kg", "DOWN", "Italy", System.currentTimeMillis())
            )
            "GB" -> listOf(
                MandiPriceEntity("UK Potatoes", "Lincolnshire Exchange", 0.95, "kg", "STABLE", "Lincolnshire", System.currentTimeMillis()),
                MandiPriceEntity("English Onions", "Evesham Market", 1.10, "kg", "UP", "Worcestershire", System.currentTimeMillis()),
                MandiPriceEntity("Cherry Tomatoes", "Kent Farms", 2.10, "kg", "DOWN", "Kent", System.currentTimeMillis()),
                MandiPriceEntity("UK Feed Wheat", "London Grain Exchange", 0.22, "kg", "STABLE", "UK", System.currentTimeMillis()),
                MandiPriceEntity("Carrots", "Yorkshire Agro", 0.70, "kg", "UP", "Yorkshire", System.currentTimeMillis())
            )
            else -> listOf(
                MandiPriceEntity("Potato (Aloo)", "Delhi Azadpur", 25.00, "kg", "STABLE", "Delhi", System.currentTimeMillis()),
                MandiPriceEntity("Onion (Pyaj)", "Mumbai Vashi", 39.00, "kg", "UP", "Maharashtra", System.currentTimeMillis()),
                MandiPriceEntity("Tomato (Tamatar)", "Bangalore", 42.00, "kg", "DOWN", "Karnataka", System.currentTimeMillis()),
                MandiPriceEntity("Wheat (Gehun)", "Indore Mandi", 27.50, "kg", "STABLE", "Madhya Pradesh", System.currentTimeMillis()),
                MandiPriceEntity("Mustard (Sarson)", "Jaipur", 58.00, "kg", "UP", "Rajasthan", System.currentTimeMillis()),
                MandiPriceEntity("Green Chilli", "Ahmedabad", 35.00, "kg", "DOWN", "Gujarat", System.currentTimeMillis())
            )
        }
        dao.insertMandiPrices(prices)
    }

    suspend fun seedDefaultMandiPricesIfEmpty() {
        val currentPrices = dao.getAllMandiPrices().firstOrNull()
        if (currentPrices.isNullOrEmpty()) {
            val country = java.util.Locale.getDefault().country ?: "US"
            val symbol = when (country.uppercase()) {
                "US" -> "$"
                "DE", "FR", "IT", "ES", "NL" -> "€"
                "GB" -> "£"
                "IN" -> "₹"
                else -> {
                    try {
                        java.util.Currency.getInstance(java.util.Locale("", country)).symbol ?: "$"
                    } catch (e: Exception) {
                        "$"
                    }
                }
            }
            updateRegionalMandiPrices(country, symbol)
        }
    }

    suspend fun processVoiceText(text: String): ParsedIntentResponse {
        val result = GeminiClient.parseVoiceIntent(text)
        
        if (result.type == "transaction") {
            val isSynced = !result.isOfflineFallback
            val entity = TransactionEntity(
                timestamp = System.currentTimeMillis(),
                rawVoiceText = text,
                item = result.item ?: "General Item",
                category = result.category ?: "Sale",
                quantity = result.quantity,
                unit = result.unit,
                pricePerUnit = result.pricePerUnit,
                totalAmount = if (result.totalAmount > 0) result.totalAmount else (result.quantity * result.pricePerUnit),
                partyName = result.partyName,
                isSynced = isSynced
            )
            dao.insertTransaction(entity)
            applyInventoryMovement(entity, isSynced)
        } else if (result.type == "query" && result.isMandiQuery && result.queryCrop != null) {
            val cleanCropName = result.queryCrop
            val updatedMandi = MandiPriceEntity(
                cropName = cleanCropName,
                marketName = "Direct AI Feed",
                price = if (result.queryAnswer?.contains("30") == true) 30.0 else 28.50,
                unit = "kg",
                priceChangeTrend = "STABLE",
                state = "Live",
                lastUpdated = System.currentTimeMillis()
            )
            dao.insertMandiPrice(updatedMandi)
        }
        
        return result
    }

    private suspend fun applyInventoryMovement(transaction: TransactionEntity, isSynced: Boolean) {
        val category = transaction.category.trim().uppercase()
        val direction = when (category) {
            "SALE" -> -1.0
            "PURCHASE", "STOCK IN", "STOCK_IN" -> 1.0
            else -> return
        }
        if (transaction.quantity <= 0.0) return

        val itemName = transaction.item.trim().replaceFirstChar { it.titlecase() }
        val existing = dao.getInventoryItemByName(itemName)
        val currentQuantity = existing?.stockQuantity ?: 0.0
        val newQuantity = (currentQuantity + (direction * transaction.quantity)).coerceAtLeast(0.0)
        val unit = existing?.unit ?: transaction.unit

        dao.insertInventoryItem(
            InventoryEntity(
                itemName = itemName,
                stockQuantity = newQuantity,
                unit = unit,
                lastUpdated = System.currentTimeMillis(),
                isSynced = isSynced
            )
        )
    }

    suspend fun syncLocalData() {
        dao.syncTransactionsLocally()
        dao.syncInventoryLocally()
    }
}
