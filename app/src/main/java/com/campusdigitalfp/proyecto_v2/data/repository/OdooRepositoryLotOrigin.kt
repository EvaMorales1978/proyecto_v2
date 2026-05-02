package com.campusdigitalfp.proyecto_v2.data.repository

import android.util.Log
import com.campusdigitalfp.proyecto_v2.data.network.OdooClient
import kotlinx.serialization.json.*

class OdooRepositoryLotOrigin {

    suspend fun getLotOrigin(url: String, db: String, uid: Int, pass: String, lot: String): String {
        val client = OdooClient(url)

        val functionArgs = buildJsonArray {
            add(lot)
        }

        return try {
            val result = client.callKw(
                db = db,
                uid = uid,
                pass = pass,
                model = "stock.lot",
                method = "get_productions_by_lot_name",
                args = functionArgs
            )

            if (result is JsonPrimitive) {
                result.content
            } else {
                result.toString()
            }

        } catch (e: Exception) {
            Log.e("ODOO_FATAL", "Error en getLotOrigin: ${e.message}")
            "ERROR: ${e.message ?: "Error desconocido"}"
        }
    }
}