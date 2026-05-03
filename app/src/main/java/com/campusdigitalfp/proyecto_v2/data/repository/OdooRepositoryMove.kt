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
                            product_done = moveMap["product_done"].toIntSafe(),
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
}
