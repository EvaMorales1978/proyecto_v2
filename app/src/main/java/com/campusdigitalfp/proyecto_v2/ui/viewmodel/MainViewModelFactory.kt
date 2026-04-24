package com.campusdigitalfp.proyecto_v2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.campusdigitalfp.proyecto_v2.data.repository.OdooRepository

class MainViewModelFactory(
    private val repository: OdooRepository,
    private val uid: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository, uid) as T
    }
}