import express from "express";
import path from "path";
import { fileURLToPath } from "url";
import { GoogleGenAI } from "@google/genai";
import dotenv from "dotenv";

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const port = process.env.PORT || 3000;

app.use(express.json());

// Serve static files from React build directory
app.use(express.static(path.join(__dirname, "dist")));

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

app.post("/api/gemini/parse", async (req, res) => {
  const { text, language } = req.body;
  if (!text || typeof text !== "string" || text.trim() === "") {
    return res.status(400).json({ error: "Empty or invalid spoken text inputs provided." });
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

    const outputText = response.text || "";
    const parsedData = JSON.parse(outputText);
    res.json(parsedData);
  } catch (error: any) {
    console.error("Gemini Parse API Error:", error);
    res.status(500).json({
      error: "Failed to parse voice command securely.",
      details: error?.message || String(error),
    });
  }
});

// Fallback to React static bundle
app.get("*", (req, res) => {
  res.sendFile(path.join(__dirname, "dist", "index.html"));
});

app.listen(port, () => {
  console.log(`VoiceOps AI server booted and listening securely on port ${port}`);
});
