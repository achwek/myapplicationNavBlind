package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat


class FingerPrintActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finger_print)

        // Initialize msgtext
        val msgtex = findViewById<TextView>(R.id.txtmsg)

        // Create a variable for our BiometricManager and check if the user can use biometric sensor
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                msgtex.text = "You can use the fingerprint sensor to login"
                msgtex.setTextColor(Color.parseColor("#fafafa"))
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> msgtex.text =
                "This device does not have a fingerprint sensor"

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> msgtex.text =
                "The biometric sensor is currently unavailable"

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> msgtex.text =
                "Your device doesn't have a fingerprint saved, please check your security settings"
        }

        //Créer une variable pour notre exécuteur
        val executor = ContextCompat.getMainExecutor(this)

        // Cela nous donnera le résultat de l'AUTHENTIFICATION
        val biometricPrompt = BiometricPrompt(
            this@FingerPrintActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                // Cette méthode est appelée lorsque l'authentification réussit
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(
                        applicationContext,
                        "Login with Empreint is Success",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this@FingerPrintActivity, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        // Créez une variable pour notre promptInfo DIALOGUE BIOMÉTRIQUE
        val promptInfo = PromptInfo.Builder()
            .setTitle("GFG")
            .setDescription("Use your fingerprint to login")
            .setNegativeButtonText("Cancel")
            .build()

        //Déclenchez l'authentification dès le démarrage de l'activité
        biometricPrompt.authenticate(promptInfo)
    }
}
