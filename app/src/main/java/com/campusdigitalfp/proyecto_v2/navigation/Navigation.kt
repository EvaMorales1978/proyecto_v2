package com.campusdigitalfp.proyecto.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.campusdigitalfp.proyecto.screens.LoginScreen
import com.campusdigitalfp.proyecto.screens.MainScreen
import com.campusdigitalfp.proyecto_v2.ui.screens.AboutScreen
import com.campusdigitalfp.proyecto_v2.ui.screens.PickingListScreen
import com.campusdigitalfp.proyecto_v2.ui.screens.ProductosListScreen
import com.campusdigitalfp.proyecto_v2.ui.screens.ScanerScreenOrigen



@Composable
fun Navigation() {
    val navController = rememberNavController()
    val db = "prueba"

    NavHost(navController = navController, startDestination = "login/$db") {
        composable(
            route = "login/{db}",
            arguments = listOf(
                navArgument("db") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val db = backStackEntry.arguments?.getString("db") ?: ""
            LoginScreen(navController, db)
        }
        composable(
            route = "main/{url}/{id}/{password}/{db}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("id") { type = NavType.IntType },
                navArgument("password") { type = NavType.StringType },
                navArgument("db") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val urlOriginal = Uri.decode(encodedUrl)

            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val pass = backStackEntry.arguments?.getString("password") ?: ""
            val db = backStackEntry.arguments?.getString("db") ?: ""

            MainScreen(navController, urlOriginal, id, pass, db)
        }
        composable(
            route = "scanerorigen/{url}/{id}/{password}/{db}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("id") { type = NavType.IntType },
                navArgument("password") { type = NavType.StringType },
                navArgument("db") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val urlOriginal = Uri.decode(encodedUrl)

            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val pass = backStackEntry.arguments?.getString("password") ?: ""
            val db = backStackEntry.arguments?.getString("db") ?: ""

            ScanerScreenOrigen(navController, urlOriginal, id, pass,db)
        }


        composable(
            route = "delivery/{url}/{id}/{password}/{db}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("id") { type = NavType.IntType },
                navArgument("password") { type = NavType.StringType },
                navArgument("db") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val urlOriginal = Uri.decode(encodedUrl)

            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val pass = backStackEntry.arguments?.getString("password") ?: ""
            val db = backStackEntry.arguments?.getString("db") ?: ""

            PickingListScreen(navController, urlOriginal, id, pass, db)
        }

        composable(
            route = "product/{url}/{id}/{password}/{db}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("id") { type = NavType.IntType },
                navArgument("password") { type = NavType.StringType },
                navArgument("db") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val urlOriginal = Uri.decode(encodedUrl)

            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val pass = backStackEntry.arguments?.getString("password") ?: ""
            val db = backStackEntry.arguments?.getString("db") ?: ""

            ProductosListScreen(navController, urlOriginal, id, pass,db)
        }

        composable("about") { AboutScreen(navController) }

    }
}