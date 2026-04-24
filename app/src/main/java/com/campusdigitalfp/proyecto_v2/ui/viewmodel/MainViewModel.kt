package com.campusdigitalfp.proyecto_v2.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.campusdigitalfp.proyecto_v2.domain.model.OdooDataPackage
import com.campusdigitalfp.proyecto_v2.data.repository.OdooRepository
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: OdooRepository,
    val uid: Int
) : ViewModel() {

    var uiState by mutableStateOf<MainUiState>(MainUiState.Loading)
        private set

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            try {
                val data = repository.fetchInitialData(uid)
                uiState = MainUiState.Success(data)
            } catch (e: Exception) {
                uiState = MainUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(val data: OdooDataPackage) : MainUiState()
    data class Error(val msg: String) : MainUiState()
}