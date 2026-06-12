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

    suspend fun parseVoiceIntent(userInput: String, language: String): ParsedIntentResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            Log.d(TAG, "No API key configured or default placeholder found. Using smart local fallback NLP engine.")
            return@withContext getMockResponse(userInput, language)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val systemInstruction = """
            You are VoiceOPS AI, a multi-lingual Operational voice NLP processor for small merchants, street vendors, and farmers.
            Analyze the user's spoken input (usually in English, Hindi, Hinglish, or regional Indian languages) and extract structured ledger logs or business queries.
            
            CRITICAL LANGUAGE MANDATE:
            The user has selected "$language" as their target interface language. 
            You MUST strictly construct the "explanation" conversational spoken feedback in the "$language" script/phrasing (e.g. Devanagari script for Hindi, Kannada script for Kannada, Tamil script for Tamil, Telugu script for Telugu, Bengali script for Bengali, Marathi script for Marathi, Hinglish in Latin letters for Hinglish).
            Do NOT explain, describe, or answer in English words or English characters of any sort when regional Indian languages are selected (unless English or Hinglish is chosen). This is a direct hard restriction to keep synthesized audio outputs in synchronized, natural-sounding local speech.
            Example conversions:
            - If selected target language is "Hindi": construct the "explanation" inside the JSON strictly in Devanagari script (e.g., "रमेश ने दो किलो आलू खरीद लिए हैं।").
            - If selected target language is "Tamil": construct the "explanation" strictly in Tamil script (e.g., "ரமேஷ் இரண்டு கிலோ உருளைக்கிழங்கு வாங்கியுள்ளார்.").
            - If selected target language is "Telugu": construct the "explanation" strictly in Telugu script (e.g., "రమేష్ రెండు కిలోల ఆలుగడ్డలు కొన్నారు.").
            - If selected target language is "Kannada": construct the "explanation" strictly in Kannada script (e.g., "ರಮೇಶ್ ಎರಡು ಕೆಜಿ ಆಲೂಗಡ್ಡೆ ಖರೀದಿಸಿದ್ದಾರೆ.").
            - If selected target language is "Marathi": construct the "explanation" strictly in Marathi script (e.g., "रमेशने दोन किलो बटाटे खरेदी केले आहेत.").
            - If selected target language is "Bengali": construct the "explanation" strictly in Bengali script (e.g., "রমেশ দুই কেজি আলু কিনেছেন।").
            - If selected target language is "Hinglish": construct the "explanation" strictly in conversational Hinglish with Latin letters (e.g., "Ramesh ne do kilo aloo khareed liya hai.").
            - If selected target language is "English": construct the "explanation" in English.
            
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
            7. EXPLANATION: Construct the conversational spoken feedback as requested above, confirming what has been logged in the required "$language" script and phrasing.
               - If any critical data like quantity, unit, or price is missing and has defaulted (e.g., 0.0 price, 1.0 quantity), include a gentle mention of the missing details in the explanation in the target language.
            
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
               "explanation": "spoken feedback in the requested target language script",
               "isMandiQuery": boolean (true if vendor is asking about mandi/market prices of crops),
               "queryCrop": "commodity name capitalized if asking about mandi rate, else null",
               "queryAnswer": "helpful spoken target language voice response if the type is query/chat (spoken in "$language" script)"
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
                return@withContext getMockResponse(userInput, language).copy(isOfflineFallback = true)
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
            getMockResponse(userInput, language).copy(isOfflineFallback = true)
        }
    }

    private fun parseQuantityFromText(text: String): Double? {
        val regex = Regex("""(\d+(?:\.\d+)?)\s*(?:kg|kilogram|crate|packet|pack|packet|bunch|liter|litre|items|item)""")
        val match = regex.find(text)
        if (match != null) {
            return match.groupValues[1].toDoubleOrNull()
        }
        // Fallback to standalone numbers that aren't preceded by currency indicators
        val numberRegex = Regex("""(?<!rs\s)(?<!rs\.\s)(?<!₹\s)(?<!₹)(?<!rupees\s)(?<!rupee\s)\b(\d+(?:\.\d+)?)\b""")
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
        total: Double,
        language: String
    ): String {
        return when (language) {
            "Hindi" -> {
                val action = if (category == "Purchase") "खरीद दर्ज की गई" else "बिक्री दर्ज की गई"
                if (price > 0.0) {
                    "$action: $item $quantity $unit @ ₹$price प्रति $unit। कुल ₹$total दर्ज।"
                } else {
                    "$action: $item $quantity $unit दर्ज।"
                }
            }
            "Hinglish" -> {
                val action = if (category == "Purchase") "Purchase likh liya hai" else "Bikri likh li hai"
                if (price > 0.0) {
                    "$action: $item $quantity $unit @ ₹$price per $unit. Total ₹$total bahi me chadh gaya hai."
                } else {
                    "$action: $item $quantity $unit bahi me likh liya hai."
                }
            }
            "Tamil" -> {
                val action = if (category == "Purchase") "கொள்முதல் பதிவு செய்யப்பட்டது" else "விற்பனை பதிவு செய்யப்பட்டது"
                if (price > 0.0) {
                    "$action: $item $quantity $unit @ ₹$price/$unit. மொத்த தொகை ₹$total."
                } else {
                    "$action: $item $quantity $unit பதிவு செய்யப்பட்டது."
                }
            }
            "Telugu" -> {
                val action = if (category == "Purchase") "కొనుగోలు నమోదు చేయబడింది" else "అమ్మకం నమోదు చేయబడింది"
                if (price > 0.0) {
                    "$action: $item $quantity $unit @ ₹$price/$unit. మొత్తం ₹$total."
                } else {
                    "$action: $item $quantity $unit నమోదు చేయబడింది."
                }
            }
            "Kannada" -> {
                val action = if (category == "Purchase") "ಖರೀದಿ ದಾಖಲಿಸಲಾಗಿದೆ" else "ಮಾರಾಟ ದಾಖಲಿಸಲಾಗಿದೆ"
                if (price > 0.0) {
                    "$action: $item $quantity $unit @ ₹$price/$unit. ಒಟ್ಟು ₹$total."
                } else {
                    "$action: $item $quantity $unit ದಾಖಲಿಸಲಾಗಿದೆ."
                }
            }
            "Bengali" -> {
                val action = if (category == "Purchase") "ক্রয় নথিভুক্ত করা হয়েছে" else "বিক্রয় নথিভুক্ত করা হয়েছে"
                if (price > 0.0) {
                    "$action: $item $quantity $unit @ ₹$price/$unit. মোট ₹$total."
                } else {
                    "$action: $item $quantity $unit নথিভুক্ত করা হয়েছে।"
                }
            }
            "Marathi" -> {
                val action = if (category == "Purchase") "खरेदी नोंदवली गेली" else "विक्री नोंदवली गेली"
                if (price > 0.0) {
                    "$action: $item $quantity $unit @ ₹$price/$unit. एकूण ₹$total."
                } else {
                    "$action: $item $quantity $unit नोंदवली गेली."
                }
            }
            else -> { // English
                val action = if (category == "Purchase") "Recorded Purchase" else "Recorded Sale"
                if (price > 0.0) {
                    "$action: $item $quantity $unit @ Rs $price/$unit. Total Rs $total logged and inventory updated."
                } else {
                    "$action: $item $quantity $unit logged and inventory updated. Note: Price/rate was not specified in the voice input."
                }
            }
        }
    }

    private fun getMockResponse(userInput: String, language: String): ParsedIntentResponse {
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
                explanation = stockExplanation(item, category, quantity, unit, price, total, language)
            )
        }

        fun mandiResponse(item: String, answer: String): ParsedIntentResponse {
            val translatedAnswer = when (item) {
                "Potato" -> when (language) {
                    "Hindi" -> "आज आजादपुर मंडी में आलू का भाव ₹22 से ₹28 प्रति किलो है, जो स्थिर है।"
                    "Hinglish" -> "Aaj Azadpur mandi me aloo ka bhav ₹22-28 per kg hai, bilkul kal jaisa steady."
                    "Tamil" -> "இன்று ஆசாத்பூர் மண்டியில் உருளைக்கிழங்கு விலை கிலோ ₹22-28 ஆக நிலையாக உள்ளது."
                    "Telugu" -> "ఈరోజు ఆజాద్‌పూర్ మండీలో బంగాళాదుంప ధర కిలోకు ₹22-28 గా స్థిరంగా ఉంది."
                    "Kannada" -> "ಇಂದು ಆಜಾದ್‌ಪುರ ಮಂಡಿಯಲ್ಲಿ ಆಲೂಗಡ್ಡೆ ಬೆಲೆ ಕೆಜಿಗೆ ₹22-28 ರಂತೆ ಸ್ಥಿರವಾಗಿದೆ."
                    "Bengali" -> "আজ আজাদপুর মান্ডিতে আলুর দর কেজি প্রতি ২২-২৮ টাকা, যা গতকালের মতই রয়েছে।"
                    "Marathi" -> "आज आझादपूर मंडीमध्ये बटाट्याचा भाव ₹२२-२८ प्रति किलो असून कालसारखाच स्थिर आहे।"
                    else -> "Potato mandi price is Rs 22-28 per kg in Azadpur, stable since yesterday."
                }
                "Onion" -> when (language) {
                    "Hindi" -> "आज वाशी मंडी में प्याज का भाव ₹35 से ₹42 प्रति किलो है, आवक कम होने से दाम बढ़ रहे हैं।"
                    "Hinglish" -> "Aaj Vashi mandi me pyaj ka bhav ₹35-42 per kg chal raha hai, supply kam hone se rate thoda up hai."
                    "Tamil" -> "குறைந்த வரத்து காரணமாக வாஷி மண்டியில் வெங்காயத்தின் விலை கிலோ ₹35-42 ஆக உயர்ந்துள்ளது."
                    "Telugu" -> "తక్కువ సప్లై కారణంగా వాషి మండీలో ఉల్లిపాయ ధర కిలోకు ₹35-42 గా పెరిగింది."
                    "Kannada" -> "ಕಡಿಮೆ ಪೂರೈಕೆಯಿಂದಾಗಿ ವಾಶಿ ಮಂಡಿಯಲ್ಲಿ ಈರುಳ್ಳಿ ಬೆಲೆ ಕೆಜಿಗೆ ₹35-42 ಕ್ಕೆ ಏರಿಕೆಯಾಗಿದೆ."
                    "Bengali" -> "কম সরবরাহের কারণে ওয়াশি মান্ডিতে পেঁয়াজের দর কেজি প্রতি ৩৫-৪২ টাকা বৃদ্ধি পেয়েছে।"
                    "Marathi" -> "कमी आवकमुळे वाशी मंडीत कांद्याचा भाव ₹३५-४२ प्रति किलो पर्यंत वाढला आहे।"
                    else -> "Onion mandi price is Rs 35-42 per kg in Vashi, trending upward on low supply."
                }
                "Tomato" -> when (language) {
                    "Hindi" -> "टमाटर का भाव ₹38 से ₹45 प्रति किलो है, मांग सामान्य है।"
                    "Hinglish" -> "Tamatar ka bhav ₹38-45 per kg chal raha hai, demand normal hai."
                    "Tamil" -> "தக்காளி விலை கிலோ ₹38-45 ஆக மிதமான தேவையில் உள்ளது."
                    "Telugu" -> "టమోటా ధర కిలోకు ₹38-45 గా సాధారణ డిమాండ్‌తో ఉంది."
                    "Kannada" -> "ಟೊಮೆಟೊ ಬೆಲೆ ಕೆಜಿಗೆ ₹38-45 ಹಾಗೆ ಸಾಮಾನ್ಯ ಬೇಡಿಕೆಯಲ್ಲಿದೆ."
                    "Bengali" -> "টমেটোর দর কেজি প্রতি ৩৮-৪৫ টাকা, চাহিদা মাঝারি।"
                    "Marathi" -> "टोमॅटोचा भाव ₹३८-४५ प्रति किलो असून मागणी मध्यम आहे।"
                    else -> "Tomato mandi price is Rs 38-45 per kg with moderate demand."
                }
                else -> answer
            }
            return ParsedIntentResponse(
                type = "query",
                isMandiQuery = true,
                queryCrop = item,
                queryAnswer = translatedAnswer,
                explanation = translatedAnswer
            )
        }

        return when {
            normalized.contains("potato") || normalized.contains("aloo") -> {
                if (normalized.contains("bhav") || normalized.contains("price") || normalized.contains("rate") || normalized.contains("bhaav")) {
                    mandiResponse("Potato", "Aloo mandi bhav is Rs 22-28 per kg in Azadpur, stable.")
                } else {
                    stockResponse("Potato")
                }
            }
            normalized.contains("onion") || normalized.contains("pyaaj") || normalized.contains("pyaj") -> {
                if (normalized.contains("bhav") || normalized.contains("price") || normalized.contains("rate") || normalized.contains("bhaav")) {
                    mandiResponse("Onion", "Pyaj mandi bhav is Rs 35-42 per kg in Vashi, trending upward.")
                } else {
                    stockResponse("Onion")
                }
            }
            normalized.contains("tomato") || normalized.contains("tamatar") -> {
                if (normalized.contains("bhav") || normalized.contains("price") || normalized.contains("rate") || normalized.contains("bhaav")) {
                    mandiResponse("Tomato", "Tamatar mandi bhav is Rs 38-45 per kg.")
                } else {
                    stockResponse("Tomato")
                }
            }
            normalized.contains("ramesh") || normalized.contains("udhaar") || normalized.contains("udhar") || normalized.contains("lent") || normalized.contains("gave") -> {
                val party = if (normalized.contains("ramesh")) "Ramesh" else if (normalized.contains("suresh")) "Suresh" else "Unknown Party"
                val amount = parsePriceFromText(normalized) ?: 0.0
                
                val extText = when (language) {
                    "Hindi" -> if (amount > 0.0) "उधार दर्ज: $party को ₹$amount उधार दिए।" else "उधार दर्ज: $party को दिए।"
                    "Hinglish" -> if (amount > 0.0) "Udhaar likh liya: $party ko ₹$amount udhaar diye." else "Udhaar likh liya: $party ko udhaar diye."
                    "Tamil" -> if (amount > 0.0) "கடன் பதிவு செய்யப்பட்டது: ${party}க்கு ₹$amount." else "கடன் பதிவு செய்யப்பட்டது: ${party}க்கு."
                    "Telugu" -> if (amount > 0.0) "అప్పు నమోదు చేయబడింది: ${party}కి ₹$amount." else "అప్పు నమోదు చేయబడింది: ${party}కి."
                    "Kannada" -> if (amount > 0.0) "ಉದ್ರಿ ದಾಖಲಿಸಲಾಗಿದೆ: ${party}ಗೆ ₹$amount." else "ಉದ್ರಿ ದಾಖಲಿಸಲಾಗಿದೆ: ${party}ಗೆ."
                    "Bengali" -> if (amount > 0.0) "বকেয়া নথিভুক্ত: ${party}কে ₹$amount ধার দেওয়া হয়েছে।" else "বকেয়া নথিভুক্ত: ${party}কে ধার দেওয়া হয়েছে।"
                    "Marathi" -> if (amount > 0.0) "उधारी नोंदवली गेली: ${party}ला ₹$amount." else "उधारी नोंदवली गेली: ${party}ला."
                    else -> if (amount > 0.0) "Udhaar Logged: Lent Rs $amount to $party." else "Udhaar Logged: Lent to $party."
                }
                
                ParsedIntentResponse(
                    type = "transaction",
                    category = "Udhaar",
                    item = "Cash Debit",
                    quantity = 1.0,
                    unit = "items",
                    pricePerUnit = amount,
                    totalAmount = amount,
                    partyName = party,
                    explanation = extText
                )
            }
            normalized.contains("diesel") || normalized.contains("expense") || normalized.contains("rent") || normalized.contains("travel") || normalized.contains("kiraya") -> {
                val amount = parsePriceFromText(normalized) ?: 0.0
                val expenseItem = when {
                    normalized.contains("diesel") || normalized.contains("fuel") || normalized.contains("petrol") -> "Fuel"
                    normalized.contains("rent") || normalized.contains("kiraya") -> "Rent"
                    else -> "Logistics"
                }
                
                val extText = when (language) {
                    "Hindi" -> if (amount > 0.0) "खर्च दर्ज: $expenseItem के लिए ₹$amount।" else "खर्च दर्ज: $expenseItem।"
                    "Hinglish" -> if (amount > 0.0) "Kharach likh liya: $expenseItem ke liye ₹$amount." else "Kharach likh liya: $expenseItem."
                    "Tamil" -> if (amount > 0.0) "செலவு பதிவு செய்யப்பட்டது: $expenseItem ₹$amount." else "செலவு பதிவு செய்யப்பட்டது: $expenseItem."
                    "Telugu" -> if (amount > 0.0) "ఖర్చు నమోదు చేయబడింది: $expenseItem ₹$amount." else "ఖర్చు నమోదు చేయబడింది: $expenseItem."
                    "Kannada" -> if (amount > 0.0) "ಖರ್ಚು ದಾಖಲಿಸಲಾಗಿದೆ: $expenseItem ₹$amount." else "ಖರ್ಚು ದಾಖಲಿಸಲಾಗಿದೆ: $expenseItem."
                    "Bengali" -> if (amount > 0.0) "খরচ নথিভুক্ত: $expenseItem বাবদ ₹$amount।" else "খরচ নথিভুক্ত: $expenseItem।"
                    "Marathi" -> if (amount > 0.0) "खर्च नोंदवला गेला: $expenseItem ₹$amount." else "खर्च नोंदवला गेला: $expenseItem."
                    else -> if (amount > 0.0) "Operational Expense Logged: $expenseItem Rs $amount." else "Operational Expense Logged: $expenseItem."
                }
                
                ParsedIntentResponse(
                    type = "transaction",
                    category = "Expense",
                    item = expenseItem,
                    quantity = 1.0,
                    unit = "items",
                    pricePerUnit = amount,
                    totalAmount = amount,
                    explanation = extText
                )
            }
            normalized.contains("loan") || normalized.contains("credit") -> {
                val ans = when (language) {
                    "Hindi" -> "आपकी ऋण पात्रता बहुत मजबूत है क्योंकि आपकी बिक्री लगातार बढ़ रही है।"
                    "Hinglish" -> "Aapka voice bahi credit limit strong hai kyunki aap roj bikri likh rahe ho."
                    "Tamil" -> "உங்களின் தொடர்ச்சியான விற்பனை காரணமாக உங்களின் கடன் தகுதி வலுவாக உள்ளது."
                    "Telugu" -> "మీ నిరంతర అమ్మకాల కారణంగా మీ రుణ అర్హత బలంగా ఉంది."
                    "Kannada" -> "ನಿಮ್ಮ ನಿರಂತರ ಮಾರಾಟದ ಕಾರಣದಿಂದ ನಿಮ್ಮ ಸಾಲದ ಅರ್ಹತೆ ಸದೃಢವಾಗಿದೆ."
                    "Bengali" -> "আপনার ক্রমাগত বিক্রির কারণে লোণ পাওয়ার যোগ্যতা অত্যন্ত ভালো।"
                    "Marathi" -> "तुमच्या सातत्यपूर्ण विक्रीमुळे तुमची कर्ज पात्रता मजबूत आहे."
                    else -> "Your credit eligibility score is robust due to daily active operating transactions."
                }
                
                ParsedIntentResponse(
                    type = "chat",
                    queryAnswer = ans,
                    explanation = ans
                )
            }
            else -> {
                val ans = when (language) {
                    "Hindi" -> "मैंने सुना: \"$userInput\"। कृपया बोलें: 'दो किलो आलू तीस रुपये' या 'रमेश को पांच सौ उधार दिए'।"
                    "Hinglish" -> "Maine suna: \"$userInput\". Aap bol sakte hain: 'Sold 5 kg onion at 30' ya 'Ramesh ko 500 rupaye udhaar diye'."
                    "Tamil" -> "நான் கேட்டது: \"$userInput\". நீங்கள் பேசலாம்: '5 கிலோ வெங்காயம் 30 ரூபாய்க்கு விற்றேன்'."
                    "Telugu" -> "నేను విన్నది: \"$userInput\". మీరు మాట్లాడవచ్చు: '5 కిలోల ఉల్లిపాయలు 30 రూపాయలకు అమ్మాను'."
                    "Kannada" -> "ನಾನು ಕೇಳಿದ್ದು: \"$userInput\". ನೀವು ಮಾತನಾಡಬಹುದು: '5 ಕೆಜಿ ಈರುಳ್ಳಿ 30 ರೂಪಾಯಿಗೆ ಮಾರಾಟ ಮಾಡಿದೆ'."
                    "Bengali" -> "আমি শুনেছি: \"$userInput\"। আপনি বলতে পারেন: '৫ কেজি পেঁয়াজ ৩০ টাকায় বিক্রি করেছি'।"
                    "Marathi" -> "मी ऐकले: \"$userInput\"। तुम्ही बोलू शकता: '5 किलो कांदा 30 रुपयांनी विकला'."
                    else -> "I heard: \"$userInput\". Try: 'Sold 5 kg onion at 30' or 'Lent Ramesh 500 rupees'."
                }
                ParsedIntentResponse(
                    type = "chat",
                    queryAnswer = ans,
                    explanation = ans
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
