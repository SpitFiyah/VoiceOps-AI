package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val rawVoiceText: String,
    val item: String,
    val category: String, // "Sale", "Expense", "Udhaar", "Payment Received"
    val quantity: Double,
    val unit: String,
    val pricePerUnit: Double,
    val totalAmount: Double,
    val partyName: String? = null,
    val isSynced: Boolean = false
)
