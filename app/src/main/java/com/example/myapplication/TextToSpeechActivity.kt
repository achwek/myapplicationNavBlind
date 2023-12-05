package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.*

class TextToSpeechActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var previewView: PreviewView
    private lateinit var detectedText: TextView
    private lateinit var textToSpeech: TextToSpeech
    private var isTtsProcessing = false

    private lateinit var cameraExecutor: ExecutorService
    private val CAMERA_PERMISSION_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_to_speech)

        previewView = findViewById(R.id.previewView)
        detectedText = findViewById(R.id.detectedText)

        // Initialiser le moteur TextToSpeech s'il n'est pas déjà initialisé
        textToSpeech = TextToSpeech(this, this)

        // Initialiser cameraExecutor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Request camera permissions if not granted
        if (hasCameraPermission()) {
            // Configurer la caméra
            startCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, start the camera
                startCamera()
            } else {
                // Camera permission denied, handle accordingly (e.g., show a message)
                Log.e("CameraPermission", "Camera permission denied")
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Configuration de la vue en direct (preview)
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Configuration de l'analyse d'image
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                            handleImageAnalysis(imageProxy)
                        })
                    }

                // Sélection de la caméra arrière
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Libération des cas d'utilisation existants et liaison des nouveaux
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )

            } catch (exc: Exception) {
                Log.e("CameraX", "La liaison des cas d'utilisation a échoué", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun handleImageAnalysis(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            detectAndExtractText(inputImage)
        }
        imageProxy.close()
    }

    private fun detectAndExtractText(image: InputImage) {
        val options = TextRecognizerOptions.Builder().build()
        val textRecognizer = TextRecognition.getClient(options)

        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                Log.d("TextExtraction", "Extracted Text: $extractedText")
                detectedText.text = "Extracted Text: $extractedText"
                detectLanguageAndRead(extractedText)
            }
            .addOnFailureListener { e ->
                Log.e("TextExtraction", "Text extraction error: $e")
                e.printStackTrace() // Loguer la trace de la pile pour plus d'informations
            }

    }

    private fun detectLanguageAndRead(text: String) {
        val languageIdentifier = LanguageIdentification.getClient(
            LanguageIdentificationOptions.Builder()
                .setConfidenceThreshold(0.5f)
                .build()
        )

        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                Log.d("LanguageDetection", "Detected Language: $languageCode")
                detectedText.text = "Detected Language: $languageCode\n$text"

                val locale = Locale(languageCode)
                if (isLanguageSupported(locale)) {
                    Log.d("LanguageDetection", "Language is supported. Reading text...")
                    // Read the text using TextToSpeech
                    readTextInLanguage(text, languageCode)
                } else {
                    Log.e("TextToSpeech", "Language not supported: $languageCode")
                }
            }
            .addOnFailureListener { e ->
                Log.e("LanguageDetection", "Language identification error: $e")
            }
    }
    private fun isLanguageSupported(locale: Locale): Boolean {
        val supportedLanguages = textToSpeech.availableLanguages

        // Vérifier si la langue spécifiée est prise en charge
        return supportedLanguages.any { supportedLocale ->
            supportedLocale.language == locale.language
                    && (locale.country.isEmpty() || supportedLocale.country == locale.country)
                    && (locale.variant.isEmpty() || supportedLocale.variant == locale.variant)
        }
    }

    @Suppress("DEPRECATION")
    private fun readTextInLanguage(text: String, languageCode: String) {
        val locale = Locale(languageCode)

        if (isTtsProcessing) {
            // Si le TextToSpeech est déjà en cours de traitement, ne lancez pas une nouvelle synthèse
            return
        }

        textToSpeech.stop()

        val result = textToSpeech.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TextToSpeech", "Language not supported")
        } else {
            isTtsProcessing = true

            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Utterance started
                }

                @Suppress("OverridingDeprecatedMember")
                override fun onDone(utteranceId: String?) {
                    // Utterance completed
                    if (utteranceId == UUID.randomUUID().toString()) {
                        // Synthesis is complete
                        // You can perform any additional actions after text is spoken
                        isTtsProcessing = false
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    // Error occurred
                    Log.e("TextToSpeech", "Error in speech synthesis. Utterance ID: $utteranceId")
                    isTtsProcessing = false
                }
            })

            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val defaultLocale = Locale.getDefault()
            val result = textToSpeech.setLanguage(defaultLocale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TextToSpeech", "Language not supported for $defaultLocale")
            }
        } else {
            Log.e("TextToSpeech", "Initialization failed")
        }
    }

    override fun onDestroy() {
        // Libérer les ressources de TextToSpeech
        if (textToSpeech.isSpeaking || isTtsProcessing) {
            textToSpeech.stop()
        }
        textToSpeech.shutdown()

        // Fermer la caméra
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}
