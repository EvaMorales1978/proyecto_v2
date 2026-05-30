package com.campusdigitalfp.proyecto_v2.data.repository

import android.util.Log
import com.campusdigitalfp.proyecto_v2.data.network.OdooClient
import com.campusdigitalfp.proyecto_v2.domain.model.Product
import com.campusdigitalfp.proyecto_v2.domain.model.ResPartner
import com.campusdigitalfp.proyecto_v2.domain.model.StockMoveLine
import com.campusdigitalfp.proyecto_v2.domain.model.StockPicking
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import java.io.EOFException
import java.net.ProtocolException
import java.net.SocketTimeoutException


class OdooRepositoryPicking {

    fun Any?.toIntSafe(): Int = when (this) {
        is Number -> this.toInt()
        is String -> this.toDoubleOrNull()?.toInt() ?: 0
        is JsonPrimitive -> this.content.toDoubleOrNull()?.toInt() ?: 0
        else -> 0
    }

    fun Any?.toStringSafe(): String = when (this) {
        is JsonPrimitive -> this.content
        else -> this?.toString() ?: ""
    }

    suspend fun getPickings(
        url: String ,
        db: String ,
        uid: Int ,
        pass: String
    ): List<StockPicking> {
        val client = OdooClient(url)

        val today = java.text.SimpleDateFormat("yyyy-MM-dd" , java.util.Locale.getDefault())
            .format(java.util.Date())

        val pickingDomain = buildJsonArray {
            add(buildJsonArray {
                add(JsonPrimitive("|"))
                add(JsonPrimitive("&"))
                add(JsonPrimitive("&"))
                add(buildJsonArray { add("state"); add("="); add("done") })
                add(buildJsonArray { add("picking_type_id"); add("="); add(2) })
                add(buildJsonArray { add("date_done"); add(">="); add("$today 00:00:00") })
                add(JsonPrimitive("&"))
                add(buildJsonArray { add("state"); add("="); add("assigned") })
                add(buildJsonArray { add("picking_type_id"); add("="); add(2) })
            })
        }


        var retryCount = 0
        val maxRetries = 3

        while (retryCount < maxRetries) {
            try {
                val allPickings = mutableListOf<StockPicking>()
                val pickingsDef = client.searchRead(
                    db , uid , pass ,
                    "stock.picking" ,
                    listOf("id" , "name" , "partner_id" , "state" , "move_line_ids") ,
                    domain = pickingDomain
                )

                Log.d("ODOO_FLOW" , "pickingsDef size: ${pickingsDef.size}")

                for (item in pickingsDef) {
                    val pickingMap = item as? Map<* , *> ?: continue
                    Log.d("ODOO_FLOW" , "pickingMap keys: ${pickingMap.keys}")
                    Log.d("ODOO_FLOW" , "pickingMap: $pickingMap")

                    val pickingId = pickingMap["id"].toIntSafe()
                    Log.d("ODOO_FLOW" , "pickingId: $pickingId")

                    val rawPartner = pickingMap["partner_id"]
                    val partnerId = when (rawPartner) {
                        is JsonArray -> rawPartner[0].toIntSafe()
                        is List<*> -> rawPartner[0].toIntSafe()
                        else -> null
                    }
                    Log.d("ODOO_FLOW" , "partnerId: $partnerId")

                    var fullPartner: ResPartner? = null

                    if (partnerId != null && partnerId != 0) {
                        var pRetry = 0
                        var pSuccess = false

                        while (pRetry < 5 && !pSuccess) {
                            try {
                                val partnerDomain = buildJsonArray {
                                    add(buildJsonArray {
                                        add(buildJsonArray {
                                            add("id")
                                            add("=")
                                            add(partnerId)
                                        })
                                    })
                                }
                                Log.d(
                                    "ODOO_FLOW" ,
                                    "partnerDomain: $partnerDomain"
                                ) // debe imprimir [[\"id\",\"=\",3]]

                                val partnersDef = client.searchRead(
                                    db , uid , pass ,
                                    "res.partner" ,
                                    listOf("id" , "name" , "street" , "city" , "sequence_route") ,
                                    domain = partnerDomain
                                )
                                Log.d("ODOO_FLOW" , "partnersDef size: ${partnersDef?.size}")

                                val pMap = partnersDef?.getOrNull(0) as? Map<* , *>
                                Log.d("ODOO_FLOW" , "pMap: $pMap")

                                if (pMap != null) {
                                    fullPartner = ResPartner(
                                        id = pMap["id"].toIntSafe() ,
                                        name = pMap["name"].toStringSafe() ,
                                        street = pMap["street"].toStringSafe() ,
                                        city = pMap["city"].toStringSafe() ,
                                        sequence_route = pMap["sequence_route"].toIntSafe() ,

                                        )
                                    Log.d("ODOO_FLOW" , "fullPartner: $fullPartner")
                                } else {
                                    Log.w(
                                        "ODOO_FLOW" ,
                                        "Partner $partnerId no encontrado, continuando sin partner"
                                    )
                                }
                                pSuccess = true

                            } catch (e: Exception) {
                                val isNetworkError = e is ProtocolException ||
                                        e is EOFException ||
                                        e is SocketTimeoutException ||
                                        e.cause is ProtocolException
                                if (isNetworkError) {
                                    pRetry++
                                    Log.e("ODOO_FLOW" , "Retry partner $pRetry - ${e.message}")
                                    delay(1000)
                                } else {
                                    Log.e("ODOO_FLOW" , "Error partner no recuperable" , e)
                                    pSuccess = true
                                    break
                                }
                            }
                        }
                    }

                    val movelineList = mutableListOf<StockMoveLine>()
                    var mRetry = 0
                    var mSuccess = false

                    while (mRetry < 5 && !mSuccess) {
                        try {
                            val moveDomain = buildJsonArray {
                                add(buildJsonArray {
                                    add(buildJsonArray {
                                        add("picking_id")
                                        add("=")
                                        add(pickingId)
                                    })
                                })
                            }
                            val linesDef = client.searchRead(
                                db , uid , pass ,
                                "stock.move.line" ,
                                listOf(
                                    "id" ,
                                    "product_id" ,
                                    "reserved_qty" ,
                                    "qty_done" ,
                                    "lot_name" ,
                                    "state"
                                ) ,
                                domain = moveDomain
                            )
                            Log.d(
                                "ODOO_FLOW" ,
                                "linesDef size: ${linesDef?.size} for pickingId: $pickingId"
                            )

                            movelineList.clear()

                            linesDef?.forEach { moveObj ->
                                val mMap = moveObj as? Map<* , *> ?: return@forEach

                                val pData = mMap["product_id"]
                                val product = Product(
                                    id = when (pData) {
                                        is JsonArray -> pData[0].toIntSafe()
                                        is List<*> -> pData[0].toIntSafe()
                                        else -> 0
                                    } ,
                                    name = when (pData) {
                                        is JsonArray -> pData[1].toStringSafe()
                                        is List<*> -> pData[1].toStringSafe()
                                        else -> ""
                                    }
                                )

                                movelineList.add(
                                    StockMoveLine(
                                        id = mMap["id"].toIntSafe() ,
                                        product_id = product ,
                                        reserved_qty = mMap["reserved_qty"].toIntSafe() ,
                                        qty_done = mMap["qty_done"].toIntSafe() ,
                                        state = mMap["state"].toStringSafe() ,
                                        lot_name = ""
                                    )
                                )
                            }

                            mSuccess = true

                        } catch (e: Exception) {
                            val isNetworkError = e is ProtocolException ||
                                    e is EOFException ||
                                    e is SocketTimeoutException ||
                                    e.cause is ProtocolException
                            if (isNetworkError) {
                                mRetry++
                                Log.e(
                                    "ODOO_FLOW" ,
                                    "Retry moves $mRetry ($pickingId) - ${e.message}"
                                )
                                delay(1000)
                            } else {
                                Log.e("ODOO_FLOW" , "Error moves no recuperable ($pickingId)" , e)
                                break
                            }
                        }
                    }

                    allPickings.add(
                        StockPicking(
                            id = pickingId ,
                            name = pickingMap["name"].toStringSafe() ,
                            partner_id = fullPartner ,
                            state = pickingMap["state"].toStringSafe() ,
                            move_line_ids = movelineList
                        )
                    )
                    Log.d("ODOO_FLOW" , "allPickings size ahora: ${allPickings.size}")
                }

                Log.d("ODOO_FLOW" , "return allPickings size: ${allPickings.size}")
                return allPickings
                    .sortedWith(
                        compareBy(
                            { it.partner_id == null } ,
                            { it.partner_id?.sequence_route ?: Int.MIN_VALUE }
                        )
                    )

            } catch (e: Exception) {
                Log.e("ODOO_FLOW" , "Error general retry $retryCount: ${e.message}" , e)
                retryCount++
                delay(1000)
            }
        }
        Log.e("ODOO_FLOW" , "Se agotaron los reintentos, devolviendo lista vacía")
        return emptyList()
    }

    suspend fun UpdateMoveLine(
        url: String ,
        db: String ,
        uid: Int ,
        pass: String ,
        pickingId: Int ,
        lotName: String
    ): Map<String , Any> {
        val client = OdooClient(url)
        //busco el lote y saco su producto
        val lotDomain = buildJsonArray {
            add(buildJsonArray {
                add(buildJsonArray { add("name"); add("="); add(lotName) })
            })
        }
        val lotResult = client.searchRead(
            db , uid , pass ,
            "stock.lot" ,
            listOf("id" , "name" , "product_id") ,
            domain = lotDomain
        )
        val lotMap = lotResult?.getOrNull(0) as? Map<* , *>
            ?: throw IllegalArgumentException("Lote '$lotName' no encontrado")

        val lotId = lotMap["id"].toIntSafe()
        val productId = when (val p = lotMap["product_id"]) {
            is JsonArray -> p[0].toIntSafe()
            is List<*> -> p[0].toIntSafe()
            else -> throw IllegalArgumentException("El lote '$lotName' no tiene producto asociado")
        }
        //busco los stock_move d ese picking y ese producto
        val moveDomain = buildJsonArray {
            add(buildJsonArray {
                add(buildJsonArray { add("picking_id"); add("="); add(pickingId) })
                add(buildJsonArray { add("product_id"); add("="); add(productId) })
            })
        }
        val moveResult = client.searchRead(
            db , uid , pass ,
            "stock.move" ,
            listOf("id" , "location_id" , "location_dest_id" , "product_uom","product_qty" , "quantity_done" ) ,
            domain = moveDomain
        )

        val pendingMoves = moveResult.mapNotNull { it as? Map<*, *> }.filter { moveMap ->
            val productQty = moveMap["product_qty"].toIntSafe()
            val qtyDone    = moveMap["quantity_done"].toIntSafe()
            qtyDone < productQty
        }

        // Si no hay ninguno pendiente, no se puede operar
        if (pendingMoves.isEmpty()) {
            return mapOf(
                "success" to false,
                "message" to "El picking $pickingId not tiene movimientos pendientes para el producto correspondiente al lote '$lotName'."
            )
        }
        val moveMap = pendingMoves.first()
        val moveId  = moveMap["id"].toIntSafe()

        //busco los stock_move_line de ese movimiento
        val moveLineDomain = buildJsonArray {
            add(buildJsonArray {
                add(buildJsonArray { add("move_id"); add("="); add(moveId) })
                add(buildJsonArray { add("lot_id"); add("="); add(lotId) })
            })
        }
        val moveLineResult = client.searchRead(
            db, uid, pass,
            "stock.move.line",
            listOf("id", "lot_id", "qty_done", "reserved_qty", "location_id", "location_dest_id"),
            domain = moveLineDomain
        )
        // sumo uno si la encuentra
        if (moveLineResult.isNotEmpty()) {
            val lineMap = moveLineResult.first() as? Map<*, *>
                ?: throw IllegalArgumentException("No se pudo leer la move.line")
            val lineId  = lineMap["id"].toIntSafe()
            val qtyDone = lineMap["qty_done"].toIntSafe()

            client.write(
                db, uid, pass,
                "stock.move.line",
                listOf(lineId),
                mapOf("qty_done" to (qtyDone + 1))
            )
            Log.d("ODOO_PICKING" , "Move line $lineId actualizada → qty_done: $qtyDone mas uno ")
            return mapOf("action" to "updated" , "move_line_id" to lineId , "qty_done" to qtyDone + 1)

        // si no la encuentra la creo
        } else {

            val locationId = when (val l = moveMap["location_id"]) {
                is JsonArray -> l[0].toIntSafe()
                is List<*> -> l[0].toIntSafe()
                else -> 0
            }
            val locationDestId = when (val l = moveMap["location_dest_id"]) {
                is JsonArray -> l[0].toIntSafe()
                is List<*> -> l[0].toIntSafe()
                else -> 0
            }
            val uomId = when (val u = moveMap["product_uom"]) {
                is JsonArray -> u[0].toIntSafe()
                is List<*> -> u[0].toIntSafe()
                else -> 0
            }
            val newLineId = client.create(
                db , uid , pass ,
                "stock.move.line" ,
                mapOf(
                    "picking_id" to pickingId ,
                    "move_id" to moveId ,
                    "product_id" to productId ,
                    "lot_id" to lotId ,
                    "qty_done" to 1.0 ,
                    "reserved_uom_qty" to 0.0 ,
                    "product_uom_id" to uomId ,
                    "location_id" to locationId ,
                    "location_dest_id" to locationDestId
                )
            )
            Log.d("ODOO_PICKING" , "Move line creada → id: $newLineId, qty_done: 1.0")
            return mapOf("action" to "created" , "move_line_id" to newLineId , "qty_done" to 1.0)
        }
        return mapOf("success" to true)
    }

    suspend fun buttonValidate(
        url: String ,
        db: String ,
        uid: Int ,
        pass: String ,
        pickingId: Int
    ): JsonElement {
        val client = OdooClient(url)
        val args = buildJsonArray {
            add(buildJsonArray {
                add(pickingId)
            })
        }

        return client.callKw(db , uid , pass , "stock.picking" , "button_validate" , args)
    }


}