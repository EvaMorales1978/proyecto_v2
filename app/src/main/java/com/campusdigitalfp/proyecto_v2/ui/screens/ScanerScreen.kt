package com.campusdigitalfp.proyecto_v2.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.campusdigitalfp.proyecto_v2.data.repository.OdooRepositoryLotOrigin
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

@Composable
fun ScanerScreenOrigen(
    navController: NavController,
    url: String,
    uid: Int,
    pass: String,
    db: String
) {

    val context = LocalContext.current

    val repository = remember { OdooRepositoryLotOrigin() }
    val scope = rememberCoroutineScope()

    var textoEscaneado by remember { mutableStateOf("") }
    var nEscaneo by remember { mutableIntStateOf(0) }

    var origen by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var mostrarScanner by remember { mutableStateOf(false) }
    //   val db = "prueba"

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
        onResult = { result ->
            if (result.contents != null) {
                textoEscaneado = result.contents
                nEscaneo++
                val partes = textoEscaneado.split("-", limit = 2)
                val lotName = if (partes.size >= 2) partes[1] else ""

                if (lotName.isNotBlank()) {
                    scope.launch {
                        isLoading = true
                        origen = repository.getLotOrigin(
                            url,
                            db = db,
                            uid = 2,
                            pass = "111111",
                            lot = lotName
                        )
                        isLoading = false
                    }
                } else {
                    Toast.makeText(context, "QR inválido", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    )
    Scaffold(
        topBar = {
            MainTopBarTexto("Origen")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(
                onClick = {
                    mostrarScanner = true
                    /* val options = ScanOptions().apply {
                         setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                         setPrompt("Escanea un código QR")
                         setBeepEnabled(true)
                         setOrientationLocked(false)
                     }
                     scanLauncher.launch(options)*/
                },
                enabled = !mostrarScanner,
                modifier = Modifier.size(200.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "QR Icon",
                    modifier = Modifier.size(120.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Escanear Código QR",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Escanea el QR que aparece en el envase y podrás ver el origen de tus huevos.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (mostrarScanner) {
                val procesando = remember { mutableStateOf(false) }
                ContinuousScanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    onScan = { codigo ->
                        if (!procesando.value) {
                            procesando.value = true
                            mostrarScanner = false
                            textoEscaneado = codigo
                            nEscaneo++

                            val partes_escaneo = codigo.split("-", limit = 2)
                            if (partes_escaneo.size < 2) {
                                Toast.makeText(context, "Código no válido: $codigo", Toast.LENGTH_SHORT).show()
                                return@ContinuousScanner
                            }
                            val lotName = partes_escaneo[1].trim()

                            if (lotName.isNotBlank()) {
                                scope.launch {
                                    isLoading = true
                                    origen = repository.getLotOrigin(
                                        url,
                                        db = db,
                                        uid = 2,
                                        pass = "111111",
                                        lot = lotName
                                    )
                                    isLoading = false
                                }
                            } else {
                                Toast.makeText(context, "QR inválido", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            if (!mostrarScanner) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (origen.isNotBlank()) {
                    val cleanUrl = origen.trim().trim('"')
                    AsyncImage(
                        model = cleanUrl,
                        contentDescription = "Origen del producto",
                        modifier = Modifier.size(600.dp)
                    )
                } else if (nEscaneo > 0) {
                    Text(
                        text = "El código escaneado no es correcto",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }


        }
    }
}