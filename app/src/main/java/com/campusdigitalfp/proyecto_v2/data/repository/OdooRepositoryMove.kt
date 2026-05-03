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
        url: String,
        db: String,
        uid: Int,
        pass: String,
        lotName: String
    ): Map<String, Any> {
        val client = OdooClient(url)

        // 1. Buscar el lote por nombre
        val lotDomain = buildJsonArray {
            add(buildJsonArray {
                add(buildJsonArray { add("name"); add("="); add(lotName) })
            })
        }
        val lotResult = client.searchRead(db, uid, pass, "stock.lot",
            listOf("id", "name", "product_id"), domain = lotDomain)
        val lotMap = lotResult?.getOrNull(0) as? Map<*, *>
            ?: throw IllegalArgumentException("Lote '$lotName' no encontrado")

        val lotId = lotMap["id"].toIntSafe()
        val productId = when (val p = lotMap["product_id"]) {
            is JsonArray -> p[0].toIntSafe()
            is List<*>   -> p[0].toIntSafe()
            else -> throw IllegalArgumentException("El lote '$lotName' no tiene producto asociado")
        }

        Log.d("ODOO_FLOW", "lotResult size: ${lotResult?.size}")
        Log.d("ODOO_FLOW", "Buscando moves para productId: $productId")


        // 2. Buscar TODOS los moves activos para ese producto en picking type 3
        //    Añadimos product_qty y qty_done para comparar
        val moveDomain = buildJsonArray {
            add(buildJsonArray {
                add(buildJsonArray { add("state");           add("="); add("assigned") })
                add(buildJsonArray { add("picking_type_id"); add("="); add(3) })
                add(buildJsonArray { add("product_id");      add("="); add(productId) })
            })
        }

        val moveResult = client.searchRead(db, uid, pass, "stock.move",
            listOf("id", "picking_id", "location_id", "location_dest_id","product_uom", "product_qty", "quantity_done")
            ,domain = moveDomain
        )

        Log.d("ODOO_FLOW", "moveResult size: ${moveResult?.size}")


        if (moveResult.isNullOrEmpty()) {
            throw IllegalArgumentException("No hay stock.move activo para el producto $productId")
        }

        // 3. Iterar moves y actuar solo donde qty_done < product_qty
        val results = mutableListOf<Map<String, Any>>()

        for (rawMove in moveResult) {
            val moveMap = rawMove as? Map<*, *> ?: continue

            val productQty = moveMap["product_qty"].toIntSafe()
            val qtyDone    = moveMap["qty_done"].toIntSafe()

            // ← Condición clave: solo procesar si falta cantidad
            if (qtyDone >= productQty) continue

            val moveId    = moveMap["id"].toIntSafe()
            val pickingId = when (val pk = moveMap["picking_id"]) {
                is JsonArray -> pk[0].toIntSafe()
                is List<*>   -> pk[0].toIntSafe()
                else -> continue  // sin picking, saltamos
            }
            val locationId = when (val l = moveMap["location_id"]) {
                is JsonArray -> l[0].toIntSafe()
                is List<*>   -> l[0].toIntSafe()
                else -> 0
            }
            val locationDestId = when (val l = moveMap["location_dest_id"]) {
                is JsonArray -> l[0].toIntSafe()
                is List<*>   -> l[0].toIntSafe()
                else -> 0
            }
            val uomId = when (val u = moveMap["product_uom"]) {
                is JsonArray -> u[0].toIntSafe()
                is List<*>   -> u[0].toIntSafe()
                else -> 0
            }

            // 4. Buscar move.line existente para este picking + producto + lote
            val lineDomain = buildJsonArray {
                add(buildJsonArray {
                    add(buildJsonArray { add("picking_id"); add("="); add(pickingId) })
                    add(buildJsonArray { add("product_id"); add("="); add(productId) })
                    add(buildJsonArray { add("lot_id");     add("="); add(lotId) })
                    add(buildJsonArray {
                        add("state"); add("not in")
                        add(buildJsonArray { add("done"); add("cancel") })
                    })
                })
            }
            val existingLines = client.searchRead(db, uid, pass, "stock.move.line",
                listOf("id", "qty_done", "reference"), domain = lineDomain)
            val existingLine = existingLines?.getOrNull(0) as? Map<*, *>

            val result = if (existingLine != null) {
                // 5a. Ya existe → sumar 1
                val lineId     = existingLine["id"].toIntSafe()
                val currentQty = existingLine["qty_done"].toIntSafe()
                val newQty     = currentQty + 1.0

                client.write(db, uid, pass, "stock.move.line", listOf(lineId), mapOf("qty_done" to newQty))
                Log.d("ODOO_PICKING", "Move line $lineId actualizada → qty_done: $newQty")

                mapOf("action" to "updated", "move_line_id" to lineId,
                    "qty_done" to newQty, "picking_id" to pickingId)

            } else {
                // 5b. No existe → crear nueva línea
                val newLineId = client.create(db, uid, pass, "stock.move.line",
                    mapOf(
                        "picking_id"       to pickingId,
                        "move_id"          to moveId,
                        "product_id"       to productId,
                        "lot_id"           to lotId,
                        "qty_done"         to 1.0,
                        "reserved_uom_qty" to 0.0,
                        "product_uom_id"   to uomId,
                        "location_id"      to locationId,
                        "location_dest_id" to locationDestId
                    )
                )
                Log.d("ODOO_PICKING", "Move line creada → id: $newLineId, qty_done: 1.0")

                mapOf("action" to "created", "move_line_id" to newLineId,
                    "qty_done" to 1.0, "picking_id" to pickingId)
            }

            results.add(result)
        }

        return if (results.size == 1) {
            results.first()
        } else {
            mapOf("action" to "batch", "results" to results, "total" to results.size)
        }
    }


}
