package com.campusdigitalfp.proyecto.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.campusdigitalfp.proyecto.screens.LoginScreen
import com.campusdigitalfp.proyecto.screens.MainScreen
import com.campusdigitalfp.proyecto_v2.data.network.OdooClient
import com.campusdigitalfp.proyecto_v2.data.repository.OdooRepository
import com.campusdigitalfp.proyecto_v2.ui.viewmodel.MainViewModel
import com.campusdigitalfp.proyecto_v2.ui.viewmodel.MainViewModelFactory


@Composable
fun Navigation(odooClient: OdooClient) {
    val navController = rememberNavController()
    val repository = OdooRepository(odooClient)
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController ) }
        composable(
            route = "main/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.IntType })
        ) { backStackEntry ->
            // Extraemos el uid de la ruta
            val uid = backStackEntry.arguments?.getInt("uid") ?: 0

            val repository = OdooRepository(odooClient)

            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(repository, uid)
            )

            // SOLUCIÓN: Pasamos los 3 parámetros exactos
            MainScreen(
                viewModel = viewModel,
                navController = navController,
                uid = uid // <--- Faltaba pasar este tercer parámetro aquí
            )
        }
    //    composable("about") { AboutScreen(navController) }
    //    composable("product") { ListaProductosScreen(navController) }
    //    composable("scanerorigen") { ScanerScreenOrigen(navController) }
    //    composable("delivery") { PickingListScreen(navController) }
    }
}