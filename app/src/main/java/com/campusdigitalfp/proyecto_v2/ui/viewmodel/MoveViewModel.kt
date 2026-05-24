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
    var allMovesCompleted by mutableStateOf(false)

    var validateSuccess by mutableStateOf<Boolean?>(null)
    var validateError by mutableStateOf<String?>(null)

    private fun updateMoves(newMoves: List<StockMove>) {
        moves = newMoves
        allMovesCompleted = newMoves.isNotEmpty() && newMoves.all { it.product_done == it.product_qty }
    }

    fun fetchMoves(url: String,db: String, uid: Int, pass: String) {
        viewModelScope.launch {
            isLoading = true
            Log.d("ODOO_CHECK" , "Iniciando fetchMoves...")
            try {
                val result = repositoryMove.getMovesGrouped(url, db , uid , pass)
                Log.d("ODOO_CHECK" , "Moves recibidos: ${result.size}")
                updateMoves(result)

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

                val updatedMoves = repositoryMove.getMovesGrouped(url, db, uid, pass)
                updateMoves(updatedMoves)

            } catch (e: IllegalArgumentException) {
                moveLineError = e.message
            } catch (e: Exception) {
                moveLineError = "Error de conexión: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }


    fun validateAssignedPickings(url: String, db: String, uid: Int, pass: String) {
        viewModelScope.launch {
            isLoading = true
            validateSuccess = null
            validateError = null
            try {
                repositoryMove.validateAssignedPickings(url, db, uid, pass)
                validateSuccess = true
            } catch (e: IllegalArgumentException) {
                validateError = e.message
            } catch (e: Exception) {
                validateError = "Error al validar: ${e.message}"

            } finally {
                isLoading = false
            }
        }
    }

}