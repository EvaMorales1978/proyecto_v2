package com.campusdigitalfp.proyecto.navigation

import android.net.Uri
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
import com.campusdigitalfp.proyecto_v2.ui.screens.ScanerScreenOrigen
import com.campusdigitalfp.proyecto_v2.ui.viewmodel.MainViewModel
import com.campusdigitalfp.proyecto_v2.ui.viewmodel.MainViewModelFactory


@Composable
fun Navigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController ) }
        composable(
            route = "main/{url}/{id}/{password}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("id") { type = NavType.IntType },
                navArgument("password") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val urlOriginal = Uri.decode(encodedUrl)

            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val pass = backStackEntry.arguments?.getString("password") ?: ""

            MainScreen(navController, urlOriginal, id, pass)
        }
        composable(
            route = "scanerorigen/{url}/{id}/{password}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("id") { type = NavType.IntType },
                navArgument("password") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val urlOriginal = Uri.decode(encodedUrl)

            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val pass = backStackEntry.arguments?.getString("password") ?: ""

            ScanerScreenOrigen(navController, urlOriginal, id, pass)
        }


       // composable("scanerorigen") { ScanerScreenOrigen(navController) }


    //    composable("about") { AboutScreen(navController) }
    //    composable("product") { ListaProductosScreen(navController) }

    //    composable("delivery") { PickingListScreen(navController) }
    }
}