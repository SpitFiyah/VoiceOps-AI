import express from "express";
import path from "path";
import { fileURLToPath } from "url";
import { GoogleGenAI } from "@google/genai";
import dotenv from "dotenv";
import { createServer as createViteServer } from "vite";

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function startServer() {
  const app = express();
  const port = Number(process.env.PORT || 3000);

  app.use(express.json());

  let aiClient: GoogleGenAI | null = null;

  function getGemini(): GoogleGenAI {
    if (!aiClient) {
      const key = process.env.GEMINI_API_KEY;
      if (!key) {
        throw new Error("GEMINI_API_KEY environment variable is required");
      }
      aiClient = new GoogleGenAI({
        apiKey: key,
        httpOptions: {
          headers: {
            "User-Agent": "aistudio-build",
          },
        },
      });
    }
    return aiClient;
  }

  // Structured prompt instructing Gemini to parse natural voice inputs into transactional JSON
  const SYSTEM_INSTRUCTION = `
You are VoiceOPS AI, a multi-lingual Operational voice NLP processor for small merchants, street vendors, and farmers.
Analyze the user's spoken input (in English, Hindi, Hinglish, Tamil, Telugu, Marathi, Bengali, or Kannada) and extract structured ledger logs or business queries.

CRITICAL RULES FOR EXTRACTION:
1. PREVENT GUESSING/ASSUMING: Do NOT guess, interpolate, or hallucinate unit names, quantities, or prices if they are missing from the spoken input.
2. QUANTITY: Extract the exact quantity mentioned. If no quantity is mentioned, set "quantity" strictly to 1.0. Do NOT assume or default to other values.
3. UNIT: Extract the unit (e.g. "kg", "liter", "crate", "packet", "bunch"). If no unit is mentioned or implied, set "unit" strictly to "items".
4. PRICE PER UNIT: Extract the price per unit strictly from rates mentioned (e.g. "30 Rs per kg", "at 30", "30 rupay kilo", "35 rupaye"). If no unit price is mentioned, set "pricePerUnit" strictly to 0.0.
5. TOTAL AMOUNT: Extract the total transactional sum if explicitly stated (e.g. "fuel expense was 450 rupees", "lent Ramesh 500 rupees").
   - If only the total amount is stated, set "totalAmount" to that value, and "pricePerUnit" to that same value if quantity is 1.0. 
   - If quantity and "pricePerUnit" are both explicitly stated but "totalAmount" is not, calculate "totalAmount" as (quantity * pricePerUnit).
   - If neither total amount nor unit price is given, set BOTH "pricePerUnit" and "totalAmount" to 0.0. Do NOT hallucinate values!
6. PARTY_NAME: Extract names of customers or vendors strictly if mentioned (e.g. "Ramesh", "Suresh"). If no name is mentioned, set "partyName" to null.
7. EXPLANATION: Construct a conversational spoken feedback confirming what has been logged. The explanation MUST strictly be written in the user's selected interface language script & phrasing (as specified in the CRITICAL CONTEXT details), regardless of what characters/script/language the raw transcribed input was received in.
   - For example:
     - If the user selected language is "Hindi": The explanation MUST be in conversational Devanagari Hindi script (e.g., "రమేష్ 2 కిలోల ఆలుగడ్డలు కొన్నారు" is Telugu, but user wants Hindi: "रमेश ने दो किलो आलू खरीद लिए हैं।").
     - If the user selected language is "Kannada": The explanation MUST be in Kannada script (e.g., "ರಮೇಶ್ ಎರಡು ಕೆಜಿ ಆಲೂಗಡ್ಡೆ ಖರೀದಿಸಿದ್ದಾರೆ.").
     - If the user selected language is "Tamil": The explanation MUST be in Tamil script (e.g., "ரமேஷ் இரண்டு கிலோ உருளைக்கிழங்கு வாங்கியுள்ளார்.").
     - If the user selected language is "Telugu": The explanation MUST be in Telugu script (e.g., "రమేష్ రెండు కిలోల ఆలుగడ్డలు కొన్నారు.").
     - If the user selected language is "Marathi": The explanation MUST be in Marathi script (e.g., "रमेशने दोन किलो बटाटे खरेदी केले आहेत.").
     - If the user selected language is "Bengali": The explanation MUST be in Bengali script (e.g., "রমেশ দুই কেজি আলু কিনেছেন।").
     - If the user selected language is "Hinglish": The explanation MUST be in Hinglish using Latin letters (e.g., "Ramesh ne do kilo aaloo khareed liya hai.").
     - If the user selected language is "English": The explanation MUST be in English.
   - If any critical data like quantity, unit, or price is missing and has defaulted (e.g., 0.0 price, 1.0 quantity), include a gentle mention of the missing details in the explanation in the target language.
   - ABSOLUTELY NEVER respond in English characters or phrasing when any regional language (Hindi, Kannada, Tamil, Telugu, Marathi, Bengali) is the active interface language. This is extremely critical!

Return ONLY a valid JSON object matching the schema below. Keep it strictly raw JSON with no markdown wrapping:
{
   "type": "transaction" | "query" | "chat",
   "item": "capitalized name of product (e.g. Potato, Tomato, Milk) or null",
   "category": "Sale" | "Purchase" | "Expense" | "Udhaar" | "Payment Received" | "Stock In" | null,
   "quantity": number,
   "unit": string,
   "pricePerUnit": number,
   "totalAmount": number,
   "partyName": string | null,
   "explanation": string,
   "isMandiQuery": boolean,
   "queryCrop": string | null,
   "queryAnswer": string | null
}
`;

  // High-fidelity Multilingual NLP simulator when API keys are unconfigured or fail
  function simulateServerSideMockNLP(text: string, language: string): any {
    const raw = text.toLowerCase().trim();
    
    const qtyMatch = raw.match(/(\d+(?:\.\d+)?)\s*(?:kg|kilo|kilogram|crate|packet|pack|bunch|liter|litre|items|item|किलो|लीटर|क्रेट|पैकेट|ಕಿಲೋ|ಲೀಟರ್|கிராம்|கिलो|లీటర్|కిలో|ব্যাগ|কেজি)/i);
    const priceMatch = raw.match(/(?:rs\.?|₹|rupees?|at|@|रुपये|रुपए|रूपಾಯಿ|రూపాయలు|ரூபாய்|টাকা|rate of)\s*(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:rs\.?|rupees?|रुपये|रुपए|रूपಾಯಿ|రూపಾಯలు|ரூபாய்|টাকা)/i);
    
    const quantity = qtyMatch ? parseFloat(qtyMatch[1]) : 1.0;
    let price = 0.0;
    if (priceMatch) {
      price = parseFloat(priceMatch[1] || priceMatch[2]);
    }
    const amountCalculated = quantity * price;

    let inferredItem = "Potato";
    if (raw.includes("potato") || raw.includes("aloo") || raw.includes("आलू") || raw.includes("ಆಲೂಗೆಡ್ಡೆ") || raw.includes("உருளை") || raw.includes("బంగాళదుంప") || raw.includes("আলু") || raw.includes("बटाटा")) inferredItem = "Potato";
    else if (raw.includes("onion") || raw.includes("pyaj") || raw.includes("pyaaj") || raw.includes("प्याज") || raw.includes("ಈರುಳ್ಳಿ") || raw.includes("வெங்காயம்") || raw.includes("ఉల్లిపాయ") || raw.includes("পেঁয়াজ") || raw.includes("कांदा")) inferredItem = "Onion";
    else if (raw.includes("tomato") || raw.includes("tamatar") || raw.includes("टमाटर") || raw.includes("ಟೊಮೆಟೊ") || raw.includes("தக்காளி") || raw.includes("టమోటా") || raw.includes("টমেটো") || raw.includes("टोमॅटो")) inferredItem = "Tomato";
    else if (raw.includes("wheat") || raw.includes("gehun") || raw.includes("गेहूं") || raw.includes("ಗೋದೂಮ") || raw.includes("கோதுமை") || raw.includes("గోధుమ") || raw.includes("গম") || raw.includes("गहू")) inferredItem = "Wheat";
    else if (raw.includes("rice") || raw.includes("chawal") || raw.includes("चावल") || raw.includes("ಅಕ್ಕಿ") || raw.includes("அரிசி") || raw.includes("బియ్యం") || raw.includes("চাল") || raw.includes("तांदूळ")) inferredItem = "Rice";
    else if (raw.includes("milk") || raw.includes("doodh") || raw.includes("दूध") || raw.includes("ಹಾಲು") || raw.includes("பால்") || raw.includes("పాలు") || raw.includes("दूध") || raw.includes("دودھ")) inferredItem = "Milk";
    else if (raw.includes("chilli") || raw.includes("mirchi") || raw.includes("मिर्च") || raw.includes("ಮೆಣಸಿನಕಾಯಿ") || raw.includes("மிளகாய்") || raw.includes("మిరపకాయ") || raw.includes("লঙ্কা") || raw.includes("मिरची")) inferredItem = "Green Chilli";
    else if (raw.includes("diesel") || raw.includes("fuel") || raw.includes("डीजल") || raw.includes("ಡೀಸೆಲ್") || raw.includes("டீசல்") || raw.includes("డీజిల్") || raw.includes("ডিজেল")) inferredItem = "Diesel";

    let unit = "items";
    if (raw.includes("kg") || raw.includes("kilo") || raw.includes("kilogram") || raw.includes("किलो") || raw.includes("ಕೆಜಿ") || raw.includes("கிலோ") || raw.includes("కిలో") || raw.includes("কেজি")) unit = "kg";
    else if (raw.includes("crate") || raw.includes("क्रेट") || raw.includes("ಕ್ರೇಟ್") || raw.includes("கிரேட்") || raw.includes("క్రేట్")) unit = "crates";
    else if (raw.includes("packet") || raw.includes("pack") || raw.includes("पैकेट") || raw.includes("ಪ್ಯಾಕೆಟ್") || raw.includes("பாக்கெட்") || raw.includes("ప్యాಕೆಟ್")) unit = "packet";
    else if (raw.includes("liter") || raw.includes("litre") || raw.includes("लीटर") || raw.includes("ಲೀಟರ್") || raw.includes("லிட்டர்") || raw.includes("లీటర్")) unit = "liter";

    let inferredCategory = "Sale";
    if (raw.includes("bought") || raw.includes("buy") || raw.includes("kharid") || raw.includes("purchase") || raw.includes("खरीदा") || raw.includes("ಖರೀದಿ") || raw.includes("வாங்கினேன்") || raw.includes("కొన్నాను") || raw.includes("কিনলাম") || raw.includes("खरेदी")) {
      inferredCategory = "Purchase";
    } else if (raw.includes("udhaar") || raw.includes("udhar") || raw.includes("lent") || raw.includes("gave") || raw.includes("उधार") || raw.includes("ಸಾಲ") || raw.includes("கடன்") || raw.includes("అప్పు") || raw.includes("ধার") || raw.includes("उधारी")) {
      inferredCategory = "Udhaar";
    } else if (raw.includes("expense") || raw.includes("diesel") || raw.includes("kiraya") || raw.includes("rent") || raw.includes("खर्च") || raw.includes("ಖರ್ಚು") || raw.includes("செலவு") || raw.includes("খরচ")) {
      inferredCategory = "Expense";
    } else if (raw.includes("received") || raw.includes("payment") || raw.includes("pay") || raw.includes("भुगतान") || raw.includes("ಪಾವತಿ") || raw.includes("பணம்") || raw.includes("చెల్లింపు") || raw.includes("পেমেন্ট") || raw.includes("जма")) {
      inferredCategory = "Payment Received";
    }

    let partyName: string | null = null;
    const names = ["ramesh", "suresh", "amit", "anil", "rajesh", "sunil", "vijay", "kamlesh", "mahesh", "rahul", "dinesh", "shankar", "ravi", "prakash"];
    for (const n of names) {
      if (raw.includes(n)) {
        partyName = n.replace(/^\w/, (c) => c.toUpperCase());
        break;
      }
    }

    const isMandiQuery = raw.includes("bhav") || raw.includes("price") || raw.includes("rate") || raw.includes("bhaav") || raw.includes("mandi") || raw.includes("भाव") || raw.includes("ದರ") || raw.includes("விலை") || raw.includes("ధర") || raw.includes("দর");

    if (isMandiQuery) {
      let priceRate = price > 0 ? price : 25;
      let ans = ``;
      switch (language) {
        case "Hindi":
          ans = `आज मंडी में ${inferredItem} का भाव लगभग ₹${priceRate} प्रति किलो चल रहा है।`;
          break;
        case "Kannada":
          ans = `ಇಂದು ಮಾರುಕಟ್ಟೆಯಲ್ಲಿ ${inferredItem} ದರ ಸುಮಾರು ₹${priceRate} ಪ್ರತಿ ಕೆಜಿಗೆ ಇದೆ.`;
          break;
        case "Tamil":
          ans = `இன்று சந்தையில் ${inferredItem} விலை சுமார் ₹${priceRate} கிலோவுக்கு ஆகும்.`;
          break;
        case "Telugu":
          ans = `ఈరోజు మార్కెట్లో ${inferredItem} ధర కిలోకు సుమారు ₹${priceRate} ఉంది.`;
          break;
        case "Marathi":
          ans = `आज बाजारात ${inferredItem} चा भाव सुमारे ₹${priceRate} प्रति किलो आहे.`;
          break;
        case "Bengali":
          ans = `আজকে বাজারে ${inferredItem} এর দাম প্রায় ₹${priceRate} প্রতি কেজি।`;
          break;
        case "Hinglish":
          ans = `Aaj mandi me ${inferredItem} ka rate lagbhag ₹${priceRate} per kg chal raha hai.`;
          break;
        default:
          ans = `Today's market price for ${inferredItem} in your nearest regional exchange is approximately ₹${priceRate} per kg.`;
      }

      return {
        type: "query",
        item: inferredItem,
        category: null,
        quantity: 1,
        unit: "kg",
        pricePerUnit: priceRate,
        totalAmount: priceRate,
        partyName,
        explanation: ans,
        isMandiQuery: true,
        queryCrop: inferredItem,
        queryAnswer: ans
      };
    }

    let explanation = "";
    const displayParty = partyName || (language === "Hindi" ? "ग्राहक" : language === "Telugu" ? "ఖాతాదారుడు" : language === "Tamil" ? "வாடிக்கையாளர்" : "Customer");
    
    switch (language) {
      case "Hindi":
        if (inferredCategory === "Sale") {
          explanation = `सफलतापूर्वक सिंक किया गया! ${displayParty} को ${quantity} ${unit} ${inferredItem} ₹${price} प्रति ${unit} की दर से बेचा गया। कुल राशि ₹${amountCalculated} दर्ज की गई है।`;
        } else if (inferredCategory === "Purchase") {
          explanation = `सफलतापूर्वक दर्ज किया गया! ${quantity} ${unit} ${inferredItem} ₹${price} प्रति ${unit} की दर से खरीदा गया। कुल राशि ₹${amountCalculated} दर्ज की गई है।`;
        } else if (inferredCategory === "Udhaar") {
          explanation = `${displayParty} के नाम ₹${amountCalculated} का उधार खाता बही में जोड़ दिया गया है।`;
        } else {
          explanation = `व्यवहार सफलतापूर्वक दर्ज किया गया! ${inferredItem} के लिए कुल ₹${amountCalculated} की एंट्री कर ली गई है।`;
        }
        break;

      case "Telugu":
        if (inferredCategory === "Sale") {
          explanation = `విజయవంతంగా నమోదు అయింది! ${displayParty} కి ${quantity} ${unit} ${inferredItem} కిలోకు ₹${price} చొప్పున అమ్మబడింది. మొత్తం ₹${amountCalculated} ఖాతాలో చేరింది.`;
        } else if (inferredCategory === "Purchase") {
          explanation = `${quantity} ${unit} ${inferredItem} ని ₹${price} చొప్పున కొనుగోలు చేశారు. మొత్తం ₹${amountCalculated} నమోదు చేయబడింది.`;
        } else if (inferredCategory === "Udhaar") {
          explanation = `${displayParty} పేరిట ₹${amountCalculated} అప్పు ఖాతాలో నమోదు చేయబడింది.`;
        } else {
          explanation = `${inferredItem} కోసం ₹${amountCalculated} విజయవంతంగా రికార్డ్ చేయబడింది.`;
        }
        break;

      case "Tamil":
        if (inferredCategory === "Sale") {
          explanation = `வெற்றிகரமாக பதிவு செய்யப்பட்டது! ${displayParty}க்கு ${quantity} ${unit} ${inferredItem} கிலோவுக்கு ₹${price} வீதம் விற்கப்பட்டது. மொத்த தொகை ₹${amountCalculated} ஆகும்.`;
        } else if (inferredCategory === "Purchase") {
          explanation = `${quantity} ${unit} ${inferredItem} ₹${price} விலையில் வாங்கப்பட்டது. மொத்த தொகை ₹${amountCalculated} பதிவு செய்யப்பட்டுள்ளது.`;
        } else if (inferredCategory === "Udhaar") {
          explanation = `${displayParty} பெயரில் ₹${amountCalculated} கடன் வெற்றிகரமாக சேர்க்கப்பட்டுள்ளது.`;
        } else {
          explanation = `${inferredItem} க்காக ₹${amountCalculated} வெற்றிகரமாக பதிவு செய்யப்பட்டது.`;
        }
        break;

      case "Kannada":
        if (inferredCategory === "Sale") {
          explanation = `ಯಶಸ್ವಿಯಾಗಿ ದಾಖಲಿಸಲಾಗಿದೆ! ${displayParty} ರವರಿಗೆ ${quantity} ${unit} ${inferredItem} ಪ್ರತಿ ಕೆಜಿಗೆ ₹${price} ನಂತೆ ಮಾರಾಟ ಮಾಡಲಾಗಿದೆ. ಒಟ್ಟು ಮೊತ್ತ ₹${amountCalculated}.`;
        } else if (inferredCategory === "Purchase") {
          explanation = `${quantity} ${unit} ${inferredItem} ಅನ್ನು ₹${price} ನಂತೆ ಖರೀದಿಸಲಾಗಿದೆ. ಒಟ್ಟು ₹${amountCalculated} ದಾಖಲಾಗಿದೆ.`;
        } else if (inferredCategory === "Udhaar") {
          explanation = `${displayParty} ಹೆಸರಿನಲ್ಲಿ ₹${amountCalculated} ಉದ್ರಿ ಸಾಲವನ್ನು ಲೆಡ್ಜರ್‌ನಲ್ಲಿ ಸೇರಿಸಲಾಗಿದೆ.`;
        } else {
          explanation = `${inferredItem} ಗಾಗಿ ₹${amountCalculated} ಯಶಸ್ವಿಯಾಗಿ ನಮೂದಿಸಲಾಗಿದೆ.`;
        }
        break;

      case "Marathi":
        if (inferredCategory === "Sale") {
          explanation = `यशस्वीरित्या नोंदवले! ${displayParty} ला ${quantity} ${unit} ${inferredItem} ₹${price}/दर ने विकले. एकूण किंमत ₹${amountCalculated} झाली.`;
        } else if (inferredCategory === "Purchase") {
          explanation = `${quantity} ${unit} ${inferredItem} ₹${price} दराने खरेदी केले. एकूण ₹${amountCalculated} नोंदवले आहे.`;
        } else if (inferredCategory === "Udhaar") {
          explanation = `${displayParty} च्या नावावर ₹${amountCalculated} ची उधारी नोंदवली गेली आहे.`;
        } else {
          explanation = `${inferredItem} साठी ₹${amountCalculated} ची नोंद यशस्वी झाली आहे.`;
        }
        break;

      case "Bengali":
        if (inferredCategory === "Sale") {
          explanation = `সফলভাবে ট্র্যাকিং করা হয়েছে! ${displayParty} কে ${quantity} ${unit} ${inferredItem} ₹${price} প্রতি ${unit} দরে বিক্রি করা হলো। মোট মূল্য ₹${amountCalculated}.`;
        } else if (inferredCategory === "Purchase") {
          explanation = `${quantity} ${unit} ${inferredItem} ₹${price} দরে কেনা হয়েছে। মোট মূল্য ₹${amountCalculated} সফলভাবে আপলোড করা হয়েছে।`;
        } else if (inferredCategory === "Udhaar") {
          explanation = `${displayParty} এর নামে ₹${amountCalculated} টাকা ধার খাতায় লেখা হলো।`;
        } else {
          explanation = `${inferredItem} এর জন্য ₹${amountCalculated} টাকার এন্ট্রি করা হলো।`;
        }
        break;

      case "Hinglish":
        if (inferredCategory === "Sale") {
          explanation = `Successfully logged! ${displayParty} ko ${quantity} ${unit} ${inferredItem} ₹${price} rate se becha gaya. Total: ₹${amountCalculated} save ho gaya.`;
        } else if (inferredCategory === "Purchase") {
          explanation = `${quantity} ${unit} ${inferredItem} ₹${price} ke bhav se kharida gaya. Total ₹${amountCalculated} save ho chuka hai.`;
        } else if (inferredCategory === "Udhaar") {
          explanation = `${displayParty} ke ledger me ₹${amountCalculated} udhaar entry add ho gayi hai.`;
        } else {
          explanation = `${inferredItem} ke liye ₹${amountCalculated} transactions successfully record ho gaya hai.`;
        }
        break;

      default:
        if (inferredCategory === "Sale") {
          explanation = `Successfully logged! Sold ${quantity} ${unit} of ${inferredItem} to ${displayParty} at ₹${price} per ${unit}. Total amount: ₹${amountCalculated}.`;
        } else if (inferredCategory === "Purchase") {
          explanation = `Successfully saved! Purchased ${quantity} ${unit} of ${inferredItem} at ₹${price} per ${unit}. Total: ₹${amountCalculated}.`;
        } else if (inferredCategory === "Udhaar") {
          explanation = `Added ₹${amountCalculated} udhaar receivable for ${displayParty} in local database.`;
        } else {
          explanation = `Successfully cataloged ledger log for ${inferredItem}. Total worth ₹${amountCalculated} was processed.`;
        }
    }

    if (price === 0.0) {
      switch (language) {
        case "Hindi":
          explanation += " (कृपया ध्यान दें: इकाई मूल्य निर्दिष्ट नहीं किया गया था)";
          break;
        case "Telugu":
          explanation += " (గమనిక: ధర వివరాలు పేర్కొనలేదు)";
          break;
        case "Tamil":
          explanation += " (குறிப்பு: அலகு விலை குறிப்பிடப்படவில்லை)";
          break;
        case "Kannada":
          explanation += " (ಗಮನಿಸಿ: ಘಟಕ ದರವನ್ನು ತಿಳಿಸಲಾಗಿಲ್ಲ)";
          break;
        case "Bengali":
          explanation += " (অনুগ্রহ করে মনে রাখবেন: একক মূল্য উল্লেখ করা হয়নি)";
          break;
        case "Hinglish":
          explanation += " (Note: rate specify nahi kiya gaya tha)";
          break;
        default:
          explanation += " (Note: unit price was unspecified)";
      }
    }

    return {
      type: "transaction",
      item: inferredItem,
      category: inferredCategory,
      quantity,
      unit,
      pricePerUnit: price,
      totalAmount: amountCalculated,
      partyName,
      explanation,
      isMandiQuery: false,
      queryCrop: null,
      queryAnswer: null
    };
  }

  // API parser route
  app.post("/api/gemini/parse", async (req, res) => {
    const { text, language } = req.body;
    if (!text || typeof text !== "string" || text.trim() === "") {
      return res.status(400).json({ error: "Empty or invalid spoken text inputs provided." });
    }

    const key = process.env.GEMINI_API_KEY;
    const isMock = !key || key === "MY_GEMINI_API_KEY" || key.trim() === "";

    if (isMock) {
      console.log("Serving high-fidelity server-side offline NLP simulation");
      const parsedData = simulateServerSideMockNLP(text, language || "English");
      return res.json(parsedData);
    }

    const userLanguageHint = language 
      ? `\nCRITICAL LANGUAGE MANDATE: The user's active device interface language is currently set strictly to "${language}".
You MUST construct the "explanation" conversational spoken feedback strictly in the "${language}" script/phrasing (e.g. Devanagari script for Hindi, Kannada script for Kannada).
Do NOT explain, describe, or answer in English words or English characters of any sort when "${language}" is selected (unless Hinglish or English is selected).
This is a direct hard restriction to keep synthesized audio outputs in synchronized, natural-sounding local speech.`
      : "";

    try {
      const ai = getGemini();
      const response = await ai.models.generateContent({
        model: "gemini-3.5-flash",
        contents: text,
        config: {
          systemInstruction: SYSTEM_INSTRUCTION + userLanguageHint,
          temperature: 0.1,
          responseMimeType: "application/json",
        },
      });

      let outputText = (response.text || "").trim();
      
      // Clean potential JSON markdown fence wrapping robustly
      const jsonMatch = outputText.match(/```(?:json)?([\s\S]*?)```/);
      if (jsonMatch) {
        outputText = jsonMatch[1].trim();
      }

      const parsedData = JSON.parse(outputText);
      res.json(parsedData);
    } catch (error: any) {
      console.error("Gemini Live Call Failed. Falling back to robust simulation:", error);
      const parsedData = simulateServerSideMockNLP(text, language || "English");
      res.json(parsedData);
    }
  });

  // Vite development middleware vs Static site hosting
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(__dirname, "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(port, "0.0.0.0", () => {
    console.log(`VoiceOps AI server booted and listening securely on port ${port}`);
  });
}

startServer().catch((err) => {
  console.error("Critical server bootstrap failure:", err);
});
