package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mandi_prices")
data class MandiPriceEntity(
    @PrimaryKey val cropName: String,
    val marketName: String,
    val price: Double,
    val unit: String = "kg",
    val priceChangeTrend: String, // "UP", "DOWN", "STABLE"
    val state: String,
    val lastUpdated: Long
)
