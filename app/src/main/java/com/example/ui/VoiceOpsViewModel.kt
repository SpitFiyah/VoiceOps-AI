package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ParsedIntentResponse
import com.example.data.db.AppDatabase
import com.example.data.db.InventoryEntity
import com.example.data.db.MandiPriceEntity
import com.example.data.db.TransactionEntity
import com.example.data.repository.VoiceOpsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VoiceOpsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VoiceOpsRepository
    
    // Multi-Language State
    private val _currentLanguage = MutableStateFlow(detectInitialLanguage())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _wasChangedManually = MutableStateFlow(false)
    val wasChangedManually: StateFlow<Boolean> = _wasChangedManually.asStateFlow()

    // Location & Regional states
    private val _detectedCountryCode = MutableStateFlow(detectInitialCountry())
    val detectedCountryCode: StateFlow<String> = _detectedCountryCode.asStateFlow()

    private val _detectedCountryName = MutableStateFlow(detectInitialCountryName())
    val detectedCountryName: StateFlow<String> = _detectedCountryName.asStateFlow()

    private val _detectedCityName = MutableStateFlow("")
    val detectedCityName: StateFlow<String> = _detectedCityName.asStateFlow()

    private val _currencySymbol = MutableStateFlow(detectInitialCurrency())
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _isLocationDetected = MutableStateFlow(false)
    val isLocationDetected: StateFlow<Boolean> = _isLocationDetected.asStateFlow()

    fun selectLanguage(lang: String) {
        _currentLanguage.value = lang
        _wasChangedManually.value = true
    }

    fun updateLocation(countryCode: String, countryName: String, cityName: String?) {
        _detectedCountryCode.value = countryCode
        _detectedCountryName.value = countryName
        _detectedCityName.value = cityName ?: ""
        _isLocationDetected.value = true

        val symbol = when (countryCode.uppercase()) {
            "US" -> "$"
            "GB" -> "£"
            "DE", "FR", "IT", "ES", "NL" -> "€"
            "IN" -> "₹"
            else -> {
                try {
                    java.util.Currency.getInstance(java.util.Locale("", countryCode)).symbol ?: "$"
                } catch (e: Exception) {
                    "$"
                }
            }
        }
        _currencySymbol.value = symbol

        // Update regional mandi and crops
        viewModelScope.launch {
            repository.updateRegionalMandiPrices(countryCode, symbol)
        }
    }

    private fun detectInitialCountry(): String {
        return java.util.Locale.getDefault().country ?: "US"
    }

    private fun detectInitialCountryName(): String {
        return java.util.Locale.getDefault().displayCountry ?: "United States"
    }

    private fun detectInitialCurrency(): String {
        val cc = detectInitialCountry()
        return when (cc.uppercase()) {
            "US" -> "$"
            "DE", "FR", "IT", "ES", "NL" -> "€"
            "GB" -> "£"
            "IN" -> "₹"
            else -> {
                try {
                    java.util.Currency.getInstance(java.util.Locale("", cc)).symbol ?: "$"
                } catch (e: Exception) {
                    "$"
                }
            }
        }
    }

    private fun detectInitialLanguage(): String {
        val sysCountry = java.util.Locale.getDefault().country
        val sysLang = java.util.Locale.getDefault().language
        return when {
            sysLang == "hi" -> "Hindi"
            sysLang == "ta" -> "Tamil"
            sysLang == "te" -> "Telugu"
            sysLang == "bn" -> "Bengali"
            sysLang == "kn" -> "Kannada"
            sysLang == "mr" -> "Marathi"
            sysCountry == "IN" -> "Hinglish" // Localized default Hinglish for Indian location
            else -> "English"
        }
    }
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = VoiceOpsRepository(database.voiceOpsDao())
        
        // Seed Mandi prices and inventory automatically on startup
        viewModelScope.launch {
            repository.seedDefaultMandiPricesIfEmpty()
            repository.seedDefaultInventoryIfEmpty()
        }
    }

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val inventory: StateFlow<List<InventoryEntity>> = repository.allInventory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val mandiPrices: StateFlow<List<MandiPriceEntity>> = repository.allMandiPrices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun syncLocalOfflineData() {
        viewModelScope.launch {
            _isSyncing.value = true
            kotlinx.coroutines.delay(1200) // Beautiful uplink simulation
            repository.syncLocalData()
            _isSyncing.value = false
        }
    }

    private val _latestResponse = MutableStateFlow<ParsedIntentResponse?>(null)
    val latestResponse: StateFlow<ParsedIntentResponse?> = _latestResponse.asStateFlow()

    fun processVoiceCommand(text: String) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val parsed = repository.processVoiceText(text)
                _latestResponse.value = parsed
            } catch (e: Exception) {
                _latestResponse.value = ParsedIntentResponse(
                    type = "chat",
                    explanation = "Failed to translate voice payload successfully: ${e.localizedMessage}"
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearTransactions()
        }
    }

    fun dismissLatestResponse() {
        _latestResponse.value = null
    }

    fun seedSampleTransactions() {
        viewModelScope.launch {
            val samples = listOf(
                TransactionEntity(
                    timestamp = System.currentTimeMillis() - 86400000 * 3, // 3 days ago
                    rawVoiceText = "Aaj 40 kg aloo beche, 30 rupaye kilo",
                    item = "Potato",
                    category = "Sale",
                    quantity = 40.0,
                    unit = "kg",
                    pricePerUnit = 30.0,
                    totalAmount = 1200.0
                ),
                TransactionEntity(
                    timestamp = System.currentTimeMillis() - 86400000 * 2, // 2 days ago
                    rawVoiceText = "Ramesh ko ₹500 udhar diye",
                    item = "Finances",
                    category = "Udhaar",
                    quantity = 1.0,
                    unit = "items",
                    pricePerUnit = 500.0,
                    totalAmount = 500.0,
                    partyName = "Ramesh"
                ),
                TransactionEntity(
                    timestamp = System.currentTimeMillis() - 86400000 * 1, // 1 day ago
                    rawVoiceText = "Mandi jane ka diesel ka kharach ₹450",
                    item = "Diesel",
                    category = "Expense",
                    quantity = 1.0,
                    unit = "items",
                    pricePerUnit = 450.0,
                    totalAmount = 450.0
                ),
                TransactionEntity(
                    timestamp = System.currentTimeMillis() - 3600000 * 4, // 4 hours ago
                    rawVoiceText = "Sold 15 crates tomatoes at ₹250 per crate",
                    item = "Tomato",
                    category = "Sale",
                    quantity = 15.0,
                    unit = "crates",
                    pricePerUnit = 250.0,
                    totalAmount = 3750.0
                ),
                TransactionEntity(
                    timestamp = System.currentTimeMillis() - 3600000 * 1, // 1 hour ago
                    rawVoiceText = "Kisan mandli membership payment ₹150",
                    item = "Fees",
                    category = "Expense",
                    quantity = 1.0,
                    unit = "items",
                    pricePerUnit = 150.0,
                    totalAmount = 150.0
                )
            )
            for (sample in samples) {
                repository.insertTransaction(sample)
            }
        }
    }
}
