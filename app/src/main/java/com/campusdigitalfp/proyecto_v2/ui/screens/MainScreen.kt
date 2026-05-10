package com.campusdigitalfp.proyecto.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.campusdigitalfp.proyecto_v2.R


@Composable
fun MainScreen(navController: NavController , url: String , uid: Int , pass: String) {

    Scaffold(
        topBar = {
            MainTopBar(navController = navController)
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp) ,
            verticalArrangement = Arrangement.Center ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Selecciona una opción" ,
                fontSize = 24.sp ,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = {
                    val encodedUrl = Uri.encode(url)
                    navController.navigate("scanerorigen/$encodedUrl/$uid/$pass")
                } ,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.buscar_procedencia))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uid != 0) {
                Button(
                    onClick = {
                        val encodedUrl = Uri.encode(url)
                        navController.navigate("product/$encodedUrl/$uid/$pass")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Mercancía Necesaria")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val encodedUrl = Uri.encode(url)
                        navController.navigate("delivery/$encodedUrl/$uid/$pass")
                    } ,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) ,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.reparto))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
            Button(
                onClick = { navController.navigate("about") } ,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.acerca_de))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(navController: NavController) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Huevos de gallinas felices") } ,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer ,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) ,
        actions = {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert ,
                    contentDescription = "Mostrar menú"
                )
            }

            DropdownMenu(
                expanded = showMenu ,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.buscar_procedencia)) } ,
                    leadingIcon = {
                        Icon(
                            Icons.Default.QrCodeScanner ,
                            contentDescription = null
                        )
                    } ,
                    onClick = {
                        showMenu = false
                        navController.navigate("scanerorigen")
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.mercanc_a_necesaria)) } ,
                    leadingIcon = { Icon(Icons.Default.Badge , contentDescription = null) } ,
                    onClick = {
                        showMenu = false
                        navController.navigate("product")

                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reparto)) } ,
                    leadingIcon = {
                        Icon(
                            Icons.Default.DirectionsCar ,
                            contentDescription = null
                        )
                    } ,
                    onClick = {
                        showMenu = false
                        navController.navigate("delivery")

                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.acerca_de)) } ,
                    leadingIcon = { Icon(Icons.Default.Info , contentDescription = null) } ,
                    onClick = {
                        showMenu = false
                        navController.navigate("about")
                    }
                )

                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Salir") } ,
                    leadingIcon = { Icon(Icons.Default.ExitToApp , contentDescription = null) } ,
                    onClick = {
                        showMenu = false
                        navController.navigate("about")
                    }
                )
            }
        }
    )
}