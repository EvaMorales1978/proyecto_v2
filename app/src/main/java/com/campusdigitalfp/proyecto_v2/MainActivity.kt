package com.campusdigitalfp.proyecto_v2


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.campusdigitalfp.proyecto.navigation.Navigation
import com.campusdigitalfp.proyecto_v2.data.network.OdooClient
import com.campusdigitalfp.proyecto_v2.ui.theme.Proyecto_v2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Proyecto_v2Theme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Navigation()
                }
            }
        }
    }
}