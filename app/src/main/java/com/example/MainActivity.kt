package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.VoiceOpsScreen
import com.example.ui.VoiceOpsViewModel
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private val viewModel: VoiceOpsViewModel by viewModels()
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    // System-level speech-to-text popup binder activity launcher
    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            if (spokenText.isNotEmpty()) {
                viewModel.processVoiceCommand(spokenText)
            }
        }
    }

    // Geolocation Permission Launcher to support real-time browser coordinates
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            detectAndTriggerLocationUpdate()
        } else {
            Toast.makeText(this, "Standard location permission denied. Using local assumptions.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Setup Indian accented Speech Synthesizer (TTS)
        textToSpeech = TextToSpeech(this, this)

        // Try to auto-detect location if permission is already given
        detectAndTriggerLocationUpdate()

        setContent {
            MyApplicationTheme {
                val latestResponse = viewModel.latestResponse.collectAsState()
                
                // Continuous voice response automation trigger
                LaunchedEffect(latestResponse.value) {
                    latestResponse.value?.let { response ->
                        if (response.explanation.isNotEmpty()) {
                            speakOutLoud(response.explanation)
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VoiceOpsScreen(
                        viewModel = viewModel,
                        onRecordClick = { triggerSpeechInput() },
                        onRequestLocationPermission = { triggerLocationPermissionRequest() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun triggerLocationPermissionRequest() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun detectAndTriggerLocationUpdate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "Location permissions are not granted yet.")
            return
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            Log.e("MainActivity", "LocationManager service not available")
            return
        }

        try {
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }

            if (bestLocation != null) {
                Log.d("MainActivity", "Found last known location: Lattitude=${bestLocation.latitude}, Longitude=${bestLocation.longitude}")
                processLocationCoordinates(bestLocation)
            } else {
                // Register a fast single update request
                val provider = when {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    else -> null
                }
                if (provider != null) {
                    locationManager.requestSingleUpdate(provider, object : LocationListener {
                        override fun onLocationChanged(loc: Location) {
                            processLocationCoordinates(loc)
                        }
                        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
                        override fun onProviderEnabled(p0: String) {}
                        override fun onProviderDisabled(p0: String) {}
                    }, Looper.getMainLooper())
                }
            }
        } catch (e: SecurityException) {
            Log.e("MainActivity", "SecurityException requesting location update", e)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error requesting location", e)
        }
    }

    private fun processLocationCoordinates(location: Location) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val address = addresses?.firstOrNull()
            if (address != null) {
                val countryCode = address.countryCode ?: "US"
                val countryName = address.countryName ?: "United States"
                val cityName = address.locality ?: address.subAdminArea ?: address.adminArea
                viewModel.updateLocation(countryCode, countryName, cityName)
            } else {
                // Heuristic mapping of coordinates if internet is sparse in sandbox
                val lat = location.latitude
                val lng = location.longitude
                if (lat in 8.0..37.0 && lng in 68.0..97.0) {
                    viewModel.updateLocation("IN", "India", "Live Region")
                } else {
                    viewModel.updateLocation("US", "United States", "Live Region")
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Geocoder failed. Using coordinate heuristics instead.", e)
            val lat = location.latitude
            val lng = location.longitude
            if (lat in 8.0..37.0 && lng in 68.0..97.0) {
                viewModel.updateLocation("IN", "India", "Live Zone")
            } else {
                viewModel.updateLocation("US", "United States", "Live Zone")
            }
        }
    }

    private fun getSpeechLanguageCode(lang: String): String {
        return when (lang) {
            "English" -> "en-IN"
            "Hindi" -> "hi-IN"
            "Hinglish" -> "hi-IN"
            "Tamil" -> "ta-IN"
            "Telugu" -> "te-IN"
            "Bengali" -> "bn-IN"
            "Kannada" -> "kn-IN"
            "Marathi" -> "mr-IN"
            else -> "en-IN"
        }
    }

    private fun getSpeechPrompt(lang: String): String {
        return when (lang) {
            "English" -> "Please speak your transaction or question..."
            "Hindi" -> "कृपया अपना लेन-देन या प्रश्न बोलें..."
            "Hinglish" -> "Apna transaction ya sawal bolein..."
            "Tamil" -> "உங்கள் பரிவர்த்தனை அல்லது கேள்வியைப் பேசவும்..."
            "Telugu" -> "దయచేసి మీ లావాదేవీ లేదా ప్రశ్న మాట్లాడండి..."
            "Bengali" -> "দয়া করে আপনার লেনদেন বা প্রশ্ন বলুন..."
            "Kannada" -> "ದಯವಿಟ್ಟು ನಿಮ್ಮ ವಹಿವಾಟು ಅಥವಾ ಪ್ರಶ್ನೆಯನ್ನು ಹೇಳಿ..."
            "Marathi" -> "कृपया आपला व्यवहार किंवा प्रश्न बोला..."
            else -> "Please speak..."
        }
    }

    private fun setTtsLanguageFor(lang: String) {
        if (!isTtsInitialized) return
        val locale = when (lang) {
            "English" -> Locale.Builder().setLanguage("en").setRegion("IN").build()
            "Hindi" -> Locale.Builder().setLanguage("hi").setRegion("IN").build()
            "Hinglish" -> Locale.Builder().setLanguage("hi").setRegion("IN").build()
            "Tamil" -> Locale.Builder().setLanguage("ta").setRegion("IN").build()
            "Telugu" -> Locale.Builder().setLanguage("te").setRegion("IN").build()
            "Bengali" -> Locale.Builder().setLanguage("bn").setRegion("IN").build()
            "Kannada" -> Locale.Builder().setLanguage("kn").setRegion("IN").build()
            "Marathi" -> Locale.Builder().setLanguage("mr").setRegion("IN").build()
            else -> Locale.Builder().setLanguage("en").setRegion("IN").build()
        }
        val result = textToSpeech?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            textToSpeech?.language = Locale.US
        }
    }

    private fun triggerSpeechInput() {
        val currentLang = viewModel.currentLanguage.value
        val langCode = getSpeechLanguageCode(currentLang)
        val prompt = getSpeechPrompt(currentLang)

        setTtsLanguageFor(currentLang)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langCode)
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice Input not supported. Try using simulation chips!", Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Speech synthesis not supported in local sandbox", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            setTtsLanguageFor(viewModel.currentLanguage.value)
        } else {
            Log.e("MainActivity", "Automated Playback (TTS) initiation failed.")
        }
    }

    private fun speakOutLoud(text: String) {
        if (isTtsInitialized) {
            setTtsLanguageFor(viewModel.currentLanguage.value)
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voiceops_speech_trigger")
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

