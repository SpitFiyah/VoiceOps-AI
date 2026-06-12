import React, { useState, useEffect, useRef } from "react";
import {
  Mic,
  MicOff,
  Search,
  Plus,
  Trash2,
  TrendingDown,
  TrendingUp,
  RefreshCw,
  TrendingUp as TrendIcon,
  Info,
  Calendar as CalendarIcon,
  User,
  ShoppingBag,
  MapPin,
  ChevronRight,
  Database,
  ArrowUpRight,
  ArrowDownLeft,
  DollarSign,
  Briefcase,
  Layers,
  HelpCircle,
  Clock,
  ExternalLink,
  Coins,
  Copy,
  Check,
  Smartphone
} from "lucide-react";

// Multi-Language Localization Engine
interface TranslationSet {
  APP_NAME: string;
  SUBTITLE: string;
  AI_ACTIVE: string;
  TOTAL_SALES: string;
  UDHAAR_DUE: string;
  EXPENSES: string;
  REGION_DEFAULT_BANNER: string;
  MIC_TAP_PROMPT: string;
  TRANSLATING: string;
  TAP_TO_SPEAK_LABEL: string;
  DEMO_CHIPS_HEADER: string;
  LEDGER_STREAM_TAB: string;
  MANDI_PRICES_TAB: string;
  VOICE_CREDIT_TAB: string;
  VOICE_TRANSLATION_TITLE: string;
  EXPLAINED_OUT_LOUD: string;
  ITEM: string;
  CAT: string;
  QTY: string;
  RATE: string;
  TOTAL: string;
  DEBT: string;
  DONE_BUTTON: string;
}

const LOCALIZATION: Record<string, TranslationSet> = {
  English: {
    APP_NAME: "VoiceOps AI",
    SUBTITLE: "Voice-First Operating System for Small Merchants",
    AI_ACTIVE: "AI CORE ACTIVE",
    TOTAL_SALES: "Total Sales",
    UDHAAR_DUE: "Udhaar (Due)",
    EXPENSES: "Expenses",
    REGION_DEFAULT_BANNER: "Region default set to",
    MIC_TAP_PROMPT: "TAP MICROPHONE TO LOG A VENTURE TRANSACTION",
    TRANSLATING: "TRANSLATING VOICE PAYLOAD...",
    TAP_TO_SPEAK_LABEL: "Tap to Speak (or type below)",
    DEMO_CHIPS_HEADER: "SUGGESTED DEMO PHRASES",
    LEDGER_STREAM_TAB: "Ledger Timeline",
    MANDI_PRICES_TAB: "Mandi Prices",
    VOICE_CREDIT_TAB: "Voice Credit Score",
    VOICE_TRANSLATION_TITLE: "Voice Transaction Structured Logs",
    EXPLAINED_OUT_LOUD: "AUDIO EXPLANATION RETURNED",
    ITEM: "Item",
    CAT: "Category",
    QTY: "Qty",
    RATE: "Rate",
    TOTAL: "Total Amount",
    DEBT: "Party / Borrower",
    DONE_BUTTON: "Confirm & Commit",
  },
  Hindi: {
    APP_NAME: "वॉयसऑप्स एआई",
    SUBTITLE: "छोटे व्यापारियों के लिए वॉयस-फर्स्ट ऑपरेटिंग सिस्टम",
    AI_ACTIVE: "एआई कोर सक्रिय",
    TOTAL_SALES: "कुल बिक्री",
    UDHAAR_DUE: "उधार (देय)",
    EXPENSES: "कुल खर्च",
    REGION_DEFAULT_BANNER: "क्षेत्र का डिफ़ॉल्ट सेट है",
    MIC_TAP_PROMPT: "लेन-देन दर्ज करने के लिए माइक्रोफ़ोन दबाएं",
    TRANSLATING: "आवाज़ का अनुवाद हो रहा है...",
    TAP_TO_SPEAK_LABEL: "बोलने के लिए टैप करें (या नीचे लिखें)",
    DEMO_CHIPS_HEADER: "सुझाए गए वाक्यांश",
    LEDGER_STREAM_TAB: "बहीखाता घटनाक्रम",
    MANDI_PRICES_TAB: "मंडी के भाव",
    VOICE_CREDIT_TAB: "क्रेडिट स्कोर",
    VOICE_TRANSLATION_TITLE: "वॉयस ट्रांसलेशन रिपोर्ट",
    EXPLAINED_OUT_LOUD: "ऑडियो पुष्टि वापस मिली",
    ITEM: "उत्पाद",
    CAT: "श्रेणी",
    QTY: "मात्रा",
    RATE: "दर (कीमत)",
    TOTAL: "कुल राशि",
    DEBT: "ऋणदाता / पक्ष",
    DONE_BUTTON: "स्वीकार और सहेजें",
  },
  Hinglish: {
    APP_NAME: "VoiceOps AI",
    SUBTITLE: "Chote Vyapariyon ke liye Voice Ledger Engine",
    AI_ACTIVE: "AI CORE RUNNING",
    TOTAL_SALES: "Total Sales",
    UDHAAR_DUE: "Udhaar (Baaki)",
    EXPENSES: "Kharche / Expense",
    REGION_DEFAULT_BANNER: "Region default set hai",
    MIC_TAP_PROMPT: "TRANSACTION LOG KARNE KE LIYE MIC DABAYEIN",
    TRANSLATING: "VOICE PROCESSING CHAL RAHI HAI...",
    TAP_TO_SPEAK_LABEL: "Bolne ke liye click karein (ya type karein)",
    DEMO_CHIPS_HEADER: "EASY DEMO FORMULAS",
    LEDGER_STREAM_TAB: "Khaata Timeline",
    MANDI_PRICES_TAB: "Mandi Rates",
    VOICE_CREDIT_TAB: "Trust Credit Score",
    VOICE_TRANSLATION_TITLE: "Voice Transaction Structure",
    EXPLAINED_OUT_LOUD: "AI SE SPOKEN FEEDBACK",
    ITEM: "Cheez / Item",
    CAT: "Category",
    QTY: "Quantity",
    RATE: "Rate per Piece",
    TOTAL: "Total Amount",
    DEBT: "Kiske naam (Party)",
    DONE_BUTTON: "Theek Hai",
  },
  Tamil: {
    APP_NAME: "வாய்ஸ்ஆப்ஸ் ஏஐ",
    SUBTITLE: "சிறு வணிகர்களுக்கான குரல்வழி இயக்க முறைமை",
    AI_ACTIVE: "செயலில் உள்ளது",
    TOTAL_SALES: "மொத்த விற்பனை",
    UDHAAR_DUE: "உதார் கடன் பாக்கி",
    EXPENSES: "வெளிச்செல்லும் செலவு",
    REGION_DEFAULT_BANNER: "இயல்புநிலை பகுதி",
    MIC_TAP_PROMPT: "பரிவர்த்தனையைப் பதிவு செய்ய மைக் தொட்டுப் பேசவும்",
    TRANSLATING: "குரல் செயலாக்கப்படுகிறது...",
    TAP_TO_SPEAK_LABEL: "பேச தட்டவும் (அல்லது கீழே தட்டச்சு செய்யவும்)",
    DEMO_CHIPS_HEADER: "மாதிரி சொற்றொடர்கள்",
    LEDGER_STREAM_TAB: "பேரேட்டு காலவரிசை",
    MANDI_PRICES_TAB: "மண்டி நிலவரம்",
    VOICE_CREDIT_TAB: "குரல் கடன் மதிப்பு",
    VOICE_TRANSLATION_TITLE: "குரல் பரிவர்த்தனை தகவல்கள்",
    EXPLAINED_OUT_LOUD: "ஒலி விளக்க உரை",
    ITEM: "பொருள்",
    CAT: "வகை",
    QTY: "அளவு",
    RATE: "விகிதம்",
    TOTAL: "மொத்த தொகை",
    DEBT: "கடன் வாங்கியவர்",
    DONE_BUTTON: "உறுதிப்படுத்து",
  },
  Telugu: {
    APP_NAME: "వాయిస్ఆప్స్ AI",
    SUBTITLE: "చిన్న వ్యాపారుల కోసం వాయిస్-ఫస్ట్ ఆపరేటింగ్ సిస్టమ్",
    AI_ACTIVE: "AI కోర్ యాక్టివ్",
    TOTAL_SALES: "మొత్తం అమ్మకాలు",
    UDHAAR_DUE: "ఉధార్ (బాకీ)",
    EXPENSES: "మొత్తం ఖర్చులు",
    REGION_DEFAULT_BANNER: "ప్రాంతీయ డిఫాల్ట్ సెట్ చేయబడింది",
    MIC_TAP_PROMPT: "లావాదేవీని నమోదు చేయడానికి మైక్రోఫోన్ నొక్కండి",
    TRANSLATING: "వాయిస్ అనువదించబడుతోంది...",
    TAP_TO_SPEAK_LABEL: "మాట్లాడటానికి నొక్కండి (లేదా కింద టైప్ చేయండి)",
    DEMO_CHIPS_HEADER: "సూచించబడిన డెమో వాక్యాలు",
    LEDGER_STREAM_TAB: "ఖాతా టైమ్‌లైన్",
    MANDI_PRICES_TAB: "మండి ధరలు",
    VOICE_CREDIT_TAB: "వాయిస్ క్రెడిట్ స్కోర్",
    VOICE_TRANSLATION_TITLE: "వాయిస్ లావాదేవీల నివేదిక",
    EXPLAINED_OUT_LOUD: "AI నుండి ఆడియో సమాధానం",
    ITEM: "వస్తువు",
    CAT: "వర్గం",
    QTY: "పరిమాణం",
    RATE: "ధర (రేటు)",
    TOTAL: "మొత్తం విలువ",
    DEBT: "బాకీదారుడు / పార్టీ",
    DONE_BUTTON: "ధృవీకరించండి",
  },
  Marathi: {
    APP_NAME: "व्हाइसऑप्स एआय",
    SUBTITLE: "लहान व्यापाऱ्यांसाठी व्हॉइस-फर्स्ट ऑपरेटिंग सिस्टीम",
    AI_ACTIVE: "एआय कोर सक्रिय",
    TOTAL_SALES: "एकूण विक्री",
    UDHAAR_DUE: "उधारी (बाकी)",
    EXPENSES: "एकूण खर्च",
    REGION_DEFAULT_BANNER: "प्रादेशिक डीफॉल्ट सेट आहे",
    MIC_TAP_PROMPT: "व्यवहार नोंदवण्यासाठी मायक्रोफोन दाबा",
    TRANSLATING: "आवाझ अनुवादित होत आहे...",
    TAP_TO_SPEAK_LABEL: "बोलण्यासाठी दाबा (किंवा खाली टाईप करा)",
    DEMO_CHIPS_HEADER: "सुचविलेले डेमो वाक्य",
    LEDGER_STREAM_TAB: "खाते टाईमलाईन",
    MANDI_PRICES_TAB: "मंडीचे भाव",
    VOICE_CREDIT_TAB: "व्हॉइस क्रेडिट स्कोर",
    VOICE_TRANSLATION_TITLE: "व्हॉइस व्यवहार अहवाल",
    EXPLAINED_OUT_LOUD: "एआय कडून ऑडिओ स्पष्टीकरण",
    ITEM: "उत्पादन",
    CAT: "श्रेणी",
    QTY: "प्रमाण",
    RATE: "दर (किंमत)",
    TOTAL: "एकूण रक्कम",
    DEBT: "उधार घेणारा / पक्ष",
    DONE_BUTTON: "स्वीकार करा",
  },
  Bengali: {
    APP_NAME: "ভয়েসঅপস এআই",
    SUBTITLE: "ক্ষুদ্র ব্যবসায়ীদের জন্য ভয়েস-ফার্স্ট অপারেটিং সিস্টেম",
    AI_ACTIVE: "এআই কোর সক্রিয়",
    TOTAL_SALES: "মোট বিক্রি",
    UDHAAR_DUE: "উধার (বাকি)",
    EXPENSES: "মোট খরচ",
    REGION_DEFAULT_BANNER: "আঞ্চলিক ডিফল্ট সেট করা হয়েছে",
    MIC_TAP_PROMPT: "লেনদেন নথিভুক্ত করতে মাইক্রোফোন টিপুন",
    TRANSLATING: "কণ্ঠস্বর অনুবাদ করা হচ্ছে...",
    TAP_TO_SPEAK_LABEL: "বলার জন্য আলতো চাপুন (অথবা নিচে লিখুন)",
    DEMO_CHIPS_HEADER: "প্রস্তাবিত ডেমো বাক্যাংশ",
    LEDGER_STREAM_TAB: "খাতা টাইমলাইন",
    MANDI_PRICES_TAB: "মন্ডির দর",
    VOICE_CREDIT_TAB: "ভয়েস ক্রেডিট স্কোর",
    VOICE_TRANSLATION_TITLE: "ভয়েস লেনদেনের রিপোর্ট",
    EXPLAINED_OUT_LOUD: "এআই থেকে অডিও ব্যাখ্যা",
    ITEM: "পণ্য",
    CAT: "বিভাগ",
    QTY: "পরিমাণ",
    RATE: "দর (মূল্য)",
    TOTAL: "মোট টাকা",
    DEBT: "পার্টি / ঋণগ্রহীতা",
    DONE_BUTTON: "নিশ্চিত করুন",
  },
  Kannada: {
    APP_NAME: "ವಾಯ್ಸ್‌ಆಪ್ಸ್ AI",
    SUBTITLE: "ಸಣ್ಣ ವ್ಯಾಪಾರಿಗಳಿಗಾಗಿ ವಾಯ್ಸ್-ಫಸ್ಟ್ ಆಪರೇಟಿಂಗ್ ಸಿಸ್ಟಮ್",
    AI_ACTIVE: "AI ಕೋರ್ ಸಕ್ರಿಯವಾಗಿದೆ",
    TOTAL_SALES: "ಒಟ್ಟು ಮಾರಾಟ",
    UDHAAR_DUE: "ಉದರಿ (ಬಾಕಿ)",
    EXPENSES: "ಒಟ್ಟು ಖರ್ಚುಗಳು",
    REGION_DEFAULT_BANNER: "ಪ್ರಾದೇಶಿಕ ಡೀಫಾಲ್ಟ್ ಹೊಂದಿಸಲಾಗಿದೆ",
    MIC_TAP_PROMPT: "ವಹಿವಾಟನ್ನು ದಾಖಲಿಸಲು ಮೈಕ್ರೊಫೋನ್ ಒತ್ತಿರಿ",
    TRANSLATING: "ಧ್ವನಿ ಅನುವಾದಿಸಲಾಗುತ್ತಿದೆ...",
    TAP_TO_SPEAK_LABEL: "ಮಾತನಾಡಲು ಒತ್ತಿ (ಅಥವಾ ಕೆಳಗೆ ಟೈಪ್ ಮಾಡಿ)",
    DEMO_CHIPS_HEADER: "ಸೂಚಿಸಲಾದ ಡೆಮೊ ವಾಕ್ಯಗಳು",
    LEDGER_STREAM_TAB: "ಖಾತಾ ಟೈಮ್‌ಲೈನ್",
    MANDI_PRICES_TAB: "ಮಂಡಿ ದರಗಳು",
    VOICE_CREDIT_TAB: "ವಾಯ್ಸ್ ಕ್ರೆಡಿಟ್ ಸ್ಕೋರ್",
    VOICE_TRANSLATION_TITLE: "ಧ್ವನಿ ವಹಿವಾಟು ವರದಿ",
    EXPLAINED_OUT_LOUD: "AI ನಿಂದ ಆಡಿಯೋ ವಿವರಣೆ",
    ITEM: "ವಸ್ತು",
    CAT: "ವರ್ಗ",
    QTY: "ಪ್ರಮಾಣ",
    RATE: "ದರ (ಬೆಲೆ)",
    TOTAL: "ಒಟ್ಟು ಮೊತ್ತ",
    DEBT: "ಬಾಕಿದಾರ / ಪಾರ್ಟಿ",
    DONE_BUTTON: "ಸ್ಥಿರೀಕರಿಸಿ",
  },
};

const SUPPORTED_LANGUAGES = ["English", "Hindi", "Hinglish", "Tamil", "Telugu", "Marathi", "Bengali", "Kannada"];

interface DemoChip {
  label: string;
  phrase: string;
}

const DEMO_CHIPS: Record<string, DemoChip[]> = {
  English: [
    { label: "Potato Sale", phrase: "Sold 40 kg potato at 35 rupees per kg" },
    { label: "Tomato Purchase", phrase: "Bought 15 crates of tomatoes at 200 per crate" },
    { label: "Ramesh Loan", phrase: "Lent Ramesh 700 rupees under udhaar" },
    { label: "Fuel Cost", phrase: "Mandi diesel logistics expense was 550" },
    { label: "Mandi Enquiry", phrase: "What is the price of potato today?" },
  ],
  Hindi: [
    { label: "आलू बेचा", phrase: "आज 50 किलो आलू बेचा 30 रुपये किलो भाव से" },
    { label: "टमाटर ख़रीदा", phrase: "दुकान के लिए 10 क्रेट टमाटर खरीदा 150 रुपये प्रति क्रेट" },
    { label: "सुरेश को उधार", phrase: "सुरेश को 1000 रुपये उधार दिए" },
    { label: "दुकान का किराया", phrase: "दुकान का किराया दिया 5000 रुपये" },
    { label: "मंडी भाव प्रश्न", phrase: "आज प्याज का मंडी भाव क्या है?" },
  ],
  Hinglish: [
    { label: "Aloo Sale", phrase: "Aaj 40 kg aloo becha, 30 rupaye kilo" },
    { label: "Ramesh Udhaar", phrase: "Ramesh ko 500 rupaye udhaar diye" },
    { label: "Diesel Kharacha", phrase: "Mandi jane ka diesel ka kharach ₹450" },
    { label: "Tomato Stock", phrase: "Sold 15 crates tomatoes at ₹250 per crate" },
    { label: "Mandi Rates", phrase: "Pyaj ka mandi bhav batao" },
  ],
  Tamil: [
    { label: "உருளைக்கிழங்கு", phrase: "இன்று 40 கிலோ உருளைக்கிழங்கு விற்றேன் கிலோ 30 ரூபாய்" },
    { label: "தக்காளி கொள்முதல்", phrase: "10 பெட்டி தக்காளி வாங்கப்பட்டது பெட்டி ஒன்றுக்கு 200 ரூபாய்" },
    { label: "ரமேஷ் கடன்", phrase: "ரமேஷுக்கு 800 ரூபாய் உதார் கடன் கொடுத்தேன்" },
    { label: "டீசல் வண்டி செலவு", phrase: "மண்டிக்கு செல்ல வண்டி டீசல் செலவு 450 ரூபாய்" },
    { label: "மண்டி விலை", phrase: "தக்காளி இன்றைய மண்டி விலை என்ன?" },
  ],
  Telugu: [
    { label: "బంగాళాదుంప విక్రయం", phrase: "40 కేజీల బంగాళాదుంపలు కేజీ 35 రూపాయల చొప్పున అమ్మాను" },
    { label: "టమోటా కొనుగోలు", phrase: "200 రూపాయల చొప్పున 15 టమోటా క్రేట్లు కొన్నాను" },
    { label: "రమేష్ బాకీ", phrase: "రమేష్‌కు 700 రూపాయలు ఉధార్ ఇచ్చాను" },
    { label: "డీజిల్ ఖర్చు", phrase: "మండి డీజిల్ రవాణా ఖర్చు 550 రూపాయలు" },
    { label: "మండి ధర", phrase: "ఈరోజు బంగాళాదుంపల ధర ఎంత ఉంది?" },
  ],
  Marathi: [
    { label: "बटाटा विक्री", phrase: "३५ रुपये किलो दराने ४० किलो बटाटे विकले" },
    { label: "टोमॅटो खरेदी", phrase: "१५ क्रेट टोमॅटो प्रत्येकी २०० रुपये दराने खरेदी केले" },
    { label: "रमेश उधार", phrase: "रमेशला ७०० रुपये उधार दिले" },
    { label: "इंधन खर्च", phrase: "मंडी वाहतूक डिझेल खर्च ५५० रुपये झाला" },
    { label: "मंडी बाजारभाव", phrase: "आज बटाट्याचा बाजारभाव काय आहे?" },
  ],
  Bengali: [
    { label: "আলু বিক্রি", phrase: "৪০ কেজি আলু ৩৫ টাকা কেজি দরে বিক্রি করলাম" },
    { label: "টমেটো ক্রয়", phrase: "২০০ টাকা প্রতি ক্রেট দরে ১৫ ক্রেট টমেটো কিনলাম" },
    { label: "রমেশ উধার", phrase: "রমেশকে ৭০০ টাকা উধার দিলাম" },
    { label: "ডিজেল খরচ", phrase: "মন্ডিতে যাওয়ার ডিজেল খরচ ৫৫০ টাকা" },
    { label: "মন্ডির দর", phrase: "আজ আলুর মন্ডির দর কত?" },
  ],
  Kannada: [
    { label: "ಆಲೂಗಡ್ಡೆ ಮಾರಾಟ", phrase: "40 ಕೆಜಿ ಆಲೂಗಡ್ಡೆಯನ್ನು ಕೆಜಿಗೆ 35 ರೂಪಾಯಿಯಂತೆ ಮಾರಾಟ ಮಾಡಿದೆ" },
    { label: "ಟೊಮೆಟೊ ಖರೀದಿ", phrase: "ಒಂದು ಕ್ರೇಟ್‌ಗೆ 200 ರಂತೆ 15 ಕ್ರೇಟ್ ಟೊಮೆಟೊ ಖರೀದಿಸಿದೆ" },
    { label: "ರಮೇಶ್ ಬಾಕಿ", phrase: "ರಮೇಶನಿಗೆ 700 ರೂಪಾಯಿ ಉದರಿ ಕೊಟ್ಟಿದ್ದೇನೆ" },
    { label: "ಡೀಸೆಲ್ ವೆಚ್ಚ", phrase: "ಮಂಡಿ ಡೀಸೆಲ್ ಸಾರಿಗೆ ವೆಚ್ಚ 550 ರೂಪಾಯಿ" },
    { label: "ಮಂಡಿ ದರ", phrase: "ಇಂದು ಆಲೂಗಡ್ಡೆ ಮಂಡಿ ದರ ಎಷ್ಟಿದೆ?" },
  ],
};

interface Transaction {
  id: string;
  timestamp: number;
  rawVoiceText: string;
  item: string;
  category: string;
  quantity: number;
  unit: string;
  pricePerUnit: number;
  totalAmount: number;
  partyName: string | null;
  isSynced: boolean;
}

interface InventoryItem {
  itemName: string;
  stockQuantity: number;
  unit: string;
  lastUpdated: number;
  isSynced: boolean;
}

interface MandiPrice {
  cropName: string;
  marketName: string;
  price: number;
  unit: string;
  priceChangeTrend: "UP" | "DOWN" | "STABLE";
  state: string;
  lastUpdated: number;
}

// Initial seeding values
const DEFAULT_INVENTORY: InventoryItem[] = [
  { itemName: "Potato", stockQuantity: 150.0, unit: "kg", lastUpdated: Date.now(), isSynced: true },
  { itemName: "Onion", stockQuantity: 120.0, unit: "kg", lastUpdated: Date.now(), isSynced: true },
  { itemName: "Tomato", stockQuantity: 80.0, unit: "crates", lastUpdated: Date.now(), isSynced: true },
  { itemName: "Wheat", stockQuantity: 210.0, unit: "kg", lastUpdated: Date.now(), isSynced: true },
  { itemName: "Green Chilli", stockQuantity: 50.0, unit: "kg", lastUpdated: Date.now(), isSynced: true },
];

const DEFAULT_MANDI_PRICES: MandiPrice[] = [
  { cropName: "Potato (Aloo)", marketName: "Delhi Azadpur Mandi", price: 26.5, unit: "kg", priceChangeTrend: "STABLE", state: "Delhi", lastUpdated: Date.now() },
  { cropName: "Onion (Pyaj)", marketName: "Mumbai Vashi Mandi", price: 41.0, unit: "kg", priceChangeTrend: "UP", state: "Maharashtra", lastUpdated: Date.now() },
  { cropName: "Tomato (Tamatar)", marketName: "Bangalore Ag Exchange", price: 38.0, unit: "kg", priceChangeTrend: "DOWN", state: "Karnataka", lastUpdated: Date.now() },
  { cropName: "Wheat (Gehun)", marketName: "Indore Mandi", price: 28.0, unit: "kg", priceChangeTrend: "STABLE", state: "Madhya Pradesh", lastUpdated: Date.now() },
  { cropName: "Mustard (Sarson)", marketName: "Jaipur Central Market", price: 59.0, unit: "kg", priceChangeTrend: "UP", state: "Rajasthan", lastUpdated: Date.now() },
  { cropName: "Green Chilli", marketName: "Ahmedabad Market", price: 34.0, unit: "kg", priceChangeTrend: "DOWN", state: "Gujarat", lastUpdated: Date.now() },
];

export default function App() {
  // Theme & Language state
  const [currentLang, setCurrentLang] = useState<string>("English");
  const [wasChangedManually, setWasChangedManually] = useState<boolean>(false);
  const [isMobileModalOpen, setIsMobileModalOpen] = useState<boolean>(false);
  const [hasCopiedUrl, setHasCopiedUrl] = useState<boolean>(false);

  // Position, Currency & Regional dynamics
  const [detectedCountryCode, setDetectedCountryCode] = useState<string>("IN");
  const [detectedCountryName, setDetectedCountryName] = useState<string>("India");
  const [detectedCityName, setDetectedCityName] = useState<string>("Live Terminal Area");
  const [currencySymbol, setCurrencySymbol] = useState<string>("₹");
  const [isLocationDetected, setIsLocationDetected] = useState<boolean>(false);

  // Core Entity Database states
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [inventory, setInventory] = useState<InventoryItem[]>([]);
  const [mandiPrices, setMandiPrices] = useState<MandiPrice[]>([]);

  // System states
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [isSyncing, setIsSyncing] = useState<boolean>(false);
  const [showTechnicalSyncDetails, setShowTechnicalSyncDetails] = useState<boolean>(false);
  const [latestResponse, setLatestResponse] = useState<any | null>(null);
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [categoryFilter, setCategoryFilter] = useState<string>("All");
  const [sortBy, setSortBy] = useState<string>("Newest");
  const [voiceTextInputValue, setVoiceTextInputValue] = useState<string>("");
  const [activeTab, setActiveTab] = useState<number>(0); // 0 = Ledger, 1 = Mandi, 2 = Credit

  // Recording State WebSpeechAPI
  const [isRecordingWebSpeech, setIsRecordingWebSpeech] = useState<boolean>(false);
  const [recognitionError, setRecognitionError] = useState<string | null>(null);
  const [interimTranscripts, setInterimTranscripts] = useState<string>("");

  const recognitionInstance = useRef<any>(null);

  // Load from local storage or apply defaults
  useEffect(() => {
    const savedTransactions = localStorage.getItem("voiceops_transactions");
    if (savedTransactions) {
      setTransactions(JSON.parse(savedTransactions));
    } else {
      // Seed some initial elegant items
      const seedTransactions: Transaction[] = [
        {
          id: "tr-1",
          timestamp: Date.now() - 86400000 * 3,
          rawVoiceText: "Aaj 40 kg aloo beche, 30 rupaye kilo",
          item: "Potato",
          category: "Sale",
          quantity: 40,
          unit: "kg",
          pricePerUnit: 30,
          totalAmount: 1200,
          partyName: null,
          isSynced: true,
        },
        {
          id: "tr-2",
          timestamp: Date.now() - 86400000 * 2,
          rawVoiceText: "Ramesh ko ₹500 udhar diye",
          item: "Cash Debit",
          category: "Udhaar",
          quantity: 1,
          unit: "items",
          pricePerUnit: 500,
          totalAmount: 500,
          partyName: "Ramesh",
          isSynced: true,
        },
        {
          id: "tr-3",
          timestamp: Date.now() - 86400000 * 1,
          rawVoiceText: "Mandi jane ka diesel ka kharach ₹450",
          item: "Diesel",
          category: "Expense",
          quantity: 1,
          unit: "items",
          pricePerUnit: 450,
          totalAmount: 450,
          partyName: null,
          isSynced: false,
        },
      ];
      setTransactions(seedTransactions);
      localStorage.setItem("voiceops_transactions", JSON.stringify(seedTransactions));
    }

    const savedInventory = localStorage.getItem("voiceops_inventory");
    if (savedInventory) {
      setInventory(JSON.parse(savedInventory));
    } else {
      setInventory(DEFAULT_INVENTORY);
      localStorage.setItem("voiceops_inventory", JSON.stringify(DEFAULT_INVENTORY));
    }

    const savedMandi = localStorage.getItem("voiceops_mandi");
    if (savedMandi) {
      setMandiPrices(JSON.parse(savedMandi));
    } else {
      setMandiPrices(DEFAULT_MANDI_PRICES);
      localStorage.setItem("voiceops_mandi", JSON.stringify(DEFAULT_MANDI_PRICES));
    }

    // Try Geo-location
    tryBrowserGeolocation();
  }, []);

  // Sync state helpers to localStorage
  const saveTransactionsState = (updated: Transaction[]) => {
    setTransactions(updated);
    localStorage.setItem("voiceops_transactions", JSON.stringify(updated));
  };

  const saveInventoryState = (updated: InventoryItem[]) => {
    setInventory(updated);
    localStorage.setItem("voiceops_inventory", JSON.stringify(updated));
  };

  const saveMandiState = (updated: MandiPrice[]) => {
    setMandiPrices(updated);
    localStorage.setItem("voiceops_mandi", JSON.stringify(updated));
  };

  // Geo Locator
  const tryBrowserGeolocation = () => {
    if (navigator?.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const lat = position.coords.latitude;
          const lng = position.coords.longitude;
          setIsLocationDetected(true);
          // Indian Coordinates roughly
          if (lat >= 8.0 && lat <= 37.0 && lng >= 68.0 && lng <= 97.0) {
            setDetectedCountryCode("IN");
            setDetectedCountryName("India");
            setDetectedCityName("Delhi NCR");
            setCurrencySymbol("₹");
          } else {
            setDetectedCountryCode("US");
            setDetectedCountryName("United States");
            setDetectedCityName("Browser Client Area");
            setCurrencySymbol("$");
          }
        },
        (error) => {
          console.log("Geolocation permission or resolution error, using country defaults:", error.message);
        }
      );
    }
  };

  // Helper to translate "Language configured/switched to..." in the target language
  const getLanguageSwitchedText = (lang: string) => {
    switch (lang) {
      case "English":
        return "Language set to English";
      case "Hindi":
        return "भाषा अब हिंदी कर दी गई है";
      case "Hinglish":
        return "Language successfully Hinglish set ho gayi hai";
      case "Tamil":
        return "மொழி தமிழுக்கு மாற்றப்பட்டது";
      case "Telugu":
        return "భాష తెలుగులోకి మార్చబడింది";
      case "Marathi":
        return "भाषा मराठीमध्ये बदलण्यात आली आहे";
      case "Bengali":
        return "ভাষা বাংলায় পরিবর্তন করা হয়েছে";
      case "Kannada":
        return "ಭಾಷೆಯನ್ನು ಕನ್ನಡಕ್ಕೆ ಬದಲಾಯಿಸಲಾಗಿದೆ";
      default:
        return `Language configured to ${lang}`;
    }
  };

  // Helper to translate sync success message in the target language
  const getSyncSuccessSpeech = (lang: string) => {
    switch (lang) {
      case "Hindi":
        return "डिजिटल बहीखाता रिकॉर्ड क्लाउड वॉल्ट के साथ सफलतापूर्वक सिंक हो गए हैं।";
      case "Hinglish":
        return "Aapke digital records cloud server par successfully save ho chuke hain.";
      case "Tamil":
        return "டிஜிட்டல் கணக்கு பதிவுகள் கிளவுட் சேமிப்பகத்துடன் வெற்றிகரமாக ஒத்திசைக்கப்பட்டன.";
      case "Telugu":
        return "డిజిటల్ బుక్కీపింగ్ రಿಕಾರ್డులు క్లౌడ్ వాల్ట్‌కి విజయవంతంగా సమకాలీకరించబడ్డాయి.";
      case "Marathi":
        return "डिजिटल बहीखाता डेटा क्लाउडवर यशस्वीरित्या सेव्ह झाला आहे.";
      case "Bengali":
        return "ডিজিটাল খাতা রেকর্ড সফলভাবে ক্লাউডের সাথে সিঙ্ক করা হয়েছে।";
      case "Kannada":
        return "ಡಿಜಿಟಲ್ ಖಾತಾ ದಾಖಲೆಗಳನ್ನು ಕ್ಲೌಡ್ ಸಂಗ್ರಹಣೆಯೊಂದಿಗೆ ಯಶಸ್ವಿಯಾಗಿ ಸಿಂಕ್ ಮಾಡಲಾಗಿದೆ.";
      default:
        return "Digital bookkeeping records are fully synchronized with Cloud Vault storage successfully.";
    }
  };

  // Helper to translate speech recognition not supported in the target language
  const getNotSupportedSpeech = (lang: string) => {
    switch (lang) {
      case "Hindi":
        return "इस सिस्टम में आवाज़ पहचानने की सुविधा उपलब्ध नहीं है। कृपया हमारे डेमो बटन को दबाएं।";
      case "Hinglish":
        return "Is device par voice recognition available nahi hai. Aap demo buttons try karein.";
      case "Tamil":
        return "இந்தச் சாதனத்தில் குரல் அறிதல் ஆதரிக்கப்படவில்லை. மாதிரி பொத்தான்களை முயற்சிக்கவும்.";
      case "Telugu":
        return "ఈ పరికరంలో వాయిస్ గుర్తింపు మద్దతు ఇవ్వబడదు. దయచేసి డెమో బటన్‌లను ప్రయత్నించండి.";
      case "Marathi":
        return "या डिव्हाइसवर व्हॉइस रेकग्निशन उपलब्ध नाही. कृपया सुचवलेले डेमो बटन वापरा.";
      case "Bengali":
        return "এই ডিভাইসে ভয়েস সনাক্তকরণ সমর্থিত নয়। অনুগ্রহ করে ডেমো বোতাম ব্যবহার করুন।";
      case "Kannada":
        return "ಈ ಸಾಧನದಲ್ಲಿ ಧ್ವನಿ ಗುರುತಿಸುವಿಕೆ ಬೆಂಬಲಿತವಾಗಿಲ್ಲ. ಡೆಮೊ ಬಟನ್‌ಗಳನ್ನು ಬಳಸಿ.";
      default:
        return "Speech synthesizer not supported inside this frame. Try clicking our rapid simulation chips!";
    }
  };

  // Text-To-Speech (TTS) spoken output synthesizer
  const speakVoiceFeedback = (sentence: string, overrideLang?: string) => {
    if (!("speechSynthesis" in window)) return;
    window.speechSynthesis.cancel(); // Stop current speech
    
    // Clean up formatting symbols to avoid unnatural robotic pronunciation pauses
    const cleanSentence = sentence
      .replace(/[*#_`\[\]]/g, "")
      .replace(/[\{\}]/g, "")
      .trim();

    const utterance = new SpeechSynthesisUtterance(cleanSentence);
    const langToUse = overrideLang || currentLang;
    
    let ttsLang = "en-IN";
    switch (langToUse) {
      case "Hindi":
      case "Hinglish":
        ttsLang = "hi-IN";
        break;
      case "Tamil":
        ttsLang = "ta-IN";
        break;
      case "Telugu":
        ttsLang = "te-IN";
        break;
      case "Marathi":
        ttsLang = "mr-IN";
        break;
      case "Bengali":
        ttsLang = "bn-IN";
        break;
      case "Kannada":
        ttsLang = "kn-IN";
        break;
      default:
        ttsLang = "en-IN";
        break;
    }
    
    utterance.lang = ttsLang;

    // Set human parameters for a smooth, natural and understandable voice:
    // Slightly slower rate (0.85 to 0.90) is crucial to avoid "high-frequency, rushing, robotic" sound.
    utterance.rate = 0.88;
    utterance.pitch = 1.0; 

    // Find the ideal native localized voice from the user's browser voice synthesis registry
    if ("speechSynthesis" in window) {
      const voices = window.speechSynthesis.getVoices();
      if (voices && voices.length > 0) {
        // 1. Strict match on target language-region locale (e.g. "hi-IN", "kn-IN")
        let matchedVoice = voices.find(v => v.lang.toLowerCase() === ttsLang.toLowerCase());
        
        // 2. Loose match on language prefix (e.g. starts with "hi", "kn", "ta", "te", "mr", "bn")
        if (!matchedVoice) {
          const langPrefix = ttsLang.split("-")[0].toLowerCase();
          matchedVoice = voices.find(v => v.lang.toLowerCase().startsWith(langPrefix));
        }

        // 3. Indian regional voice backup only for English or Hinglish to prevent English voice reading regional scripts
        if (!matchedVoice && (langToUse === "English" || langToUse === "Hinglish")) {
          matchedVoice = voices.find(v => v.lang.toLowerCase().includes("in"));
        }

        if (matchedVoice) {
          utterance.voice = matchedVoice;
          // Synchronize utterance's lang property with the selected voice's actual lang
          utterance.lang = matchedVoice.lang;
        }
      }
    }
    
    // Use a brief timeout to allow speechSynthesis.cancel() to fully clear browser state
    setTimeout(() => {
      window.speechSynthesis.speak(utterance);
    }, 60);
  };

  // Native SpeechRecognition Web API binding
  const toggleRecording = () => {
    if (isRecordingWebSpeech) {
      if (recognitionInstance.current) {
        recognitionInstance.current.stop();
      }
      setIsRecordingWebSpeech(false);
      return;
    }

    setRecognitionError(null);
    setInterimTranscripts("");

    // Look for browser speech engines
    const SpeechRecognitionAPI =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognitionAPI) {
      const errMsg = "HTML5 Speech recognition not supported in this browser. Try copying chip examples below!";
      setRecognitionError(errMsg);
      speakVoiceFeedback(getNotSupportedSpeech(currentLang));
      return;
    }

    try {
      const recognition = new SpeechRecognitionAPI();
      recognition.continuous = false;
      recognition.interimResults = true;

      // Select matching lang code
      switch (currentLang) {
        case "Hindi":
        case "Hinglish":
          recognition.lang = "hi-IN";
          break;
        case "Tamil":
          recognition.lang = "ta-IN";
          break;
        case "Telugu":
          recognition.lang = "te-IN";
          break;
        case "Marathi":
          recognition.lang = "mr-IN";
          break;
        case "Bengali":
          recognition.lang = "bn-IN";
          break;
        case "Kannada":
          recognition.lang = "kn-IN";
          break;
        default:
          recognition.lang = "en-IN";
          break;
      }

      recognition.onstart = () => {
        setIsRecordingWebSpeech(true);
      };

      recognition.onresult = (event: any) => {
        let interim = "";
        let finalTrans = "";
        for (let i = event.resultIndex; i < event.results.length; ++i) {
          if (event.results[i].isFinal) {
            finalTrans += event.results[i][0].transcript;
          } else {
            interim += event.results[i][0].transcript;
          }
        }
        if (interim) {
          setInterimTranscripts(interim);
        }
        if (finalTrans) {
          setVoiceTextInputValue(finalTrans);
          submitVoiceToParser(finalTrans);
          recognition.stop();
        }
      };

      recognition.onerror = (event: any) => {
        console.error("Speech Recognition Error Event:", event);
        if (event.error === "not-allowed") {
          setRecognitionError("Microphone permission denied. Enable microphone access in browser or type payloads manually!");
        } else {
          setRecognitionError(`Recognition halted: ${event.error}`);
        }
        setIsRecordingWebSpeech(false);
      };

      recognition.onend = () => {
        setIsRecordingWebSpeech(false);
      };

      recognitionInstance.current = recognition;
      recognition.start();
    } catch (e: any) {
      setRecognitionError(e.message || "Initialization failed.");
      setIsRecordingWebSpeech(false);
    }
  };

  // Send transcription to backend secure Gemini proxy parser
  const submitVoiceToParser = async (payload: string) => {
    if (!payload || payload.trim() === "") return;
    setIsProcessing(true);
    try {
      const res = await fetch("/api/gemini/parse", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text: payload, language: currentLang }),
      });

      if (!res.ok) {
        throw new Error(`Server returned HTTP bad request code ${res.status}`);
      }

      const aiResponse = await res.json();
      setLatestResponse(aiResponse);
      setVoiceTextInputValue("");
      setInterimTranscripts("");

      // Trigger automatic Speech playback of AI feedback explanation
      if (aiResponse.explanation) {
        speakVoiceFeedback(aiResponse.explanation);
      }
    } catch (e: any) {
      console.error("Error submitting payload to voice parser:", e);
      // Generate highly intelligent offline fallback on browser side
      const localFallback = simulateLocalOfflineFallback(payload);
      setLatestResponse(localFallback);
      setVoiceTextInputValue("");
      setInterimTranscripts("");
      if (localFallback.explanation) {
        speakVoiceFeedback(localFallback.explanation);
      }
    } finally {
      setIsProcessing(false);
    }
  };

  // Confirms the parsed result, updates inventory and transactions and database tables
  const commitParsedResponse = () => {
    if (!latestResponse) return;

    if (latestResponse.type === "transaction") {
      const newTransaction: Transaction = {
        id: `tr-${Date.now()}`,
        timestamp: Date.now(),
        rawVoiceText: voiceTextInputValue || latestResponse.explanation || "Manual entry",
        item: latestResponse.item || "General Inventory",
        category: latestResponse.category || "Sale",
        quantity: latestResponse.quantity || 1,
        unit: latestResponse.unit || "items",
        pricePerUnit: latestResponse.pricePerUnit || 0,
        totalAmount: latestResponse.totalAmount || (latestResponse.quantity * latestResponse.pricePerUnit) || 0,
        partyName: latestResponse.partyName || null,
        isSynced: true,
      };

      // Push to Transactions Feed
      const updatedTrans = [newTransaction, ...transactions];
      saveTransactionsState(updatedTrans);

      // Track stock impact
      const categoryUpper = newTransaction.category.trim().toUpperCase();
      const isSalesImpact = categoryUpper === "SALE";
      const isPurchaseImpact = categoryUpper === "PURCHASE" || categoryUpper === "STOCK IN";

      if (isSalesImpact || isPurchaseImpact) {
        const factor = isSalesImpact ? -1 : 1;
        const targetCrop = newTransaction.item.trim().replace(/^\w/, (c) => c.toUpperCase());

        let matched = false;
        const nextInventory = inventory.map((inv) => {
          if (inv.itemName.toLowerCase() === targetCrop.toLowerCase()) {
            matched = true;
            return {
              ...inv,
              stockQuantity: Math.max(0, inv.stockQuantity + factor * newTransaction.quantity),
              lastUpdated: Date.now(),
            };
          }
          return inv;
        });

        if (!matched && newTransaction.quantity > 0) {
          nextInventory.push({
            itemName: targetCrop,
            stockQuantity: newTransaction.quantity,
            unit: newTransaction.unit,
            lastUpdated: Date.now(),
            isSynced: true,
          });
        }
        saveInventoryState(nextInventory);
      }
    } else if (latestResponse.type === "query" && latestResponse.isMandiQuery && latestResponse.queryCrop) {
      // Append crop rates into the local mandi prices
      const matchingCrop = latestResponse.queryCrop;
      let present = false;
      const nextMandi = mandiPrices.map((m) => {
        if (m.cropName.toLowerCase().includes(matchingCrop.toLowerCase())) {
          present = true;
          return {
            ...m,
            price: latestResponse.pricePerUnit || m.price || 28,
            lastUpdated: Date.now(),
          };
        }
        return m;
      });

      if (!present) {
        nextMandi.push({
          cropName: matchingCrop,
          marketName: "Direct AI Feed",
          price: latestResponse.pricePerUnit || 32,
          unit: "kg",
          priceChangeTrend: "UP",
          state: "Live Zone",
          lastUpdated: Date.now(),
        });
      }
      saveMandiState(nextMandi);
    }

    setLatestResponse(null);
  };

  // Syntactic Local Fallback simulation if network/key behaves stubby
  const simulateLocalOfflineFallback = (text: string): any => {
    const raw = text.toLowerCase().trim();
    const qtyMatch = raw.match(/(\d+(?:\.\d+)?)\s*(kg|kilogram|crate|packet|pack|bunch|liter|litre|items|item)/);
    const priceMatch = raw.match(/(?:rs\.?|₹|rupees?|at|@)\s*(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:rs\.?|rupees?)/);
    
    const quantity = qtyMatch ? parseFloat(qtyMatch[1]) : 1.0;
    const price = priceMatch ? parseFloat(priceMatch[1] || priceMatch[2]) : 0;
    const amountCalculated = quantity * price;

    let inferredItem = "General Crop";
    if (raw.includes("potato") || raw.includes("aloo")) inferredItem = "Potato";
    else if (raw.includes("onion") || raw.includes("pyaj") || raw.includes("pyaaj")) inferredItem = "Onion";
    else if (raw.includes("tomato") || raw.includes("tamatar")) inferredItem = "Tomato";
    else if (raw.includes("wheat") || raw.includes("gehun")) inferredItem = "Wheat";
    else if (raw.includes("chilli") || raw.includes("mirchi")) inferredItem = "Green Chilli";
    else if (raw.includes("diesel") || raw.includes("fuel")) inferredItem = "Diesel";

    let unit = "items";
    if (raw.includes("kg") || raw.includes("kilo") || raw.includes("kilogram")) unit = "kg";
    else if (raw.includes("crate")) unit = "crates";
    else if (raw.includes("packet") || raw.includes("pack")) unit = "packet";
    else if (raw.includes("liter") || raw.includes("litre")) unit = "liter";

    let inferredCategory = "Sale";
    if (raw.includes("bought") || raw.includes("buy") || raw.includes("kharid") || raw.includes("purchase")) {
      inferredCategory = "Purchase";
    } else if (raw.includes("udhaar") || raw.includes("udhar") || raw.includes("lent") || raw.includes("gave")) {
      inferredCategory = "Udhaar";
    } else if (raw.includes("expense") || raw.includes("diesel") || raw.includes("kiraya") || raw.includes("rent")) {
      inferredCategory = "Expense";
    }

    const partyName = raw.includes("ramesh") ? "Ramesh" : raw.includes("suresh") ? "Suresh" : null;

    // Check Mandi prices queries
    const isMandiQuery = raw.includes("bhav") || raw.includes("price") || raw.includes("rate") || raw.includes("bhaav") || raw.includes("mandi");

    if (isMandiQuery) {
      const ans = `Offline AI Feed: Today ${inferredItem} mandi rate in nearest regional exchange is ${price > 0 ? price : "22-30"} Rs per kg.`;
      return {
        type: "query",
        isMandiQuery: true,
        queryCrop: inferredItem,
        queryAnswer: ans,
        explanation: ans,
        pricePerUnit: price || 25,
      };
    }

    const actionText = inferredCategory === "Purchase" ? "Recorded Purchase" : inferredCategory === "Udhaar" ? "Udhaar Accounted" : inferredCategory === "Expense" ? "Expense Accounted" : "Sale Logged";
    const detailString = price > 0 
      ? `Captured ${actionText}: ${inferredItem} ${quantity} ${unit} @ ${currencySymbol}${price}/${unit}. Total worth is ${currencySymbol}${amountCalculated}.`
      : `Captured ${actionText}: ${inferredItem} of ${quantity} ${unit}. Note: Unit price was unspecified.`;

    return {
      type: "transaction",
      item: inferredItem,
      category: inferredCategory,
      quantity,
      unit,
      pricePerUnit: price,
      totalAmount: amountCalculated,
      partyName,
      explanation: `${detailString} Saving transaction ledger.`,
    };
  };

  const deleteTransaction = (id: string) => {
    const updated = transactions.filter((t) => t.id !== id);
    saveTransactionsState(updated);
  };

  const clearAllData = () => {
    if (confirm("Are you sure you want to clear your local ledger timeline?")) {
      saveTransactionsState([]);
      saveInventoryState(DEFAULT_INVENTORY);
    }
  };

  const seedSampleTransactions = () => {
    const samples: Transaction[] = [
      {
        id: "sample-1",
        timestamp: Date.now() - 86400000 * 4,
        rawVoiceText: "Sold 30 kg onion to Ramesh at 40 Rs",
        item: "Onion",
        category: "Sale",
        quantity: 30,
        unit: "kg",
        pricePerUnit: 40,
        totalAmount: 1200,
        partyName: "Ramesh",
        isSynced: true,
      },
      {
        id: "sample-2",
        timestamp: Date.now() - 86400000 * 3,
        rawVoiceText: "Bought 10 crates rich tomatoes at ₹250 per crate",
        item: "Tomato",
        category: "Purchase",
        quantity: 10,
        unit: "crates",
        pricePerUnit: 250,
        totalAmount: 2500,
        partyName: null,
        isSynced: true,
      },
      {
        id: "sample-3",
        timestamp: Date.now() - 86400000 * 2,
        rawVoiceText: "Suresh ko Rs 1000 cash loan udhar diya",
        item: "Finances",
        category: "Udhaar",
        quantity: 1,
        unit: "items",
        pricePerUnit: 1000,
        totalAmount: 1000,
        partyName: "Suresh",
        isSynced: false,
      },
      {
        id: "sample-4",
        timestamp: Date.now() - 3600000 * 5,
        rawVoiceText: "Fertilizer purchase worth 1500 rupees",
        item: "Fertilizer",
        category: "Expense",
        quantity: 1,
        unit: "bags",
        pricePerUnit: 1500,
        totalAmount: 1500,
        partyName: null,
        isSynced: false,
      },
    ];
    saveTransactionsState([...transactions, ...samples]);
  };

  // Uplink sync simulation
  const syncOfflineTransactions = () => {
    setIsSyncing(true);
    setTimeout(() => {
      const synced = transactions.map((t) => ({ ...t, isSynced: true }));
      saveTransactionsState(synced);
      setIsSyncing(false);
      speakVoiceFeedback(getSyncSuccessSpeech(currentLang));
    }, 1500);
  };

  // Translate wrapper
  const translate = (key: keyof TranslationSet): string => {
    const set = LOCALIZATION[currentLang] || LOCALIZATION["English"];
    return set[key] || LOCALIZATION["English"][key];
  };

  // Calculate high-level business stats
  const totalSalesVal = transactions.filter((t) => t.category === "Sale").reduce((sum, t) => sum + t.totalAmount, 0);
  const totalUdhaarVal = transactions.filter((t) => t.category === "Udhaar").reduce((sum, t) => sum + t.totalAmount, 0);
  const totalExpensesVal = transactions.filter((t) => t.category === "Expense" || t.category === "Purchase").reduce((sum, t) => sum + t.totalAmount, 0);

  // Business Health Score Formulation
  const calculateBusinessHealth = (): number => {
    if (transactions.length === 0) return 50;
    const countSales = transactions.filter((t) => t.category === "Sale").length;
    const countUdhaar = transactions.filter((t) => t.category === "Udhaar").length;
    const stockOuts = inventory.filter((i) => i.stockQuantity < 20).length;

    let score = 65;
    score += Math.min(20, countSales * 4);
    score -= Math.max(-15, countUdhaar * 3);
    score -= stockOuts * 6;
    return Math.max(10, Math.min(100, score));
  };

  const healthScore = calculateBusinessHealth();

  // Dynamic Credit Index Score Formulation (similar to CIBIL / Experian, but generated from voice accounting logs)
  const calculateCreditIndex = (): number => {
    if (transactions.length === 0) return 480;
    const logVolumeBonus = Math.min(250, (totalSalesVal / 80));
    const consistenciesBonus = Math.min(120, transactions.length * 15);
    const udhaarDeduction = Math.min(80, (totalUdhaarVal / 50));
    return Math.round(Math.min(850, Math.max(300, 500 + logVolumeBonus + consistenciesBonus - udhaarDeduction)));
  };

  const creditScore = calculateCreditIndex();

  // Filter & Sorted Ledger Timeline
  const getFilteredTransactions = (): Transaction[] => {
    let result = [...transactions];

    if (searchQuery.trim() !== "") {
      const q = searchQuery.toLowerCase();
      result = result.filter(
        (t) =>
          t.item.toLowerCase().includes(q) ||
          (t.partyName && t.partyName.toLowerCase().includes(q)) ||
          t.rawVoiceText.toLowerCase().includes(q)
      );
    }

    if (categoryFilter !== "All") {
      result = result.filter((t) => t.category === categoryFilter);
    }

    if (sortBy === "Newest") {
      result.sort((a, b) => b.timestamp - a.timestamp);
    } else if (sortBy === "Oldest") {
      result.sort((a, b) => a.timestamp - b.timestamp);
    } else if (sortBy === "Highest Val") {
      result.sort((a, b) => b.totalAmount - a.totalAmount);
    }

    return result;
  };

  const filteredTrans = getFilteredTransactions();

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col items-center justify-center font-sans transition-all duration-300 antialiased overflow-x-hidden selection:bg-cyan-500 selection:text-black p-0 md:p-3 relative">
      
      {/* Decorative neon background grid glows */}
      <div className="absolute top-0 left-0 w-full h-full overflow-hidden pointer-events-none z-0">
        <div className="absolute top-[10%] left-[-20%] w-[60vw] h-[60vw] rounded-full bg-cyan-500/10 blur-[120px]" />
        <div className="absolute bottom-[20%] right-[-10%] w-[50vw] h-[50vw] rounded-full bg-indigo-500/10 blur-[130px]" />
        <div className="absolute top-[40%] left-[30%] w-[45vw] h-[45vw] rounded-full bg-emerald-500/5 blur-[100px]" />
      </div>

      {/* Phone chassis frame for desktops, regular mobile screen behaviour on real phones */}
      <div className="z-10 w-full max-w-md bg-slate-950 md:border-[10px] md:border-slate-900 md:rounded-[48px] md:shadow-[0_0_100px_rgba(0,0,0,0.9)] md:shadow-cyan-950/20 flex-1 md:flex-initial md:h-[860px] flex flex-col overflow-hidden relative md:my-2">
        
        {/* Virtual smartphone top notch and status bar */}
        <div className="hidden md:flex justify-between items-center px-6 pt-5 pb-3 shrink-0 bg-slate-950 z-20 text-slate-500 font-mono text-[9px] tracking-wide select-none">
          <span>09:41</span>
          <div className="w-20 h-4.5 bg-black rounded-full border border-white/5 flex items-center justify-center">
            <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 mr-1.5 animate-pulse" />
            <span className="text-[7.5px] font-black tracking-widest text-cyan-400">VOICEOPS</span>
          </div>
          <div className="flex items-center gap-1">
            <span>5G</span>
            <div className="w-4.5 h-2.5 border border-slate-700 rounded-xs p-0.5 flex">
              <div className="w-2.5 h-full bg-cyan-400 rounded-3xs" />
            </div>
          </div>
        </div>

        {/* Swipeable Inner Screen Viewport scroll-chamber container */}
        <div className="flex-1 overflow-y-auto p-4 flex flex-col relative w-full">
        
          {/* Navigation / Header Bar */}
          <header className="flex flex-col gap-3.5 pb-4 border-b border-white/5">
            
            <div className="flex items-center gap-3">
              <div className="bg-slate-900 border border-white/10 rounded-xl p-2.5 flex items-center justify-center shrink-0">
                <Mic className="h-6 w-6 text-cyan-400 animate-pulse" />
              </div>
              <div className="min-w-0">
                <h1 id="app-title" className="text-xl font-extrabold tracking-tight bg-gradient-to-r from-cyan-400 via-sky-300 to-indigo-200 bg-clip-text text-transparent truncate">
                  {translate("APP_NAME")}
                </h1>
                <p className="text-[10px] text-slate-400 font-medium truncate">{translate("SUBTITLE")}</p>
              </div>
            </div>

            {/* Quick Stats & Regional Localization Details */}
            <div className="flex flex-col gap-2.5 w-full mt-1.5">
              
              <div className="flex items-center justify-between w-full">
                {/* Status indicator */}
                <div className="flex items-center gap-1.5 bg-gradient-to-r from-emerald-500/10 to-teal-500/10 border border-emerald-500/30 px-3 py-1 rounded-full text-emerald-400 text-[9px] font-black uppercase tracking-wider">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-ping" />
                  {translate("AI_ACTIVE")}
                </div>

                {/* Region banner indicator */}
                <p className="text-[9px] font-extrabold text-slate-500 uppercase tracking-widest">
                  India Region Active
                </p>
              </div>

              {/* Language Selector Horizontally Scrollable Tray */}
              <div className="flex items-center bg-slate-900/90 border border-white/5 rounded-2xl p-1.5 shadow-lg w-full overflow-hidden shrink-0">
                <span className="text-[10px] text-slate-400 font-extrabold uppercase tracking-wider px-2 shrink-0">🌐 Language:</span>
                <div className="flex gap-1.5 overflow-x-auto whitespace-nowrap pb-2 pt-1 px-1 w-full">
                  {SUPPORTED_LANGUAGES.map((lang) => (
                    <button
                      key={lang}
                      onClick={() => {
                        setCurrentLang(lang);
                        setWasChangedManually(true);
                        speakVoiceFeedback(getLanguageSwitchedText(lang), lang);
                      }}
                      className={`px-3 py-1 text-[11px] font-extrabold rounded-lg transition-all duration-200 shrink-0 cursor-pointer min-h-[32px] flex items-center justify-center ${
                        currentLang === lang
                          ? "bg-cyan-500 text-slate-950 font-black shadow-md shadow-cyan-400/25 scale-[1.03]"
                          : "text-slate-400 hover:text-slate-100 hover:bg-white/5"
                      }`}
                    >
                      {lang}
                    </button>
                  ))}
                </div>
              </div>

            </div>
          </header>

        {/* Location Banner Alert */}
        <div className="mt-4 shrink-0">
          {isLocationDetected ? (
            <div className="flex items-center justify-between gap-2 bg-emerald-500/10 border border-emerald-500/20 rounded-xl px-4 py-2.5 text-xs text-emerald-300 shadow-md">
              <div className="flex items-center gap-2.5">
                <MapPin className="h-4.5 w-4.5 text-emerald-400 shrink-0" />
                <span className="font-semibold">
                  Geolocated Live Currency Profile: {detectedCityName}, {detectedCountryName} ({currencySymbol})
                </span>
              </div>
              <button
                onClick={tryBrowserGeolocation}
                className="text-[10px] uppercase font-bold text-emerald-400 hover:underline bg-white/5 px-2.5 py-1 rounded-md"
              >
                Re-scan 🛰️
              </button>
            </div>
          ) : (
            <div className="flex items-center justify-between gap-2 bg-cyan-500/10 border border-cyan-500/20 rounded-xl px-4 py-2.5 text-xs text-cyan-300 shadow-md">
              <div className="flex items-center gap-2.5">
                <MapPin className="h-4.5 w-4.5 text-cyan-400 shrink-0" />
                <span className="font-semibold">
                  Default Regional Assumption: India ({currencySymbol}). Keep browser location services active to adjust automatically.
                </span>
              </div>
              <button
                onClick={tryBrowserGeolocation}
                className="text-[10px] uppercase font-bold text-cyan-400 hover:underline bg-cyan-500/10 px-2.5 py-1 rounded-md"
              >
                Enable Geolocation 🛰️
              </button>
            </div>
          )}
        </div>

        {/* Quick Core Financial Stats row */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-6 shrink-0">
          
          <div className="relative overflow-hidden bg-slate-900/60 border border-white/5 rounded-2xl p-4.5 shadow-xl hover:border-cyan-500/30 transition-all group">
            <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-cyan-500/5 to-transparent rounded-full blur-lg" />
            <div className="flex justify-between items-start">
              <div>
                <p className="text-xs font-bold text-slate-400 uppercase tracking-widest">{translate("TOTAL_SALES")}</p>
                <h3 className="text-2xl font-black text-white mt-1 heading-value">
                  {currencySymbol}{totalSalesVal.toLocaleString(undefined, { minimumFractionDigits: 0 })}
                </h3>
              </div>
              <div className="p-2 rounded-xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400">
                <ArrowUpRight className="h-5 w-5" />
              </div>
            </div>
            <div className="mt-3 flex items-center gap-1.5 text-[11px] text-emerald-400 font-semibold">
              <TrendingUp className="h-3.5 w-3.5" />
              <span>+18% from last week</span>
            </div>
          </div>

          <div className="relative overflow-hidden bg-slate-900/60 border border-white/5 rounded-2xl p-4.5 shadow-xl hover:border-rose-500/30 transition-all group">
            <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-rose-500/5 to-transparent rounded-full blur-lg" />
            <div className="flex justify-between items-start">
              <div>
                <p className="text-xs font-bold text-slate-400 uppercase tracking-widest">{translate("UDHAAR_DUE")}</p>
                <h3 className="text-2xl font-black text-rose-400 mt-1 heading-value">
                  {currencySymbol}{totalUdhaarVal.toLocaleString(undefined, { minimumFractionDigits: 0 })}
                </h3>
              </div>
              <div className="p-2 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400">
                <Coins className="h-5 w-5" />
              </div>
            </div>
            <div className="mt-3 flex items-center gap-1.5 text-[11px] text-rose-400 font-semibold">
              <TrendingDown className="h-3.5 w-3.5" />
              <span>Outstanding credit books</span>
            </div>
          </div>

          <div className="relative overflow-hidden bg-slate-900/60 border border-white/5 rounded-2xl p-4.5 shadow-xl hover:border-amber-500/30 transition-all group">
            <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-amber-500/5 to-transparent rounded-full blur-lg" />
            <div className="flex justify-between items-start">
              <div>
                <p className="text-xs font-bold text-slate-400 uppercase tracking-widest">{translate("EXPENSES")}</p>
                <h3 className="text-2xl font-black text-amber-400 mt-1 heading-value">
                  {currencySymbol}{totalExpensesVal.toLocaleString(undefined, { minimumFractionDigits: 0 })}
                </h3>
              </div>
              <div className="p-2 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
                <ArrowDownLeft className="h-5 w-5" />
              </div>
            </div>
            <div className="mt-3 flex items-center gap-1.5 text-[11px] text-amber-400 font-semibold">
              <TrendingUp className="h-3.5 w-3.5" />
              <span>Includes stock procurement</span>
            </div>
          </div>

        </div>

        {/* Business Health Meter Box Panel */}
        <div className="mt-4 bg-gradient-to-br from-slate-900 to-slate-950/70 border border-white/5 rounded-2xl p-4 shadow-lg flex flex-col md:flex-row justify-between items-start md:items-center gap-4 shrink-0">
          <div className="flex-1">
            <div className="flex items-center gap-2">
              <h4 className="text-sm font-bold text-slate-200">Voice-Powered Business Health Index</h4>
              <span className="text-[10px] font-bold uppercase bg-cyan-400/15 text-cyan-400 px-2 py-0.5 rounded-md">Live Assessment</span>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Constructed dynamically based on consistency of logs, outstanding receivables due (Udhaar), inventory replenishment rates.
            </p>
            <div className="w-full bg-slate-800/80 rounded-full h-2.5 mt-3 border border-white/5 overflow-hidden">
              <div
                className={`h-full rounded-full transition-all duration-1000 ${
                  healthScore >= 75 ? "bg-gradient-to-r from-emerald-500 to-cyan-400" : healthScore >= 50 ? "bg-amber-400" : "bg-rose-500"
                }`}
                style={{ width: `${healthScore}%` }}
              />
            </div>
          </div>
          <div className="flex items-center gap-4 self-end md:self-auto">
            <div className="text-right">
              <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Health score</span>
              <span className="text-2xl font-black text-white">{healthScore} / 100</span>
            </div>
            <div className="bg-slate-800 border border-white/5 px-3 py-1.5 rounded-xl text-center">
              <span className="text-[9px] text-slate-400 font-bold uppercase block">Procure status</span>
              <span className={`text-[11px] font-bold ${inventory.some(i => i.stockQuantity < 20) ? "text-amber-400" : "text-emerald-400"}`}>
                {inventory.some(i => i.stockQuantity < 20) ? "Restock Needed" : "Stock Stable"}
              </span>
            </div>
          </div>
        </div>

        {/* Dynamic Voice Recording & Interactive Simulator Hub */}
        <div className="mt-6 bg-slate-900 border border-white/10 rounded-3xl p-5 md:p-6 shadow-2xl relative overflow-hidden shrink-0">
          <div className="absolute top-0 right-0 w-32 h-32 bg-cyan-400/5 rounded-full blur-2xl" />
          
          <div className="flex flex-col items-center">
            
            <p className="text-slate-400 text-xs md:text-sm font-bold tracking-wider text-center uppercase bg-white/5 border border-white/10 px-4 py-2.5 rounded-2xl min-h-[44px] flex items-center justify-center">
              {translate("MIC_TAP_PROMPT")}
            </p>

            {/* Glowing Microphone button */}
            <div className="mt-6 relative flex flex-col items-center shrink-0">
              
              <button
                id="voice-ops-mic-button"
                onClick={toggleRecording}
                className={`h-24 w-24 md:h-28 md:w-28 rounded-full flex flex-col items-center justify-center transition-all duration-300 relative z-10 select-none shrink-0 cursor-pointer ${
                  isRecordingWebSpeech
                    ? "bg-gradient-to-tr from-rose-500 to-rose-600 shadow-lg shadow-rose-500/40 border border-rose-400/30 scale-105"
                    : "bg-slate-950 hover:bg-slate-900 hover:scale-105 border border-cyan-500/40 shadow-xl shadow-cyan-950/40"
                }`}
              >
                {isRecordingWebSpeech ? (
                  <MicOff className="h-9 w-9 text-white animate-pulse" />
                ) : (
                  <Mic className="h-9 w-9 text-cyan-400 group-hover:text-cyan-300" />
                )}
                <span className="text-[10px] font-black tracking-widest mt-2 uppercase text-slate-400">
                  {isRecordingWebSpeech ? "RECORDING" : "TAP MIC"}
                </span>
                
                {/* Visual pulse rings when active */}
                {isRecordingWebSpeech && (
                  <>
                    <div className="absolute top-0 left-0 w-full h-full rounded-full bg-rose-500/20 -z-10 animate-ping" />
                    <div className="absolute -inset-1 rounded-full border border-rose-500/40 -z-10 animate-pulse" />
                  </>
                )}
              </button>
            </div>

            {/* Voice streaming visual waveform text */}
            {isRecordingWebSpeech && (
              <div className="mt-4 flex flex-col items-center gap-1">
                <span className="text-xxs font-black text-rose-400 tracking-wider animate-pulse uppercase">Audible signal detected:</span>
                <p className="text-sm font-semibold text-rose-200 bg-white/5 border border-white/5 px-4 py-2 rounded-xl italic">
                  "{interimTranscripts || "Listening carefully..."}"
                </p>
              </div>
            )}

            {/* Parsing loading state placeholder */}
            {isProcessing && (
              <div className="mt-4 flex items-center gap-2 text-cyan-400 text-xs font-bold uppercase tracking-wider animate-pulse bg-cyan-500/5 px-4 py-2 rounded-xl border border-cyan-500/10">
                <RefreshCw className="h-4 w-4 animate-spin text-cyan-400" />
                <span>{translate("TRANSLATING")}</span>
              </div>
            )}

            {/* Micro Error Alerts */}
            {recognitionError && (
              <div className="mt-4 bg-rose-500/10 border border-rose-500/20 px-4 py-2 rounded-xl text-xs text-rose-400 text-center font-medium max-w-lg">
                ⚠️ {recognitionError}
              </div>
            )}

            <div className="w-full max-w-xl mt-4">
              <label htmlFor="manual-voice-input" className="sr-only">Or Type Spoken Query manually</label>
              <div className="relative">
                <input
                  id="manual-voice-input"
                  type="text"
                  placeholder="Or mock speech typing here... (e.g. 'Sold 40 kg potato at 30 Rs')"
                  value={voiceTextInputValue}
                  onChange={(e) => setVoiceTextInputValue(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      submitVoiceToParser(voiceTextInputValue);
                    }
                  }}
                  className="w-full bg-slate-950 border border-white/10 rounded-2xl pl-4 pr-12 py-3.5 text-xs text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500/50 shadow-inner transition-all"
                />
                <button
                  onClick={() => submitVoiceToParser(voiceTextInputValue)}
                  className="absolute right-2.5 top-1.5 p-2 bg-cyan-500 hover:bg-cyan-400 text-slate-950 rounded-xl transition duration-150"
                  aria-label="Submit Manual Query"
                >
                  <ChevronRight className="h-4.5 w-4.5 font-bold" />
                </button>
              </div>
            </div>



          </div>
        </div>

        {/* Real-time Parsed AI structural JSON Overlay Modal */}
        {latestResponse && (
          <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fade-in">
            <div className="bg-slate-900/95 border border-cyan-500/30 shadow-2xl shadow-cyan-950/40 rounded-3xl p-5 md:p-6 max-w-lg w-full relative z-10 transition-all max-h-[90vh] overflow-y-auto">
              
              <div className="flex items-center justify-between border-b border-white/5 pb-4">
                <div className="flex items-center gap-2 text-cyan-400">
                  <Database className="h-5 w-5" />
                  <h3 className="font-extrabold text-sm uppercase tracking-wider">{translate("VOICE_TRANSLATION_TITLE")}</h3>
                </div>
                <button
                  onClick={() => setLatestResponse(null)}
                  className="p-1 text-slate-400 hover:text-slate-100 bg-white/5 rounded-lg"
                >
                  ✕
                </button>
              </div>

              {/* Backtalk Speech Output representation banner */}
              <div className="mt-4 bg-cyan-500/10 border border-cyan-500/20 rounded-2xl p-4.5 shadow-md">
                <span className="text-[10px] font-black text-cyan-400 tracking-wider uppercase block">
                  📢 {translate("EXPLAINED_OUT_LOUD")}
                </span>
                <p className="text-xs md:text-sm text-slate-100 font-bold mt-1.5 italic">
                  "{latestResponse.explanation || "Processed successfully."}"
                </p>
              </div>

              {/* Ledger structure cards summary */}
              {latestResponse.type === "transaction" ? (
                <div className="mt-5 space-y-3.5 bg-slate-950/60 p-4 rounded-2xl border border-white/5">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block">{translate("ITEM")}</span>
                      <span className="text-sm font-extrabold text-white mt-1 block h-5 truncate">
                        {latestResponse.item || "General Inventory"}
                      </span>
                    </div>
                    <div>
                      <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block">{translate("CAT")}</span>
                      <span className={`text-sm font-extrabold mt-1 block h-5 ${latestResponse.category === "Udhaar" ? "text-rose-400" : "text-emerald-400"}`}>
                        {latestResponse.category || "Sale"}
                      </span>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4 border-t border-white/5 pt-3">
                    <div>
                      <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block">{translate("QTY")}</span>
                      <span className="text-sm font-bold text-white mt-1 block">
                        {latestResponse.quantity || 1} {latestResponse.unit || "items"}
                      </span>
                    </div>
                    <div>
                      <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block">{translate("RATE")}</span>
                      <span className="text-sm font-bold text-white mt-1 block">
                        {currencySymbol}{latestResponse.pricePerUnit?.toLocaleString() || "0"}
                      </span>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4 border-t border-white/5 pt-3">
                    <div>
                      <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block">{translate("TOTAL")}</span>
                      <span className="text-sm font-black text-cyan-400 mt-1 block">
                        {currencySymbol}{(latestResponse.totalAmount || latestResponse.quantity * latestResponse.pricePerUnit || 0).toLocaleString()}
                      </span>
                    </div>
                    {latestResponse.partyName && (
                      <div>
                        <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block">{translate("DEBT")}</span>
                        <span className="text-sm font-bold text-amber-400 mt-1 block truncate">
                          {latestResponse.partyName}
                        </span>
                      </div>
                    )}
                  </div>
                </div>
              ) : latestResponse.type === "query" && latestResponse.queryAnswer ? (
                <div className="mt-5 bg-slate-950/60 p-4.5 rounded-2xl border border-white/5 space-y-2 text-center">
                  <span className="text-[10px] font-black text-emerald-400 uppercase tracking-wider block">Mandi Rate Information query</span>
                  <p className="text-xs text-slate-300 font-semibold">{latestResponse.queryAnswer}</p>
                </div>
              ) : (
                <div className="mt-5 bg-slate-950/60 p-4.5 rounded-2xl border border-white/5 space-y-2 text-center">
                  <span className="text-[10px] font-black text-slate-400 uppercase tracking-wider block">AI Conversation response</span>
                  <p className="text-xs text-slate-300 font-semibold">{latestResponse.queryAnswer || "Processing command successfully Completed."}</p>
                </div>
              )}

              <div className="mt-6 flex gap-3.5">
                <button
                  onClick={() => setLatestResponse(null)}
                  className="flex-1 py-3 bg-slate-950 hover:bg-slate-900 border border-white/10 rounded-xl text-xs text-slate-400 hover:text-slate-200 transition font-bold"
                >
                  Discard
                </button>
                <button
                  onClick={commitParsedResponse}
                  className="flex-1 py-3 bg-cyan-500 hover:bg-cyan-400 text-slate-950 rounded-xl text-xs font-black shadow-lg shadow-cyan-400/10 transition"
                >
                  {translate("DONE_BUTTON")}
                </button>
              </div>

            </div>
          </div>
        )}



        {/* Tab switcher Navigation Slider */}
        <div className="mt-8 flex bg-slate-900/60 p-1.5 rounded-2xl border border-white/5 shadow-inner shrink-0">
          {[
            { label: translate("LEDGER_STREAM_TAB"), icon: Briefcase },
            { label: translate("MANDI_PRICES_TAB"), icon: Layers },
            { label: translate("VOICE_CREDIT_TAB"), icon: Coins },
          ].map((tab, idx) => {
            const SelectedIcon = tab.icon;
            const isTabActive = activeTab === idx;
            return (
              <button
                key={idx}
                onClick={() => setActiveTab(idx)}
                className={`flex-1 py-3 rounded-xl transition duration-200 text-xxs md:text-xs font-bold flex items-center justify-center gap-2 ${
                  isTabActive
                    ? "bg-gradient-to-r from-slate-800 to-slate-900 border border-white/10 text-white shadow"
                    : "text-slate-400 hover:text-slate-100"
                }`}
              >
                <SelectedIcon className={`h-4 w-4 ${isTabActive ? "text-cyan-400" : "text-slate-400"}`} />
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Dynamic Secondary Panel Contents according to chosen Tab */}
        <div className="mt-6 flex-1 flex flex-col">
          
          {/* LEDGER TIMELINE TAB CONTAINER */}
          {activeTab === 0 && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              
              {/* Chronological Timeline Grid Stream */}
              <div className="lg:col-span-2 space-y-4">
                
                {/* Search Bar & Categorizations filters block */}
                <div className="bg-slate-900/40 border border-white/5 rounded-2xl p-4 flex flex-col md:flex-row gap-3">
                  <div className="flex-1 relative">
                    <Search className="absolute left-3 top-3 h-4.5 w-4.5 text-slate-500 pointer-events-none" />
                    <input
                      type="text"
                      placeholder="Search transactions..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="w-full bg-slate-950 border border-white/5 rounded-xl pl-10 pr-4 py-2 text-xs placeholder:text-slate-500 focus:outline-none focus:border-cyan-500"
                    />
                  </div>
                  
                  <div className="flex items-center gap-2">
                    <span className="text-[10px] text-slate-500 font-bold uppercase whitespace-nowrap">Filter:</span>
                    <select
                      value={categoryFilter}
                      onChange={(e) => setCategoryFilter(e.target.value)}
                      className="bg-slate-950 border border-white/5 text-slate-300 text-xs font-bold rounded-xl px-3 py-2 focus:outline-none"
                    >
                      <option value="All">All Categories</option>
                      <option value="Sale">Sales</option>
                      <option value="Purchase">Purchases</option>
                      <option value="Udhaar">Udhaar Books</option>
                      <option value="Expense">Expenses</option>
                    </select>

                    <select
                      value={sortBy}
                      onChange={(e) => setSortBy(e.target.value)}
                      className="bg-slate-950 border border-white/5 text-slate-300 text-xs font-bold rounded-xl px-3 py-2 focus:outline-none"
                    >
                      <option value="Newest">Newest</option>
                      <option value="Oldest">Oldest</option>
                      <option value="Highest Val">Highest Amount</option>
                    </select>
                  </div>
                </div>

                <div className="flex items-center justify-between px-1">
                  <h3 id="timeline-feed-title" className="text-sm font-extrabold text-slate-300 uppercase tracking-widest flex items-center gap-1.5">
                    <Clock className="h-4 w-4 text-cyan-400" />
                    <span>Transaction Timeline ({filteredTrans.length})</span>
                  </h3>
                  
                  <div className="flex items-center gap-2">
                    <button
                      onClick={seedSampleTransactions}
                      className="text-[10px] font-bold text-cyan-400 bg-cyan-400/5 hover:bg-cyan-500/10 px-2.5 py-1.5 rounded-lg border border-cyan-400/20 cursor-pointer"
                    >
                      Seed Demo data
                    </button>
                    <button
                      onClick={clearAllData}
                      className="text-[10px] font-bold text-rose-400 bg-rose-400/5 hover:bg-rose-500/10 px-2.5 py-1.5 rounded-lg border border-rose-400/20 cursor-pointer"
                    >
                      Clear All
                    </button>
                  </div>
                </div>

                {/* Ledger Timline Records Output */}
                {filteredTrans.length === 0 ? (
                  <div className="bg-slate-900/50 border border-white/5 rounded-2xl p-10 text-center">
                    <p className="text-slate-500 text-xs font-semibold">No transactions match the selected filters.</p>
                    <p className="text-slate-600 text-[10px] mt-1.5">Tap "Seed Demo Data" above, or speak using the microphone!</p>
                  </div>
                ) : (
                  <div className="space-y-3.5 max-h-[550px] overflow-y-auto pr-1">
                    {filteredTrans.map((t) => (
                      <div
                        key={t.id}
                        className="bg-slate-900/50 border border-white/5 hover:border-white/10 rounded-2xl p-4 transition-all duration-200 group relative overflow-hidden"
                      >
                        <div className="absolute top-0 left-0 h-full w-1 bg-gradient-to-b from-cyan-500 to-indigo-500 opacity-0 group-hover:opacity-100 transition-all rounded" />
                        
                        <div className="flex items-start justify-between gap-3">
                          <div className="flex-1 min-w-0">
                            
                            <div className="flex flex-wrap items-center gap-2">
                              <span className={`text-[9px] font-black uppercase tracking-widest px-2.5 py-0.5 rounded-md ${
                                t.category === "Sale"
                                  ? "bg-emerald-500/15 text-emerald-400 border border-emerald-500/10"
                                  : t.category === "Udhaar"
                                  ? "bg-rose-500/15 text-rose-400 border border-rose-500/10"
                                  : "bg-amber-500/15 text-amber-400 border border-amber-500/10"
                              }`}>
                                {t.category}
                              </span>
                              
                              <span className="text-slate-500 text-[10px] font-medium font-mono">
                                {new Date(t.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} • {new Date(t.timestamp).toLocaleDateString([], { month: 'short', day: 'numeric' })}
                              </span>

                              {!t.isSynced && (
                                <span className="text-xxs font-bold text-amber-400 hover:underline cursor-pointer bg-amber-500/10 px-2 py-0.5 rounded-md border border-amber-500/15 flex items-center gap-1" onClick={syncOfflineTransactions}>
                                  <span className="h-1.5 w-1.5 rounded-full bg-amber-400 animate-pulse" /> Off-line log
                                </span>
                              )}
                            </div>

                            <h4 className="text-xs font-bold text-white mt-2 truncate">{t.item}</h4>
                            
                            <p className="text-[11px] text-slate-400 mt-1 font-semibold italic truncate">
                              "{t.rawVoiceText}"
                            </p>

                            <div className="flex items-center gap-3.5 mt-2.5 text-xxs font-mono text-slate-500 font-bold uppercase tracking-wider">
                              <span>Qty: {t.quantity} {t.unit}</span>
                              <span>Rate: {currencySymbol}{t.pricePerUnit}</span>
                              {t.partyName && <span className="text-amber-400">Borrower: {t.partyName}</span>}
                            </div>

                          </div>

                          <div className="flex flex-col items-end gap-2.5">
                            <span className="text-sm font-black text-white px-2 py-1 bg-white/5 rounded-lg">
                              {currencySymbol}{t.totalAmount.toLocaleString()}
                            </span>
                            <button
                              onClick={() => deleteTransaction(t.id)}
                              className="opacity-0 group-hover:opacity-100 p-1.5 bg-rose-500/15 hover:bg-rose-500/30 text-rose-400 rounded-lg transition-all"
                              title="Delete Transaction"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          </div>
                        </div>

                      </div>
                    ))}
                  </div>
                )}

              </div>

              {/* Side bar - Dynamic stock and Offline Data sync simulator */}
              <div className="space-y-6">
                
                {/* Live Inventory Stock Card */}
                <div className="bg-slate-900/60 border border-white/5 rounded-3xl p-5 shadow-xl">
                  
                  <div className="flex items-center justify-between border-b border-white/5 pb-3">
                    <h3 className="text-xs font-black text-slate-200 uppercase tracking-widest flex items-center gap-2">
                      <Layers className="h-4 w-4 text-cyan-400" />
                      <span>Live Stock Levels ({inventory.length})</span>
                    </h3>
                    <ShoppingBag className="h-4 w-4 text-slate-500" />
                  </div>

                  <div className="divide-y divide-white/5 mt-4 space-y-3.5 max-h-[300px] overflow-y-auto pr-1">
                    {inventory.map((inv, idx) => (
                      <div key={idx} className="flex items-center justify-between pt-3 first:pt-0">
                        <div>
                          <span className="text-xs font-extrabold text-white">{inv.itemName}</span>
                          <span className="text-[10px] text-slate-500 block mt-0.5">Last updated: {new Date(inv.lastUpdated).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}</span>
                        </div>
                        <div className="text-right">
                          <span className="text-xs font-black text-slate-200 block">
                            {inv.stockQuantity.toFixed(1)} {inv.unit}
                          </span>
                          
                          {/* Stock capacity tags */}
                          {inv.stockQuantity < 20 ? (
                            <span className="text-[9px] font-extrabold text-rose-400 bg-rose-500/10 px-1.5 py-0.2 rounded uppercase block mt-1">
                              Low Stock ⚠️
                            </span>
                          ) : (
                            <span className="text-[9px] font-extrabold text-emerald-400 bg-emerald-500/10 px-1.5 py-0.2 rounded uppercase block mt-1">
                              Healthy
                            </span>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>

                </div>

                {/* Secure Sync Management card */}
                <div className="bg-gradient-to-br from-slate-900 to-slate-950 border border-white/5 rounded-3xl p-5 shadow-xl relative group">
                  <div className="absolute top-0 right-0 w-24 h-24 bg-indigo-500/5 rounded-full blur-2xl" />
                  
                  <div className="flex items-center justify-between gap-3">
                    <h3 className="text-xs font-black text-slate-200 uppercase tracking-widest flex items-center gap-2">
                      <RefreshCw className="h-4.5 w-4.5 text-cyan-400" />
                      <span>Uplink Synchronize</span>
                    </h3>
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setShowTechnicalSyncDetails(prev => !prev);
                      }}
                      className="relative z-10 p-2 bg-white/5 hover:bg-white/15 hover:text-cyan-400 border border-white/5 hover:border-cyan-500/30 rounded-xl text-slate-400 transition-all duration-200 cursor-pointer shrink-0 flex items-center justify-center active:scale-95"
                      title="Show Technical Details"
                    >
                      <Info className="h-4.5 w-4.5 text-cyan-400" />
                    </button>
                  </div>

                  <p className="text-xxs text-slate-400 mt-2">
                    Keeps all your recorded sales, purchases, and cash transactions safely backed up online.
                  </p>

                  {showTechnicalSyncDetails && (
                    <div className="mt-3.5 pt-3.5 border-t border-white/5 space-y-2 text-xxs leading-relaxed animate-fade-in text-slate-300">
                      <p className="font-bold text-slate-200">
                        Architecture Details:
                      </p>
                      <ul className="list-disc pl-4 space-y-1 text-slate-400">
                        <li>Local persistence uses Android Room DB with SQLite schemas.</li>
                        <li>Buffered transaction streams are cached in a safe local sandbox.</li>
                        <li>Transactions synchronize using JSON payload handshakes with cloud microservices.</li>
                        <li>Un-synced changes are flagged as offline fallbacks and are auto-resolved upon connection.</li>
                      </ul>
                      
                      <div className="bg-slate-950/60 p-2.5 rounded-xl border border-white/5 font-mono text-[10px] text-slate-400 space-y-1 mt-2">
                        <div>Database Type: SQLite (Local) / Firestore (Cloud)</div>
                        <div>Total Records: {transactions.length} items</div>
                        <div>Pending Push Queue: {transactions.filter((t) => !t.isSynced).length} items</div>
                      </div>
                    </div>
                  )}
                  
                  <div className="mt-4 bg-slate-950/80 p-3 rounded-2xl border border-white/5 flex justify-between items-center text-xxs font-bold">
                    <span className="text-slate-400">Sync Status:</span>
                    {transactions.filter((t) => !t.isSynced).length > 0 ? (
                      <span className="text-amber-400 font-mono">
                        {transactions.filter((t) => !t.isSynced).length} items pending
                      </span>
                    ) : (
                      <span className="text-emerald-400 font-mono">
                        ✓ All synced safely
                      </span>
                    )}
                  </div>

                  <button
                    onClick={syncOfflineTransactions}
                    disabled={isSyncing}
                    className="w-full mt-4 py-3 bg-cyan-500 hover:bg-cyan-400 disabled:bg-slate-800 text-slate-950 disabled:text-slate-500 font-bold text-xs rounded-xl flex items-center justify-center gap-2 shadow-lg transition cursor-pointer"
                  >
                    <RefreshCw className={`h-4 w-4 ${isSyncing ? "animate-spin" : ""}`} />
                    {isSyncing ? "Syncing..." : "Synchronize"}
                  </button>
                </div>

              </div>

            </div>
          )}

          {/* DYNAMIC REGIONAL MANDI PRICES TAB */}
          {activeTab === 1 && (
            <div className="bg-slate-900/40 border border-white/5 rounded-3xl p-5 md:p-6 shadow-xl space-y-6">
              
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-white/5 pb-4.5">
                <div>
                  <h3 className="text-base font-extrabold text-white flex items-center gap-2">
                    <Layers className="h-5 w-5 text-cyan-400" />
                    <span>Dynamic Mandi Agricultural Rates</span>
                  </h3>
                  <p className="text-xs text-slate-400 mt-1">
                    Aggregated across APMC market exchanges based on regional coordinates, updated via live AI feed logs.
                  </p>
                </div>
                <div className="bg-slate-950 border border-white/5 px-4 py-2 rounded-xl text-center">
                  <span className="text-[10px] uppercase font-bold text-slate-500 block">Market State</span>
                  <span className="text-xs font-black text-emerald-400">● EXCHANGE OPEN</span>
                </div>
              </div>

              {/* Grid of agricultural rates */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {mandiPrices.map((crop, idx) => (
                  <div
                    key={idx}
                    className="bg-slate-950/90 border border-white/5 hover:border-white/10 rounded-2xl p-4.5 shadow transition duration-200 flex flex-col justify-between"
                  >
                    <div>
                      <div className="flex justify-between items-start gap-4">
                        <span className="text-sm font-extrabold text-white">{crop.cropName}</span>
                        
                        {/* Rate trend badges */}
                        {crop.priceChangeTrend === "UP" ? (
                          <span className="px-2.5 py-0.5 rounded bg-emerald-500/15 text-emerald-400 text-[10px] font-black flex items-center gap-1 border border-emerald-500/10">
                            <TrendingUp className="h-3.5 w-3.5" /> UP
                          </span>
                        ) : crop.priceChangeTrend === "DOWN" ? (
                          <span className="px-2.5 py-0.5 rounded bg-rose-500/15 text-rose-400 text-[10px] font-black flex items-center gap-1 border border-rose-500/10">
                            <TrendingDown className="h-3.5 w-3.5" /> DOWN
                          </span>
                        ) : (
                          <span className="px-2.5 py-0.5 rounded bg-slate-800 text-slate-400 text-[10px] font-black tracking-wide border border-white/5">
                            STABLE
                          </span>
                        )}
                      </div>
                      
                      <p className="text-xxs text-slate-400 font-medium mt-1 uppercase flex items-center gap-1">
                        <MapPin className="h-3 w-3 text-cyan-400" /> {crop.marketName}
                      </p>
                    </div>

                    <div className="mt-5 pt-3.5 border-t border-white/5 flex items-end justify-between">
                      <div>
                        <span className="text-[9px] uppercase text-slate-500 font-bold block">Assessed Price</span>
                        <span className="text-lg font-black text-white">
                          {currencySymbol}{crop.price.toFixed(2)} <span className="text-xs font-semibold text-slate-400">/ {crop.unit}</span>
                        </span>
                      </div>
                      <span className="text-[10px] text-slate-500 font-mono font-bold">
                        {crop.state}
                      </span>
                    </div>

                  </div>
                ))}
              </div>

              {/* Add customized Mandi item info box */}
              <div className="bg-slate-950/40 border border-white/5 rounded-2xl p-4 flex flex-col md:flex-row items-center gap-4 text-xs text-slate-400">
                <HelpCircle className="h-6 w-6 text-cyan-400 shrink-0" />
                <p>
                  <strong>Tip for Farmers and Traders:</strong> You can ask about new crops or pricing by asking our AI. For example: 
                  <span className="italic block font-bold text-cyan-400 mt-1">"What is the mandi price of mustard today?" or "Give me pyaj rates"</span>
                </p>
              </div>

            </div>
          )}

          {/* REAL TIME CREDIT ELIGIBILITY TAB */}
          {activeTab === 2 && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              
              {/* Credit eligibility index meter speedometer */}
              <div className="lg:col-span-1 bg-slate-900/40 border border-white/5 rounded-3xl p-5 md:p-6 shadow-xl flex flex-col items-center justify-center text-center">
                
                <h3 className="text-xs font-black text-slate-300 uppercase tracking-widest self-start mb-6">
                  Vendor Credit Readiness Index
                </h3>

                {/* Animated credit gauge */}
                <div className="relative w-44 h-44 flex items-center justify-center">
                  <svg className="w-full h-full transform -rotate-90">
                    <circle
                      cx="88"
                      cy="88"
                      r="76"
                      stroke="#1e293b"
                      strokeWidth="15"
                      fill="transparent"
                    />
                    <circle
                      cx="88"
                      cy="88"
                      r="76"
                      stroke="url(#creditGrad)"
                      strokeWidth="15"
                      fill="transparent"
                      strokeDasharray={477}
                      strokeDashoffset={477 - (477 * ((creditScore - 300) / 550))}
                      strokeLinecap="round"
                      className="transition-all duration-1000 ease-out"
                    />
                    <defs>
                      <linearGradient id="creditGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stopColor="#ef4444" />
                        <stop offset="50%" stopColor="#f59e0b" />
                        <stop offset="100%" stopColor="#10b981" />
                      </linearGradient>
                    </defs>
                  </svg>
                  
                  {/* Gauge value readout */}
                  <div className="absolute inset-0 flex flex-col items-center justify-center">
                    <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Score</span>
                    <span className="text-3xl font-black text-white">{creditScore}</span>
                    <span className="text-[10px] text-slate-400 font-bold mt-1 uppercase">300-850 range</span>
                  </div>
                </div>

                <div className="mt-4">
                  <span className={`px-4 py-1.5 rounded-full text-xs font-black uppercase tracking-widest ${
                    creditScore >= 700
                      ? "bg-emerald-500/15 text-emerald-400 border border-emerald-400/20"
                      : creditScore >= 550
                      ? "bg-amber-500/15 text-amber-400 border border-amber-400/20"
                      : "bg-rose-500/15 text-rose-400 border border-rose-400/20"
                  }`}>
                    {creditScore >= 700 ? "EXCELLENT STATUS" : creditScore >= 550 ? "FAIR STATUS" : "NEED CONSOLIDATION"}
                  </span>
                </div>

                <p className="text-xxs text-slate-400 font-bold mt-4">
                  Powered by Swarajya Micro-Credit algorithmic indices. Your logged volume increases your credit value.
                </p>

              </div>

              {/* Loans, credit suggestions and recommendations */}
              <div className="lg:col-span-2 space-y-6">
                
                <div className="bg-slate-900/40 border border-white/5 rounded-3xl p-5 md:p-6 shadow-xl">
                  
                  <h3 className="text-sm font-extrabold text-white flex items-center gap-2">
                    <Info className="h-5 w-5 text-cyan-400" />
                    <span>Algorithmic Trust Analysis</span>
                  </h3>
                  
                  <div className="divide-y divide-white/5 mt-4 space-y-4">
                    
                    <div className="pt-4 first:pt-0">
                      <h4 className="text-xs font-bold text-slate-200">Consistency Ledger Bonus</h4>
                      <p className="text-xxs text-slate-400 mt-1">
                        Regular ledger logs over consecutive days are treated as positive operating consistency trackers. 
                        You have logged <strong>{transactions.length} total operational transactions</strong>, adding strength to your credit profile.
                      </p>
                    </div>

                    <div className="pt-4">
                      <h4 className="text-xs font-bold text-slate-200">Repayment Receivables (Udhaar) Load</h4>
                      <p className="text-xxs text-slate-400 mt-1">
                        High ratios of outstanding debts (Udhaar due) compared to completed sales volumes acts as a slight pressure point on score values. 
                        Your current credit receivable is <strong>{currencySymbol}{totalUdhaarVal.toLocaleString()}</strong>. Keep collecting due balances to elevate score rankings.
                      </p>
                    </div>

                    <div className="pt-4">
                      <h4 className="text-xs font-bold text-slate-200">Micro-Lending Institution Partners</h4>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-3">
                        <div className="bg-slate-950 p-3.5 rounded-2xl border border-white/5 flex flex-col justify-between">
                          <div>
                            <span className="text-xs font-extrabold text-white block">Swarajya Vendor Loan</span>
                            <span className="text-[10px] text-slate-400 block mt-0.5">Micro-collateral microfinance scheme</span>
                          </div>
                          
                          <div className="mt-4 flex items-center justify-between">
                            <span className="text-xxs font-mono font-bold text-slate-500">Interest rate: 12% p.a.</span>
                            {creditScore >= 650 ? (
                              <span className="text-[9px] font-black text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded uppercase">
                                Eligible ✅
                              </span>
                            ) : (
                              <span className="text-[9px] font-black text-rose-400 bg-rose-500/10 px-2 py-0.5 rounded uppercase">
                                Score low
                              </span>
                            )}
                          </div>
                        </div>

                        <div className="bg-slate-950 p-3.5 rounded-2xl border border-white/5 flex flex-col justify-between">
                          <div>
                            <span className="text-xs font-extrabold text-white block">NABARD Agri-Restock</span>
                            <span className="text-[10px] text-slate-400 block mt-0.5">Financing for small agri procurers</span>
                          </div>
                          
                          <div className="mt-4 flex items-center justify-between">
                            <span className="text-xxs font-mono font-bold text-slate-500">Interest rate: 8% p.a.</span>
                            {creditScore >= 720 ? (
                              <span className="text-[9px] font-black text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded uppercase">
                                Eligible ✅
                              </span>
                            ) : (
                              <span className="text-[9px] font-black text-rose-400 bg-rose-500/10 px-2 py-0.5 rounded uppercase">
                                Score low
                              </span>
                            )}
                          </div>
                        </div>

                      </div>
                    </div>

                  </div>

                </div>

              </div>

            </div>
          )}

        </div>

      </div>

    </div>

      {/* Humble outer frame and container clear styling bounds (Respecting Design Philosophy instructions) */}
      <footer className="z-10 py-5 text-center text-[10px] font-bold text-slate-600 bg-slate-950 border-t border-white/5">
        <p className="tracking-wide">
          VoiceOps AI Business Ledger Suite • Fully Operational with SECURE Server-Side Gemini API & HTML5 Browser Speech Synthesis
        </p>
      </footer>

    </div>
  );
}
