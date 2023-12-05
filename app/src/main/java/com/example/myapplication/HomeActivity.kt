package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class HomeActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialiser TextToSpeech
        textToSpeech = TextToSpeech(this, this)

        // Trouvez les éléments TextView par leur ID
        val objects = findViewById<RelativeLayout>(R.id.objetRelativeLayout)
        val texts = findViewById<RelativeLayout>(R.id.textRelativeLayout)
        val parametre = findViewById<RelativeLayout>(R.id.settingsRelativeLayout)
        val navigate = findViewById<RelativeLayout>(R.id.navRelativeLayout)
        val vocale = findViewById<RelativeLayout>(R.id.vocaleRelativeLayout)


        // Ajoutez un écouteur de clic à l'élément TextView
        objects.setOnClickListener {
            speakText("Object")
            val intent = Intent(this@HomeActivity, MainActivity::class.java)
            startActivity(intent)
        }

        texts.setOnClickListener {
            speakText("Text to speech")
            val intent = Intent(this@HomeActivity, TextToSpeechActivity::class.java)
            startActivity(intent)
        }

        parametre.setOnClickListener {
            speakText("Parametre")
           /* val intent = Intent(this@HomeActivity, Setting::class.java)
            startActivity(intent)*/
        }

        navigate.setOnClickListener {
            speakText("Navigation")
            /*val intent = Intent(this@HomeActivity, Navigation::class.java)
            startActivity(intent)*/
        }
        vocale.setOnClickListener {
            speakText("Analyse Vocale")

        }
    }

    // Fonction pour lire le texte avec TextToSpeech
    private fun speakText(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // Implémenter la méthode OnInitListener
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Langue par défaut : vous pouvez changer la langue si nécessaire
            val result = textToSpeech.setLanguage(Locale.getDefault())

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // La langue n'est pas prise en charge, vous pouvez gérer cela ici
            }
        } else {
            // Erreur lors de l'initialisation de TextToSpeech
        }
    }

    // Assurez-vous de libérer les ressources TextToSpeech lors de la fermeture de l'activité
    override fun onDestroy() {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        textToSpeech.shutdown()
        super.onDestroy()
    }
}