package com.campusdigitalfp.proyecto_v2.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campusdigitalfp.proyecto_v2.domain.model.StockPicking
import com.campusdigitalfp.proyecto_v2.ui.viewmodel.PickingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun PickingListScreen(
    navController: NavController,
    url: String,
    uid: Int,
    pass: String,
    viewModel: PickingViewModel = viewModel()
) {
    var scannedLot by remember { mutableStateOf<String?>(null) }
    var expandedPickingId by remember { mutableStateOf<Int?>(null) }
    var mostrarSoloPendientes by remember { mutableStateOf(false) }
    val db = "prueba"

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.fetchPickings(url, db, uid, pass) }

    LaunchedEffect(scannedLot) {
        scannedLot?.let { lot ->
            val pickingId = expandedPickingId ?: return@let
            viewModel.processScannedLot(url, db, uid, pass, pickingId, lot)
            scannedLot = null
        }
    }

    LaunchedEffect(viewModel.validateSuccess) {
        if (viewModel.validateSuccess == true) {
            snackbarHostState.showSnackbar("Picking validado correctamente")
            viewModel.validateSuccess = null
        }
    }

    LaunchedEffect(viewModel.validateError) {
        viewModel.validateError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.validateError = null
        }
    }

    val listaFiltrada = if (mostrarSoloPendientes) {
        viewModel.pickings.filter { it.state == "assigned" }
    } else {
        viewModel.pickings
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { MainTopBarTexto("Entregas") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        BotonFiltroSimplePicking(
                            texto = "Todos",
                            seleccionado = !mostrarSoloPendientes,
                            onClick = { mostrarSoloPendientes = false }
                        )
                        BotonFiltroSimplePicking(
                            texto = "Pendientes",
                            seleccionado = mostrarSoloPendientes,
                            onClick = { mostrarSoloPendientes = true }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listaFiltrada) { picking ->
                    MoveItem(
                        picking = picking,
                        expanded = expandedPickingId == picking.id,
                        onClick = {
                            expandedPickingId =
                                if (expandedPickingId == picking.id) null
                                else picking.id
                        },
                        onLotScanned = { lot -> scannedLot = lot },
                        onValidate = { pickingId ->
                            viewModel.validatePicking(url, db, uid, pass, pickingId)
                        },
                        isLoading = viewModel.isLoading
                    )
                }
            }
        }
    }
}

@Composable
fun BotonFiltroSimplePicking(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    val fondo = if (seleccionado) MaterialTheme.colorScheme.primary else Color.LightGray
    val textoColor = if (seleccionado) Color.White else Color.Black

    Box(
        modifier = Modifier
            .background(fondo, shape = RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = texto, color = textoColor, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun MoveItem(
    picking: StockPicking,
    expanded: Boolean,
    onClick: () -> Unit,
    onLotScanned: (String) -> Unit,
    onValidate: (Int) -> Unit,
    isLoading: Boolean
) {
    var scannerLocked by remember { mutableStateOf(false) }
    val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isCompleted = picking.move_line_ids.isNotEmpty() &&
            picking.move_line_ids.all { it.qty_done == it.reserved_qty }
    val cardColor = when (picking.state) {
        "assigned" -> Color(0xFFFFF3CD) // amarillo suave
        "done"     -> Color(0xFFD4EDDA) // verde suave
        else       -> Color.White
    }

    if (expanded) {
        ContinuousScanner(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) { contenido ->
            if (scannerLocked) return@ContinuousScanner
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            scannerLocked = true

            val lotName = contenido.split("-", limit = 2)[1].trim()
            onLotScanned(lotName)

            Toast.makeText(context, "Escaneado lote", Toast.LENGTH_SHORT).show()

            scope.launch {
                delay(2000)
                scannerLocked = false
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).background(cardColor, shape = RoundedCornerShape(8.dp))) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pedido: ${picking.name}" ,
                    style = MaterialTheme.typography.bodyLarge ,
                    fontWeight = FontWeight.Bold
                )


                if (isCompleted) {
                    IconButton(
                        onClick = { onValidate(picking.id) },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "✅",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }

                }
            }


            picking.partner_id?.let {
                Text(
                    text = it.name + " - " + it.street + " (" + it.city + ")",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                picking.move_line_ids.forEach { line ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = line.product_id.name,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${line.qty_done} / ${line.reserved_qty}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}