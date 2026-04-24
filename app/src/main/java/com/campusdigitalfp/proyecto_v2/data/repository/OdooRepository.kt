package com.campusdigitalfp.proyecto_v2.data.repository

import com.campusdigitalfp.proyecto_v2.data.network.OdooClient
import com.campusdigitalfp.proyecto_v2.domain.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*

class OdooRepository(private val client: OdooClient) {

    private val db = "prueba"
    private val user = "eva@eva.com"
    private val pass = "111111"

    // Función auxiliar para leer Strings de Odoo de forma segura (evita el error del 'false')
    private fun JsonElement?.asString(): String {
        return if (this is JsonPrimitive && this.isString) this.content else ""
    }

    // Función auxiliar para leer Ints de Odoo de forma segura
    private fun JsonElement?.asInt(): Int {
        return try {
            this?.jsonPrimitive?.int ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun fetchInitialData(uid: Int): OdooDataPackage = coroutineScope {
       // val uid = client.authenticate(db , user , pass)


        val pickingDomain = buildJsonArray {
            add(buildJsonArray { // <--- Este es el corchete extra que descubrimos en Postman
                add(buildJsonArray {
                    add("state")
                    add("=")
                    add("assigned")
                })
                add(buildJsonArray {
                    add("picking_type_id")
                    add("=")
                    add(2)
                })
            })
        }

        val lotsDef =
            async { client.searchRead(db , uid , pass , "stock.lot" , listOf("id" , "name")) }
        val partnersDef = async {
            client.searchRead(
                db ,
                uid ,
                pass ,
                "res.partner" ,
                listOf("id" , "name" , "street" , "city")
            )
        }
        val productDef =
            async { client.searchRead(db , uid , pass , "product.product" , listOf("id" , "name")) }
        val linesDef = async {
            client.searchRead(
                db ,
                uid ,
                pass ,
                "stock.move.line" ,
                listOf("id" , "product_id" , "reserved_qty" , "qty_done" , "lot_name" , "state")
            )
        }
        val pickingsDef = async {
            client.searchRead(
                db ,
                uid ,
                pass ,
                "stock.picking" ,
                listOf("id" , "name" , "partner_id" , "state" , "move_line_ids")
                , domain = pickingDomain
            )
        }

        OdooDataPackage(
            lots = lotsDef.await().map { item ->
                val obj = item.jsonObject
                StockLot(
                    id = obj["id"].asInt() ,
                    name = obj["name"].asString()
                )
            } ,
            partners = partnersDef.await().map { item ->
                val obj = item.jsonObject
                ResPartner(
                    id = obj["id"].asInt() ,
                    name = obj["name"].asString() ,
                    street = obj["street"].asString().ifEmpty { "Sin calle" } ,
                    city = obj["city"].asString().ifEmpty { "Sin ciudad" }
                )
            } ,
            products = productDef.await().map { item ->
                val obj = item.jsonObject
                Product(
                    id = obj["id"].asInt() ,
                    name = obj["name"].asString()
                )
            } ,
            lines = linesDef.await().map { item ->
                val obj = item.jsonObject
                val prodArray = obj["product_id"]?.jsonArray
                StockMoveLine(
                    id = obj["id"].asInt() ,
                    product_id = Product(
                        id = prodArray?.get(0).asInt() ,
                        name = prodArray?.get(1).asString()
                    ) ,
                    reserved_qty = obj["reserved_qty"].asInt() ,
                    qty_done = obj["qty_done"].asInt() ,
                    lot_name = obj["lot_name"].asString() ,
                    state = obj["state"].asString()
                )
            } ,
            pickings = pickingsDef.await().map { item ->
                val obj = item.jsonObject
                val partArray = obj["partner_id"]?.jsonArray
                StockPicking(
                    id = obj["id"].asInt() ,
                    name = obj["name"].asString() ,
                    partner_id = partArray?.get(0).asInt() ,
                    state = obj["state"].asString() ,
                    // Mantenemos el dummy pero ahora con el mapeo seguro
                    move_line_ids = StockMoveLine(0 , Product(0 , "") , 0 , 0 , "" , "")
                )
            }
        )
    }
}