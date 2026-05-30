package com.campusdigitalfp.proyecto_v2.data.repository


import android.util.Log
import com.campusdigitalfp.proyecto_v2.data.network.OdooClient
import com.campusdigitalfp.proyecto_v2.domain.model.Product
import com.campusdigitalfp.proyecto_v2.domain.model.StockMove
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray


class OdooRepositoryMove {

    fun Any?.toIntSafe(): Int = when (this) {
        is Number      -> this.toInt()
        is String      -> this.toDoubleOrNull()?.toInt() ?: 0
        is JsonPrimitive -> this.content.toDoubleOrNull()?.toInt() ?: 0
        else -> 0
    }

    fun Any?.toStringSafe(): String = when (this) {
        is JsonPrimitive -> this.content
        else -> this?.toString() ?: ""
    }

    suspend fun getMoves(url: String, db: String , uid: Int , pass: String): List<StockMove> {
        val client = OdooClient(url)
        val moveDomain = buildJsonArray {
            add(buildJsonArray {
                add(buildJsonArray {
                    add("state")
                    add("=")
                    add("assigned")
                })
                add(buildJsonArray {
                    add("picking_type_id")
                    add("=")
                    add(3)
                })
            })
        }

        var retryCount = 0
        val maxRetries = 3

        while (retryCount < maxRetries) {
            try {
                val moveDef = client.searchRead(
                    db, uid, pass,
                    "stock.move",
                    listOf("id" , "name" , "product_id" , "product_qty" , "quantity_done" , "state")
                    ,domain = moveDomain
                )
                Log.d("ODOO_FLOW", "moveDef size: ${moveDef?.size}")
                val allMoves = mutableListOf<StockMove>()
                moveDef?.forEach{ moveObj ->
                    val moveMap = moveObj as? Map<* , *> ?: return@forEach
                    val pData = moveMap["product_id"]
                    val product = Product(
                        id = when (pData) {
                            is JsonArray -> pData[0].toIntSafe()
                            is List<*> -> pData[0].toIntSafe()
                            else -> 0
                        },
                        name = when (pData) {
                            is JsonArray -> pData[1].toStringSafe()
                            is List<*> -> pData[1].toStringSafe()
                            else -> ""
                        }
                    )
                    allMoves.add(
                        StockMove(
                            id = (moveMap["id"] as? Number)?.toInt() ?: 0 ,
                            name = moveMap["name"]?.toString() ?: "" ,
                            product = product ,
                            product_done = moveMap["quantity_done"].toIntSafe(),
                            product_qty = moveMap["product_qty"].toIntSafe(),
                            state = moveMap["state"]?.toString() ?: ""
                            )
                    )
                }

                return allMoves
            } catch (e: Exception) {
                retryCount++
                Log.e("ODOO" , "Retry $retryCount - ${e.message}" , e)
                delay(1000)
            }
        }

        return emptyList()
    }


    suspend fun getMovesGrouped(url:String, db: String , uid: Int , pass: String): List<StockMove> {
        val allMoves = getMoves(url,  db , uid , pass)

        val groupedMoves = allMoves.groupBy { it.product.id }
            .map { (productId , moves) ->
                val firstMove = moves.first()
                StockMove(
                    id = productId ,
                    name = firstMove.name ,
                    product = firstMove.product ,
                    product_done = moves.sumOf { it.product_done } ,
                    product_qty = moves.sumOf { it.product_qty } ,
                    state = firstMove.state
                )
            }

        return groupedMoves
    }

    suspend fun UpdateMoveLine(
        url: String ,
        db: String ,
        uid: Int ,
        pass: String ,
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
        //busco los stock_move d ese producto
        val moveDomain = buildJsonArray {
            add(buildJsonArray {
                add(buildJsonArray { add("picking_type_id"); add("="); add(3) })
                add(buildJsonArray { add("product_id"); add("="); add(productId) })
            })
        }
        val moveResult = client.searchRead(
            db , uid , pass ,
            "stock.move" ,
            listOf("id" , "location_id" , "location_dest_id" , "product_uom","product_qty" , "quantity_done","picking_id" ) ,
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
                "message" to "No hay movimientos pendientes para el producto correspondiente al lote '$lotName'."
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
            val locationId = when (val l = moveMap["picking_id"]) {
                is JsonArray -> l[0].toIntSafe()
                is List<*> -> l[0].toIntSafe()
                else -> 0
            }
            val pickingId = when (val l = moveMap["location_id"]) {
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


    suspend fun validateAssignedPickings(
        url: String, db: String, uid: Int, pass: String
    ) {
        val client = OdooClient(url)

        val pickingDomain = buildJsonArray {
            add(buildJsonArray {
                add(buildJsonArray {
                    add("state")
                    add("in")
                    add(buildJsonArray {
                        add("assigned")
                    })
                })
                add(buildJsonArray {
                    add("picking_type_id")
                    add("=")
                    add(3)
                })
            })
        }

        val moveResult = client.searchRead(
            db, uid, pass,
            "stock.picking",
            listOf("id"),
            domain = pickingDomain
        )
        Log.d("ODOO_FLOW", "Pickings recibidos: ${moveResult.size}")

        val pickingIds = moveResult
            ?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                when (val pk = map["id"]) {
                    is JsonArray -> pk[0].toIntSafe()
                    is List<*>   -> pk[0].toIntSafe()
                    else -> pk.toIntSafe().takeIf { it != 0 }
                }
            }
            ?.distinct()
            ?: emptyList()

        Log.d("ODOO_FLOW", "Pickings a validar: $pickingIds")

        pickingIds.forEach { id ->
            val args = buildJsonArray {
                add(buildJsonArray { add(id) })
            }
            client.callKw(db, uid, pass, "stock.picking", "button_validate", args)
            Log.d("ODOO_FLOW", "Validado picking: $id")
        }
    }

}
