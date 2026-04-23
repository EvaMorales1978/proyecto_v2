package com.campusdigitalfp.proyecto_v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.campusdigitalfp.proyecto_v2.data.network.OdooClient
import com.campusdigitalfp.proyecto_v2.data.repository.OdooRepository
import com.campusdigitalfp.proyecto_v2.ui.screens.MainScreen
import com.campusdigitalfp.proyecto_v2.ui.viewmodel.MainViewModel
import com.campusdigitalfp.proyecto_v2.ui.theme.Proyecto_v2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configuración de la conexión a Odoo
        val odooUrl = "http://192.168.1.249:8069"
        val odooClient = OdooClient(odooUrl)
        val odooRepository = OdooRepository(odooClient)

        // 2. Crear el ViewModel usando una Factory
        // (Esto es necesario porque el ViewModel tiene un parámetro en el constructor)
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(odooRepository) as T
            }
        })[MainViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            Proyecto_v2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 3. Llamada a tu pantalla principal pasando el viewModel configurado
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}