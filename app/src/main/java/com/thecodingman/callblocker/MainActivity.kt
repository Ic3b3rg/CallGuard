package com.thecodingman.callblocker

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile
        var isBlockingEnabled = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Richiama i permessi necessari
        requestPermissions()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Call Blocker",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // Switch per attivare/disattivare il blocco
                        var toggleState by remember { mutableStateOf(isBlockingEnabled) }
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(text = if (toggleState) "Blocco Attivo" else "Blocco Disattivo")
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = toggleState,
                                onCheckedChange = { isOn ->
                                    toggleState = isOn
                                    isBlockingEnabled = isOn
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Bottone per guidare l'utente alla schermata per impostare l'app come Call Screening App
                        Button(onClick = { promptCallScreeningPermission() }) {
                            Text("Imposta come filtratore di chiamate")
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = arrayOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS
        )
        requestPermissionsLauncher.launch(permissionsToRequest)
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            if (!it.value) {
                Toast.makeText(
                    this,
                    "Permesso negato: ${it.key}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Guida l'utente a impostare l'app come filtratore di chiamate.
     * Attualmente non esiste un intent diretto per il Call Screening, quindi viene aperta la schermata
     * delle impostazioni di default.
     */
    private fun promptCallScreeningPermission() {
        Toast.makeText(
            this,
            "Vai in Impostazioni > App > Chiamate per impostare questa app come filtratore di chiamate.",
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        startActivity(intent)
    }

    /**
     * Implementazione interna del CallScreeningService.
     * Quando il blocco è attivo, intercetta e rifiuta le chiamate in entrata.
     */
    class MyCallScreeningService : CallScreeningService() {
        override fun onScreenCall(callDetails: Call.Details) {
            Log.d("TAG", "Dettagli chiamata - Numero: ${callDetails.handle?.schemeSpecificPart ?: "sconosciuto"}, " +
                    "callerDisplayName: ${callDetails.callerDisplayName}, " +
                    "Stato: ${callDetails.callProperties}, " +
                    "ID: ${callDetails.callerDisplayNamePresentation}")
            if (isBlockingEnabled) {
                val response = CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .build()
                respondToCall(callDetails, response)
                Log.d("CallScreening", "Chiamata bloccata: ${callDetails.handle}")
            } else {
                // Se il blocco non è attivo, consente la chiamata.
                val response = CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .build()
                respondToCall(callDetails, response)
            }
        }
    }
}
