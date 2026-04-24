package com.campusdigitalfp.proyecto_v2.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusdigitalfp.proyecto_v2.data.network.OdooClient
import kotlinx.coroutines.launch

class AuthViewModel() : ViewModel() {

    var uid by mutableStateOf<Int?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun fetchUser(url: String,db: String, user: String, pass: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val client = OdooClient(url)
                val result = client.authenticate(db, user, pass)

                uid = result
            } catch (e: Exception) {
                uid = 0
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