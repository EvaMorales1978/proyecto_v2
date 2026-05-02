package com.campusdigitalfp.proyecto_v2.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

import android.util.Log
import com.campusdigitalfp.proyecto_v2.data.repository.OdooRepositoryPicking
import com.campusdigitalfp.proyecto_v2.domain.model.StockPicking



class PickingViewModel : ViewModel() {

    private val repositoryPicking = OdooRepositoryPicking()
  //  private val repository = OdooRepository()

    var pickings by mutableStateOf<List<StockPicking>>(emptyList())
    var isLoading by mutableStateOf(false)
    var moveLineError by mutableStateOf<String?>(null)
    var moveLineResult by mutableStateOf<Map<String, Any>?>(null)

    fun fetchPickings(url: String,db: String, uid: Int, pass: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                Log.d("ODOO_CHECK", "Autenticación exitosa. UID: $uid")
                val result = repositoryPicking.getPickings(url , db, uid, pass)
                Log.d("ODOO_CHECK", "Pickings recibidos: ${result.size}")
                pickings = result

            } catch (e: Exception) {
                Log.e("ODOO_CHECK", "Error en la petición: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
                Log.d("ODOO_CHECK", "Finalizado fetchPickings. Estado de pickings: ${pickings.size}")
            }
        }
    }

    fun processScannedLot(
        url: String,
        db: String,
        uid: Int,
        pass: String,
        pickingId: Int,
        lotName: String
    ) {
        viewModelScope.launch {
            isLoading = true
            moveLineError = null
            try {
                val result = repositoryPicking.UpdateMoveLine(
                    url, db, uid, pass,
                    pickingId, lotName
                )
                moveLineResult = result
                fetchPickings(url, db, uid, pass)
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