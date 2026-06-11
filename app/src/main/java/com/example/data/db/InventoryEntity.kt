package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey val itemName: String,
    val stockQuantity: Double,
    val unit: String,
    val lastUpdated: Long,
    val isSynced: Boolean = false
)
