package com.campusdigitalfp.proyecto_v2.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

import android.util.Log
import com.campusdigitalfp.proyecto_v2.data.repository.OdooRepositoryMove
import com.campusdigitalfp.proyecto_v2.domain.model.StockMove

class MoveViewModel : ViewModel() {
    private val repositoryMove = OdooRepositoryMove()


    var moves by mutableStateOf<List<StockMove>>(emptyList())

    var isLoading by mutableStateOf(false)

    fun fetchMoves(url: String,db: String, uid: Int, pass: String) {
        viewModelScope.launch {
            isLoading = true
            Log.d("ODOO_CHECK" , "Iniciando fetchMoves...")
            try {
                val result = repositoryMove.getMovesGrouped(url, db , uid , pass)
                Log.d("ODOO_CHECK" , "Moves recibidos: ${result.size}")
                moves = result

            } catch (e: Exception) {
                Log.e("ODOO_CHECK" , "Error en la petición: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
                Log.d("ODOO_CHECK" , "Finalizado fetchMoves. Estado de moves: ${moves.size}")
            }
        }
    }
}