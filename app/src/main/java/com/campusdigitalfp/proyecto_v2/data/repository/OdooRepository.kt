package com.campusdigitalfp.proyecto_v2.data.repository

import com.campusdigitalfp.proyecto_v2.data.network.OdooClient
import com.campusdigitalfp.proyecto_v2.domain.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
// IMPORTS CRÍTICOS PARA EL MAPEO
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.contentOrNull

class OdooRepository(private val client: OdooClient) {

    private val db = "prueba"
    private val user = "1@1.com"
    private val pass = "111111"

    suspend fun fetchInitialData(): OdooDataPackage = coroutineScope {
        val uid = client.authenticate(db, user, pass)

        val lotsDef = async { client.searchRead(db, uid, pass, "stock.lot", listOf("name")) }
        val partnersDef = async { client.searchRead(db, uid, pass, "res.partner", listOf("name", "email")) }
        val pickingsDef = async { client.searchRead(db, uid, pass, "stock.picking", listOf("name", "state")) }

        OdooDataPackage(
            lots = lotsDef.await().map { item ->
                val obj = item.jsonObject
                StockLot(
                    id = obj["id"]?.jsonPrimitive?.int ?: 0,
                    name = obj["name"]?.jsonPrimitive?.content ?: "Sin nombre"
                )
            },
            partners = partnersDef.await().map { item ->
                val obj = item.jsonObject
                ResPartner(
                    id = obj["id"]?.jsonPrimitive?.int ?: 0,
                    name = obj["name"]?.jsonPrimitive?.content ?: "Sin nombre",
                    email = obj["email"]?.jsonPrimitive?.contentOrNull
                )
            },
            pickings = pickingsDef.await().map { item ->
                val obj = item.jsonObject
                StockPicking(
                    id = obj["id"]?.jsonPrimitive?.int ?: 0,
                    name = obj["name"]?.jsonPrimitive?.content ?: "Sin nombre",
                    state = obj["state"]?.jsonPrimitive?.content ?: "unknown"
                )
            }
        )
    }
}