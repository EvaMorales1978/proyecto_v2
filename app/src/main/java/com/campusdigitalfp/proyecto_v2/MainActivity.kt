package com.campusdigitalfp.proyecto_v2


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.campusdigitalfp.proyecto.navigation.Navigation
import com.campusdigitalfp.proyecto_v2.data.network.OdooClient
import com.campusdigitalfp.proyecto_v2.ui.theme.Proyecto_v2Theme // El nombre de tu carpeta de tema
// Importa la función Navigation (ajusta la ruta según donde la creaste)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val odooClient = OdooClient("http://192.168.1.249:8069")

        enableEdgeToEdge()
        setContent {
            Proyecto_v2Theme {
              //  Navigation()
                Surface(color = MaterialTheme.colorScheme.background) {
                    Navigation(odooClient)
                }
            }
        }
    }
}