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

    fun fetchPickings(url: String,db: String, uid: Int, pass: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                if (uid != null && uid > 0) {
                    Log.d("ODOO_CHECK", "Autenticación exitosa. UID: $uid")
                    val result = repositoryPicking.getPickings(url , db, uid, pass)
                    Log.d("ODOO_CHECK", "Pickings recibidos: ${result.size}")
                    pickings = result
                } else {
                    Log.e("ODOO_CHECK", "Fallo de autenticación: UID es nulo o 0")
                }
            } catch (e: Exception) {
                Log.e("ODOO_CHECK", "Error en la petición: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
                Log.d("ODOO_CHECK", "Finalizado fetchPickings. Estado de pickings: ${pickings.size}")
            }
        }
    }
}