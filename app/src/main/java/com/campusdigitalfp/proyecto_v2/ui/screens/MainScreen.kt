package com.campusdigitalfp.proyecto_v2.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Importante para listar elementos
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier // <--- Verifica este import
import com.campusdigitalfp.proyecto_v2.ui.viewmodel.MainUiState
import com.campusdigitalfp.proyecto_v2.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier // <--- Añade esto aquí
) {
    // Usamos el modifier en el contenedor principal (Box)
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = viewModel.uiState) {
            is MainUiState.Loading -> CircularProgressIndicator()
            is MainUiState.Error -> Text("Error: ${state.msg}")
            is MainUiState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Text("Lotes: ${state.data.lots.size}") }
                    item { Text("Socios: ${state.data.partners.size}") }
                    item { Text("Pickings: ${state.data.pickings.size}") }
                }
            }
        }
    }
}