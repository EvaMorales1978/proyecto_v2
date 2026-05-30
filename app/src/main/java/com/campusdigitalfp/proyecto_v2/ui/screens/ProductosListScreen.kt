package com.campusdigitalfp.proyecto_v2.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import com.campusdigitalfp.proyecto_v2.domain.model.StockMove
import com.campusdigitalfp.proyecto_v2.ui.viewmodel.MoveViewModel

@Composable
fun ProductosListScreen(
    navController: NavController ,
    url: String ,
    uid: Int ,
    pass: String ,
    db: String,
    viewModel: MoveViewModel = viewModel()
) {
   // val db = "prueba"
    val scope = rememberCoroutineScope()
    val listaDeMoves = viewModel.moves
    var scannerLocked by remember { mutableStateOf(false) }
    val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION , 100)
    val context = LocalContext.current
    var scannedLot by remember { mutableStateOf<String?>(null) }
    var validated by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) { viewModel.fetchMoves(url , db , uid , pass) }

    LaunchedEffect(scannedLot) {
        Log.e("ODOO_lauch" , "Entra")

        scannedLot?.let { lot ->
            viewModel.processScannedLotMove(url , db , uid , pass , lot)
            scannedLot = null
        }
    }

    Scaffold(
        topBar = {
            MainTopBarTexto("Productos")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // otra pantalla q inserto en esta
            if (!viewModel.allMovesCompleted) {
                ContinuousScanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) { contenido ->
                    if (scannerLocked) return@ContinuousScanner
                    tone.startTone(
                        ToneGenerator.TONE_PROP_BEEP ,
                        150
                    )
                    scannerLocked = true

                    val lotName = contenido.split("-" , limit = 2)[1].trim()
                    scannedLot = lotName

                    Toast.makeText(
                        context ,
                        "Sumado: $lotName" ,
                        Toast.LENGTH_SHORT
                    ).show()

                    scope.launch {
                        delay(2000)
                        scannerLocked = false
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp) ,
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Text(
                        "Nuestros Productos" ,
                        color = MaterialTheme.colorScheme.primary ,
                        style = MaterialTheme.typography.headlineMedium ,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                items(listaDeMoves) { moveItem ->
                    MoveItem(moveItem)
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth() ,
                        horizontalArrangement = Arrangement.Center ,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (viewModel.allMovesCompleted) {
                            if (!validated) {
                                OutlinedButton(
                                onClick = {
                                        validated = true
                                        viewModel.validateAssignedPickings(url, db, uid, pass)
                                        Toast.makeText(
                                            context ,
                                            "Pulsado Check" ,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                } ,
                                modifier = Modifier.wrapContentWidth() ,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check ,
                                    contentDescription = "Confirmar Escaneo" ,
                                    modifier = Modifier.size(120.dp) ,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }


                            }

                        }
                        /*Spacer(modifier = Modifier.width(16.dp))
                        OutlinedButton(
                            onClick = { navController.popBackStack() } ,
                            modifier = Modifier.wrapContentWidth() ,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack ,
                                contentDescription = "Escanear producto" ,
                                modifier = Modifier.size(120.dp) ,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }*/
                    }

                }
            }
        }
    }
}

@Composable
fun MoveItem(move: StockMove) {

    val statusColor = when {
        move.product_done > move.product_qty -> Color(0xFFFFEBEE)
        move.product_done < move.product_qty -> Color(0xFFFFF3E0)
        else -> Color(0xFFE8F5E9)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp) ,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp , vertical = 4.dp)
                .background(statusColor)
                .fillMaxWidth()
                .padding(16.dp) ,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = move.product.name , style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = move.product_done.toString() + " - " ,
                style = MaterialTheme.typography.headlineSmall ,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = move.product_qty.toString() ,
                style = MaterialTheme.typography.headlineSmall ,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
