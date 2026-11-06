package com.darchums.dalmiren

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.darchums.dalmiren.data.remote.NetworkClient
import com.darchums.dalmiren.ui.navigation.DalMirenApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Initialize encrypted session storage before any API request.
        NetworkClient.initialize(applicationContext)

        // Development LAN address. Production must use HTTPS.
        NetworkClient.setCustomBaseUrl("http://192.168.0.212:8000/")

        setContent {
            DalMirenApp()
        }
    }
}
