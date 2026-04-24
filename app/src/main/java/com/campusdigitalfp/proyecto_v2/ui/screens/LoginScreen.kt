package com.campusdigitalfp.proyecto.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campusdigitalfp.proyecto_v2.ui.viewmodel.AuthViewModel


@Composable
fun LoginScreen(navController: NavController , authViewModel: AuthViewModel = viewModel()) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 1. ESCUCHADOR DE NAVEGACIÓN: Reacciona cuando authViewModel.uid cambia
    LaunchedEffect(authViewModel.uid) {
        authViewModel.uid?.let { id ->
            if (id > 0) {
                Log.d("LOGIN_SUCCESS" , "Navegando con UID: $id")
                navController.navigate("main/$id") {
                    popUpTo("login") { inclusive = true } // Evita volver atrás al login
                }
            } else if (id == 0) {
                errorMessage = "Credenciales incorrectas"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) ,
        verticalArrangement = Arrangement.Center ,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Iniciar Sesión" , style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email ,
            onValueChange = { email = it } ,
            label = { Text("Correo electrónico") } ,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email) ,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password ,
            onValueChange = { password = it } ,
            label = { Text("Contraseña") } ,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password) ,
            visualTransformation = PasswordVisualTransformation() ,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. BOTÓN DE LOGIN
        Button(
            onClick = {
                errorMessage = null // Limpiamos errores previos
                authViewModel.fetchUser("prueba" , email , password)
                // IMPORTANTE: Aquí NO ponemos navController.navigate
            } ,
            modifier = Modifier.fillMaxWidth() ,
            enabled = !authViewModel.isLoading // Bloquea el botón si está cargando
        ) {
            if (authViewModel.isLoading) {
                CircularProgressIndicator(size = 20.dp , color = Color.White)
            } else {
                Text("Iniciar Sesión")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. BOTÓN DE INVITADO
        Button(
            onClick = {
                navController.navigate("main/0")
            } ,
            modifier = Modifier.fillMaxWidth() ,
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
        ) {
            Text("Acceder como invitado")
        }
// Campo de texto con la ip para poder modificarla
        var texto by remember {
            mutableStateOf("http://192.168.1.249:8069/")
        }
        var visible by remember {
            mutableStateOf(false)
        }

        if (visible) {
            Button(onClick = { visible = false }) {
                Text("-")
            }

            OutlinedTextField(
                value = texto ,
                onValueChange = { texto = it } ,
                label = { Text("Escribe algo") }
            )
            Button(onClick = {
              //  RetrofitClient.setBaseUrl(texto)
            }) {
                Text("Guardar IP")
            }
        } else {
            Button(onClick = {
                visible = true
            }) {
                Text("+")
            }
        }


        // 4. MENSAJE DE ERROR
        errorMessage?.let {
            Text(
                text = it ,
                color = MaterialTheme.colorScheme.error ,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// Extensión rápida para el indicador de carga dentro del botón
@Composable
fun CircularProgressIndicator(size: androidx.compose.ui.unit.Dp , color: Color) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = Modifier.size(size) ,
        color = color ,
        strokeWidth = 2.dp
    )
}
