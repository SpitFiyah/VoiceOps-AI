package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY.trim().trim('"')
            if (key == "MY_GEMINI_API_KEY" || key.isEmpty()) "" else key
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun parseVoiceIntent(userInput: String): ParsedIntentResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            Log.d(TAG, "No API key configured or default placeholder found. Using smart local fallback NLP engine.")
            return@withContext getMockResponse(userInput)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val systemInstruction = """
            You are VoiceOPS AI, a multi-lingual Operational voice NLP processor for small merchants, street vendors, and farmers.
            Analyze the user's spoken input (usually in English, Hindi, Hinglish, or regional Indian languages) and extract structured ledger logs or business queries.
            
            CRITICAL RULES FOR EXTRACTION:
            1. PREVENT GUESSING/ASSUMING: Do NOT guess, interpolate, or hallucinate unit names, quantities, or prices if they are missing from the spoken input.
            2. QUANTITY: Extract the exact quantity mentioned. If no quantity is mentioned, set "quantity" strictly to 1.0. Do NOT assume or default to quantities like 40 or 10.
            3. UNIT: Extract the unit (e.g. "kg", "liter", "crate", "packet", "bunch"). If no unit is mentioned or implied, set "unit" strictly to "items".
            4. PRICE PER UNIT: Extract the price per unit strictly from rates mentioned (e.g. "30 Rs per kg", "at 30", "30 rupay kilo"). If no unit price is mentioned, set "pricePerUnit" strictly to 0.0.
            5. TOTAL AMOUNT: Extract the total transactional sum if explicitly stated (e.g. "fuel expense was 450 rupees", "lent Ramesh 500 rupees").
               - If only the total amount is stated, set "totalAmount" to that value, and "pricePerUnit" to that same value if quantity is 1.0. 
               - If quantity and "pricePerUnit" are both explicitly stated but "totalAmount" is not, calculate "totalAmount" as (quantity * pricePerUnit).
               - If neither total amount nor unit price is given, set BOTH "pricePerUnit" and "totalAmount" to 0.0. Do NOT hallucinate values!
            6. PARTY NAME: Extract names of customers or vendors strictly if mentioned (e.g. "Ramesh", "Suresh"). If no name is mentioned, set "partyName" to null.
            7. EXPLANATION: Construct a conversational spoken feedback confirming what has been logged in the spoken language.
               - If any critical data like quantity, unit, or price is missing and has defaulted (e.g., 0.0 price, 1.0 quantity), include a gentle mention of the missing details in the explanation (e.g., "Registered Sale: Potato. Note: Quantity and price were not specified and logged as default.").
            
            Return ONLY a valid JSON object matching the schema below. No markdown formatting, no code blocks (do not wrap in ```json or ```), just raw JSON:
            {
               "type": "transaction" or "query" or "chat",
               "item": "capitalized name of product (e.g. Potato, Tomato, Milk) or null",
               "category": "Sale" or "Purchase" or "Expense" or "Udhaar" or "Payment Received" or "Stock In" or null,
               "quantity": double (quantity value),
               "unit": "kg" or "liter" or "crate" or "packet" or "bunch" or "items",
               "pricePerUnit": double (price per unit, 0.0 if not specified),
               "totalAmount": double (total transactional amount or payment amount, 0.0 if not specified),
               "partyName": "name of a customer / vendor if Udhaar or Payment Received, else null",
               "explanation": "spoken feedback in the language spoken",
               "isMandiQuery": boolean (true if vendor is asking about mandi/market prices of crops),
               "queryCrop": "commodity name capitalized if asking about mandi rate, else null",
               "queryAnswer": "helpful spoken language voice response if the type is query/chat (e.g. Today potato mandi rate is 22-28 Rs per kg in Azadpur)"
            }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", userInput)
                        }
                        put(partObj)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val systemObj = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    val partObj = JSONObject().apply {
                        put("text", systemInstruction)
                    }
                    put(partObj)
                }
                put("parts", partsArray)
            }
            put("systemInstruction", systemObj)

            val configObj = JSONObject().apply {
                put("temperature", 0.1)
                put("responseMimeType", "application/json")
            }
            put("generationConfig", configObj)
        }

        try {
            val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "API call failed code=${response.code}, body=${response.body?.string()}")
                return@withContext getMockResponse(userInput).copy(isOfflineFallback = true)
            }

            val body = response.body?.string() ?: ""
            Log.d(TAG, "Gemini Raw Response: $body")

            val responseJson = JSONObject(body)
            val candidates = responseJson.getJSONArray("candidates")
            val text = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val jsonStart = text.indexOf("{")
            val jsonEnd = text.lastIndexOf("}")
            val cleanedText = if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                text.substring(jsonStart, jsonEnd + 1)
            } else {
                text.trim()
            }

            val parsedJson = JSONObject(cleanedText)
            
            ParsedIntentResponse(
                type = parsedJson.optString("type", "chat"),
                item = parsedJson.optString("item").takeIf { it != "null" && it.isNotEmpty() },
                category = parsedJson.optString("category").takeIf { it != "null" && it.isNotEmpty() },
                quantity = parsedJson.optDouble("quantity", 1.0),
                unit = parsedJson.optString("unit", "items"),
                pricePerUnit = parsedJson.optDouble("pricePerUnit", 0.0),
                totalAmount = parsedJson.optDouble("totalAmount", 0.0),
                partyName = parsedJson.optString("partyName").takeIf { it != "null" && it.isNotEmpty() },
                explanation = parsedJson.optString("explanation", "Processed voice command successfully."),
                isMandiQuery = parsedJson.optBoolean("isMandiQuery", false),
                queryCrop = parsedJson.optString("queryCrop").takeIf { it != "null" && it.isNotEmpty() },
                queryAnswer = parsedJson.optString("queryAnswer").takeIf { it != "null" && it.isNotEmpty() },
                isOfflineFallback = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing voice intent with Gemini. Reverting to local NLP.", e)
            getMockResponse(userInput).copy(isOfflineFallback = true)
        }
    }

    private fun parseQuantityFromText(text: String): Double? {
        val regex = Regex("""(\d+(?:\.\d+)?)\s*(?:kg|kilogram|crate|packet|pack|packet|bunch|liter|litre|items|item)""")
        val match = regex.find(text)
        if (match != null) {
            return match.groupValues[1].toDoubleOrNull()
        }
        // Fallback to standalone numbers that aren't preceded by currency indicators
        val numberRegex = Regex("""(?<!rs\s|rs\.\s|₹\s|₹|rupees\s|rupee\s)\b(\d+(?:\.\d+)?)\b""")
        val matches = numberRegex.findAll(text)
        for (m in matches) {
            val num = m.groupValues[1].toDoubleOrNull()
            if (num != null) return num
        }
        return null
    }

    private fun parsePriceFromText(text: String): Double? {
        val regex = Regex("""(?:rs\.?|₹|rupees?|at|@)\s*(\d+(?:\.\d+)?)|\b(\d+(?:\.\d+)?)\s*(?:rs\.?|rupees?)""")
        val match = regex.find(text)
        if (match != null) {
            val value = match.groupValues[1].takeIf { it.isNotEmpty() } ?: match.groupValues[2]
            return value.toDoubleOrNull()
        }
        return null
    }

    private fun parseUnitFromText(text: String): String {
        return when {
            text.contains("kg") || text.contains("kilogram") || text.contains("kilo") -> "kg"
            text.contains("crate") -> "crate"
            text.contains("packet") || text.contains("pack") -> "packet"
            text.contains("liter") || text.contains("litre") -> "liter"
            text.contains("bunch") -> "bunch"
            else -> "items"
        }
    }

    private fun inferStockCategory(text: String): String {
        return when {
            text.contains("bought") ||
                text.contains("buy") ||
                text.contains("purchase") ||
                text.contains("purchased") ||
                text.contains("khareed") ||
                text.contains("kharida") ||
                text.contains("stock in") ||
                text.contains("restock") -> "Purchase"
            else -> "Sale"
        }
    }

    private fun stockExplanation(
        item: String,
        category: String,
        quantity: Double,
        unit: String,
        price: Double,
        total: Double
    ): String {
        val action = if (category == "Purchase") "Recorded Purchase" else "Recorded Sale"
        return if (price > 0.0) {
            "$action: $item $quantity $unit @ Rs $price/$unit. Total Rs $total logged and inventory updated."
        } else {
            "$action: $item $quantity $unit logged and inventory updated. Note: Price/rate was not specified in the voice input."
        }
    }

    private fun getMockResponse(userInput: String): ParsedIntentResponse {
        val normalized = userInput.lowercase()

        fun stockResponse(item: String): ParsedIntentResponse {
            val quantity = parseQuantityFromText(normalized) ?: 1.0
            val price = parsePriceFromText(normalized) ?: 0.0
            val unit = parseUnitFromText(normalized)
            val category = inferStockCategory(normalized)
            val total = quantity * price
            return ParsedIntentResponse(
                type = "transaction",
                category = category,
                item = item,
                quantity = quantity,
                unit = unit,
                pricePerUnit = price,
                totalAmount = total,
                explanation = stockExplanation(item, category, quantity, unit, price, total)
            )
        }

        fun mandiResponse(item: String, answer: String): ParsedIntentResponse {
            return ParsedIntentResponse(
                type = "query",
                isMandiQuery = true,
                queryCrop = item,
                queryAnswer = answer,
                explanation = answer
            )
        }

        return when {
            normalized.contains("potato") || normalized.contains("aloo") -> {
                if (normalized.contains("bhav") || normalized.contains("price") || normalized.contains("rate") || normalized.contains("bhaav")) {
                    mandiResponse("Potato", "Aloo mandi bhav is Rs 22-28 per kg in Azadpur, stable since yesterday.")
                } else {
                    stockResponse("Potato")
                }
            }
            normalized.contains("onion") || normalized.contains("pyaaj") || normalized.contains("pyaj") -> {
                if (normalized.contains("bhav") || normalized.contains("price") || normalized.contains("rate") || normalized.contains("bhaav")) {
                    mandiResponse("Onion", "Pyaj mandi bhav is Rs 35-42 per kg in Vashi, trending upward on low supply.")
                } else {
                    stockResponse("Onion")
                }
            }
            normalized.contains("tomato") || normalized.contains("tamatar") -> {
                if (normalized.contains("bhav") || normalized.contains("price") || normalized.contains("rate") || normalized.contains("bhaav")) {
                    mandiResponse("Tomato", "Tamatar mandi bhav is Rs 38-45 per kg with moderate demand.")
                } else {
                    stockResponse("Tomato")
                }
            }
            normalized.contains("ramesh") || normalized.contains("udhaar") || normalized.contains("udhar") || normalized.contains("lent") || normalized.contains("gave") -> {
                val party = if (normalized.contains("ramesh")) "Ramesh" else if (normalized.contains("suresh")) "Suresh" else "Unknown Party"
                val amount = parsePriceFromText(normalized) ?: 0.0
                ParsedIntentResponse(
                    type = "transaction",
                    category = "Udhaar",
                    item = "Cash Debit",
                    quantity = 1.0,
                    unit = "items",
                    pricePerUnit = amount,
                    totalAmount = amount,
                    partyName = party,
                    explanation = if (amount > 0.0) {
                        "Udhaar Logged: Lent Rs $amount to $party. Added to local digital financial profile."
                    } else {
                        "Udhaar Logged: Lent to $party. Note: Amount was not specified in the voice input."
                    }
                )
            }
            normalized.contains("diesel") || normalized.contains("expense") || normalized.contains("rent") || normalized.contains("travel") || normalized.contains("kiraya") -> {
                val amount = parsePriceFromText(normalized) ?: 0.0
                val expenseItem = when {
                    normalized.contains("diesel") || normalized.contains("fuel") || normalized.contains("petrol") -> "Fuel"
                    normalized.contains("rent") || normalized.contains("kiraya") -> "Rent"
                    else -> "Logistics"
                }
                ParsedIntentResponse(
                    type = "transaction",
                    category = "Expense",
                    item = expenseItem,
                    quantity = 1.0,
                    unit = "items",
                    pricePerUnit = amount,
                    totalAmount = amount,
                    explanation = if (amount > 0.0) {
                        "Operational Expense Logged: $expenseItem Rs $amount."
                    } else {
                        "Operational Expense Logged: $expenseItem. Note: Amount was not specified in the voice input."
                    }
                )
            }
            normalized.contains("loan") || normalized.contains("credit") -> {
                ParsedIntentResponse(
                    type = "chat",
                    queryAnswer = "Your credit-readiness estimate is strong because transaction consistency and operating volume are improving.",
                    explanation = "VoiceOPS credit readiness looks healthy. Keep logging sales, expenses, and stock movement daily."
                )
            }
            else -> {
                ParsedIntentResponse(
                    type = "chat",
                    queryAnswer = "I heard: \"$userInput\". Try: 'Sold 5 kg onion at 30', 'Bought 20 kg onion', or 'What is pyaj bhav today?'.",
                    explanation = "Try logging a sale, purchase, expense, udhaar, or mandi query."
                )
            }
        }
    }
}

data class ParsedIntentResponse(
    val type: String,
    val item: String? = null,
    val category: String? = null,
    val quantity: Double = 1.0,
    val unit: String = "items",
    val pricePerUnit: Double = 0.0,
    val totalAmount: Double = 0.0,
    val partyName: String? = null,
    val explanation: String = "",
    val isMandiQuery: Boolean = false,
    val queryCrop: String? = null,
    val queryAnswer: String? = null,
    val isOfflineFallback: Boolean = false
)
