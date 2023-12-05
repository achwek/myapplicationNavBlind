package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.myapplication.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import android.speech.tts.TextToSpeech
import android.content.Intent
import android.widget.Toast
import java.util.Locale
import android.speech.tts.TextToSpeech.OnInitListener


class MainActivity : AppCompatActivity(),TextToSpeech.OnInitListener {

    // Initialisation de variables
    lateinit var labels: List<String>
    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)
    // Crée une nouvelle instance de la classe Paint pour configurer les paramètres de dessin.
    val paint = Paint()
    lateinit var imageProcessor: ImageProcessor
// Elle servira à effectuer le traitement des images, par exemple le redimensionnement.

    lateinit var bitmap: Bitmap
    // Elle sera utilisée pour stocker l'image capturée depuis la caméra ou l'image traitée.
    lateinit var imageView: ImageView
    // Elle sera utilisée pour afficher l'image traitée à l'écran.
    lateinit var cameraDevice: CameraDevice
    // Elle représente la caméra matérielle du dispositif et permet d'interagir avec la caméra.
    lateinit var handler: Handler
    // Elle sera utilisée pour gérer un thread dédié à la gestion de la caméra, notamment pour la capture d'images.
    lateinit var cameraManager: CameraManager
    // Elle sera utilisée pour obtenir un accès au système de gestion des caméras de l'appareil, notamment pour configurer et ouvrir la caméra.
    lateinit var textureView: TextureView
    // Elle sera utilisée pour afficher un aperçu de la caméra et écouter les événements liés à la surface de la vue.
    lateinit var model: SsdMobilenetV11Metadata1
    // Elle semble représenter un modèle TensorFlow Lite pour la détection d'objets, et sera utilisée pour l'inférence sur les images capturées par la caméra.
    lateinit var textToSpeech: TextToSpeech
    private var lastSpeakTime: Long = 0

    // Fonction appelée lors de la création de l'activité
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Demande d'autorisation d'accès à la caméra
        get_permission()
        textToSpeech = TextToSpeech(this, this)
        // Chargement des étiquettes à partir d'un fichier
        labels = FileUtil.loadLabels(this, "labels.txt")
        // Configuration du processeur d'image
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()
        // Initialisation du modèle TensorFlow Lite
        model = SsdMobilenetV11Metadata1.newInstance(this)
        // Création d'un thread pour la gestion de la caméra
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        // Initialisation des éléments de l'interface utilisateur
        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)
        // Configuration du TextureView pour écouter les événements de la surface
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }
            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                processImageAndSpeak()
            }
            private fun speakText(objectName: String) {
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastSpeakTime > 1000) {
                    textToSpeech.speak(objectName, TextToSpeech.QUEUE_FLUSH, null, null)
                    lastSpeakTime = currentTime
                }
            }
            private fun processImageAndSpeak() {
                bitmap = textureView.bitmap!!
                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)

                val outputs = model.process(image)
                val locations = outputs.locationsAsTensorBuffer.floatArray
                val classes = outputs.classesAsTensorBuffer.floatArray
                val scores = outputs.scoresAsTensorBuffer.floatArray

                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutable)

                val h = mutable.height
                val w = mutable.width
                paint.textSize = h / 15f
                paint.strokeWidth = h / 85f
                var x = 0
                scores.forEachIndexed { index, fl ->
                    x = index
                    x *= 4
                    if (fl > 0.5) {
                        paint.setColor(colors[index])
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(RectF(locations[x + 1] * w, locations[x] * h, locations[x + 3] * w, locations[x + 2] * h), paint)
                        paint.style = Paint.Style.FILL
                        val objectName = labels[classes[index].toInt()]
                        val spokenText = objectName + " " + fl.toString()
                        canvas.drawText(spokenText, locations[x + 1] * w, locations[x] * h, paint)
                        speakText(objectName)
                    }
                }

                imageView.setImageBitmap(mutable)
            }
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

    }
    // Fonction appelée lors de la destruction de l'activité
    override fun onDestroy() {
        super.onDestroy()
        // Fermeture du modèle TensorFlow Lite
        model.close()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // TTS initialization successful
            val result = textToSpeech.setLanguage(Locale.getDefault())

            if (result == TextToSpeech.LANG_MISSING_DATA) {
                // Language data is missing, prompt the user to install it
                val installIntent = Intent()
                installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                startActivity(installIntent)
            } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language is not supported, prompt the user to choose another language
                // You can display a dialog or take appropriate action
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show()
            }
        } else {
            // TTS initialization failed, handle accordingly
            Toast.makeText(this, "Text-to-speech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }
    // Fonction pour ouvrir la caméra
    @SuppressLint("MissingPermission")
    fun open_camera() {
        // Ouvre la caméra via le gestionnaire de caméra (cameraManager)
        cameraManager.openCamera(cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
            // Lorsque la caméra est ouverte avec succès
            override fun onOpened(p0: CameraDevice) {
                // Stocke la caméra ouverte dans la variable cameraDevice
                cameraDevice = p0

                // Récupère la texture de la vue d'affichage (textureView)
                var surfaceTexture = textureView.surfaceTexture
                // Crée une surface à partir de la texture
                var surface = Surface(surfaceTexture)

                // Crée une requête de capture pour l'affichage en direct (TEMPLATE_PREVIEW)
                var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                // Associe la surface à la requête de capture
                captureRequest.addTarget(surface)

                // Crée une session de capture pour la caméra avec la liste des surfaces
                cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    // Lorsque la session de capture est configurée avec succès
                    override fun onConfigured(p0: CameraCaptureSession) {
                        // Définit la requête de capture en mode de répétition
                        p0.setRepeatingRequest(captureRequest.build(), null, null)
                    }

                    // En cas d'échec de configuration de la session de capture
                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                        // Gérer les erreurs ici si nécessaire
                    }
                }, handler)
            }
            // Lorsque la caméra est déconnectée
            override fun onDisconnected(p0: CameraDevice) {
                // Gérer la déconnexion ici si nécessaire
            }

            // En cas d'erreur lors de l'ouverture de la caméra
            override fun onError(p0: CameraDevice, p1: Int) {
                // Gérer les erreurs ici si nécessaire
            }
        }, handler)
    }

    // Fonction pour obtenir l'autorisation de la caméra
    fun get_permission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    // Fonction appelée lors de la réponse de l'utilisateur à la demande d'autorisation
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            get_permission()
        }
    }
}