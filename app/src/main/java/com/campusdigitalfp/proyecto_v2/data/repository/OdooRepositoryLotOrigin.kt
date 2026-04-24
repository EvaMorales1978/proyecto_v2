package com.campusdigitalfp.proyecto_v2.data.repository

import android.util.Log
import com.campusdigitalfp.proyecto_v2.data.network.OdooClient
import kotlinx.serialization.json.*

class OdooRepositoryLotOrigin {

    suspend fun getLotOrigin(url: String, db: String, uid: Int, pass: String, lot: String): String {
        // 1. Instanciar el cliente de Ktor
        val client = OdooClient(url)

        // 2. Preparar los argumentos específicos de la función de Odoo
        // get_productions_by_lot_name suele esperar una lista de nombres o un string
        val functionArgs = buildJsonArray {
            add(lot)
        }

        return try {
            // 3. Llamar al nuevo método genérico callKw
            val result = client.callKw(
                db = db,
                uid = uid,
                pass = pass,
                model = "stock.lot",
                method = "get_productions_by_lot_name",
                args = functionArgs
            )

            // 4. Procesar el resultado
            // Si el resultado es un String, lo devolvemos.
            // Si es un objeto/lista, lo convertimos a String JSON.
            if (result is JsonPrimitive) {
                result.content
            } else {
                result.toString() // Esto devuelve el JSON formateado como String
            }

        } catch (e: Exception) {
            Log.e("ODOO_FATAL", "Error en getLotOrigin: ${e.message}")
            "ERROR: ${e.message ?: "Error desconocido"}"
        }
    }
}