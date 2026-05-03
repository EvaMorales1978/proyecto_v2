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

    var moveLineError by mutableStateOf<String?>(null)
    var moveLineResult by mutableStateOf<Map<String, Any>?>(null)

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


    fun processScannedLotMove(
        url: String,
        db: String,
        uid: Int,
        pass: String,
        lotName: String
    ) {
        viewModelScope.launch {
            isLoading = true
            moveLineError = null
            try {
                val result = repositoryMove.UpdateMoveLine(
                    url, db, uid, pass,
                    lotName
                )
                Log.d("ODOO_CHECK", "Finalizado UpdateMoveLine. Estado de pickings: ${result.size}")

                moveLineResult = result
                //fetchMoves(url, db, uid, pass)

                val updatedMoves = repositoryMove.getMovesGrouped(url, db, uid, pass)
                moves = updatedMoves  // ← esto actualiza el State y recompone la pantalla

            } catch (e: IllegalArgumentException) {
                moveLineError = e.message
            } catch (e: Exception) {
                moveLineError = "Error de conexión: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

}