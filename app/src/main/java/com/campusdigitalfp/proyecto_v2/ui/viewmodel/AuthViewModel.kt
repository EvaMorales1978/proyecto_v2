package com.campusdigitalfp.proyecto_v2.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusdigitalfp.proyecto_v2.data.network.OdooClient
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    // Usamos el cliente directamente o un repositorio de auth
    private val client = OdooClient("http://192.168.1.249:8069")

    var uid by mutableStateOf<Int?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun fetchUser(db: String, user: String, pass: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val result = client.authenticate(db, user, pass)
                uid = result // Si falla, suele devolver 0 o lanzar excepción
            } catch (e: Exception) {
                uid = 0 // Marcamos error
            } finally {
                isLoading = false
            }
        }
    }

    // Función para resetear el estado al volver al login
    fun resetLogin() {
        uid = null
    }
}