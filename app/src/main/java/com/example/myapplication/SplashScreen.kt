package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity


class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Crée un gestionnaire pour retarder le passage à l'écran principal
        Handler().postDelayed({
            // Démarre l'activité principale
            val intent = Intent(this@SplashScreen, FingerPrintActivity::class.java)
            startActivity(intent)
            finish() // Ferme l'écran de démarrage pour qu'il ne puisse pas être revenir en arrière
        }, SPLASH_DELAY)
    }

    companion object {
        // La durée de l'écran de démarrage en millisecondes
        private const val SPLASH_DELAY: Long = 3000
    }
}
