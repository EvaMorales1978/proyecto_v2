package com.campusdigitalfp.proyecto_v2.data.repository

import android.util.Log
import com.campusdigitalfp.proyecto_v2.data.network.OdooClient
import com.campusdigitalfp.proyecto_v2.domain.model.Product
import com.campusdigitalfp.proyecto_v2.domain.model.ResPartner
import com.campusdigitalfp.proyecto_v2.domain.model.StockMoveLine
import com.campusdigitalfp.proyecto_v2.domain.model.StockPicking
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import java.io.EOFException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OdooRepositoryPicking {

    fun Any?.toIntSafe(): Int = when (this) {
        is Number -> this.toInt()
        is String -> this.toIntOrNull() ?: 0
        is JsonPrimitive -> this.content.toIntOrNull() ?: 0
        else -> 0
    }

    fun Any?.toStringSafe(): String = when (this) {
        is JsonPrimitive -> this.content
        else -> this?.toString() ?: ""
    }

    suspend fun getPickings(url: String, db: String, uid: Int, pass: String): List<StockPicking> {
        val client = OdooClient(url)

        val pickingDomain = buildJsonArray {
            add(buildJsonArray {
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

        var retryCount = 0
        val maxRetries = 3

        while (retryCount < maxRetries) {
            try {
                val allPickings = mutableListOf<StockPicking>()
                val pickingsDef = client.searchRead(
                    db, uid, pass,
                    "stock.picking",
                    listOf("id", "name", "partner_id", "state", "move_line_ids"),
                    domain = pickingDomain
                )

                Log.d("ODOO_FLOW", "pickingsDef size: ${pickingsDef.size}")

                for (item in pickingsDef) {
                    val pickingMap = item as? Map<*, *> ?: continue
                    Log.d("ODOO_FLOW", "pickingMap keys: ${pickingMap.keys}")
                    Log.d("ODOO_FLOW", "pickingMap: $pickingMap")

                    val pickingId = pickingMap["id"].toIntSafe()
                    Log.d("ODOO_FLOW", "pickingId: $pickingId")

                    val rawPartner = pickingMap["partner_id"]
                    val partnerId = when (rawPartner) {
                        is JsonArray -> rawPartner[0].toIntSafe()
                        is List<*> -> rawPartner[0].toIntSafe()
                        else -> null
                    }
                    Log.d("ODOO_FLOW", "partnerId: $partnerId")

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
                                Log.d("ODOO_FLOW", "partnerDomain: $partnerDomain") // debe imprimir [[\"id\",\"=\",3]]

                                val partnersDef = client.searchRead(
                                    db, uid, pass,
                                    "res.partner",
                                    listOf("id", "name", "street", "city"),
                                    domain = partnerDomain
                                )
                                Log.d("ODOO_FLOW", "partnersDef size: ${partnersDef?.size}")

                                val pMap = partnersDef?.getOrNull(0) as? Map<*, *>
                                Log.d("ODOO_FLOW", "pMap: $pMap")

                                if (pMap != null) {
                                    fullPartner = ResPartner(
                                        id = pMap["id"].toIntSafe(),
                                        name = pMap["name"].toStringSafe(),
                                        street = pMap["street"].toStringSafe(),
                                        city = pMap["city"].toStringSafe()
                                    )
                                    Log.d("ODOO_FLOW", "fullPartner: $fullPartner")
                                } else {
                                    Log.w("ODOO_FLOW", "Partner $partnerId no encontrado, continuando sin partner")
                                }
                                pSuccess = true // ✅ Salir siempre, encuentre o no el partner

                            } catch (e: Exception) {
                                val isNetworkError = e is ProtocolException ||
                                        e is EOFException ||
                                        e is SocketTimeoutException ||
                                        e.cause is ProtocolException
                                if (isNetworkError) {
                                    pRetry++
                                    Log.e("ODOO_FLOW", "Retry partner $pRetry - ${e.message}")
                                    delay(1000)
                                } else {
                                    Log.e("ODOO_FLOW", "Error partner no recuperable", e)
                                    pSuccess = true // ✅ También salir en error no recuperable
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
                                db, uid, pass,
                                "stock.move.line",
                                listOf("id", "product_id", "reserved_qty", "qty_done", "lot_name", "state"),
                                domain = moveDomain
                            )
                            Log.d("ODOO_FLOW", "linesDef size: ${linesDef?.size} for pickingId: $pickingId")

                            movelineList.clear()

                            linesDef?.forEach { moveObj ->
                                val mMap = moveObj as? Map<*, *> ?: return@forEach

                                val pData = mMap["product_id"]
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

                                movelineList.add(
                                    StockMoveLine(
                                        id = mMap["id"].toIntSafe(),
                                        product_id = product,
                                        reserved_qty = mMap["reserved_qty"].toIntSafe(),
                                        qty_done = mMap["qty_done"].toIntSafe(),
                                        state = mMap["state"].toStringSafe(),
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
                                Log.e("ODOO_FLOW", "Retry moves $mRetry ($pickingId) - ${e.message}")
                                delay(1000)
                            } else {
                                Log.e("ODOO_FLOW", "Error moves no recuperable ($pickingId)", e)
                                break
                            }
                        }
                    }

                    allPickings.add(
                        StockPicking(
                            id = pickingId,
                            name = pickingMap["name"].toStringSafe(),
                            partner_id = fullPartner,
                            state = pickingMap["state"].toStringSafe(),
                            move_line_ids = movelineList
                        )
                    )
                    Log.d("ODOO_FLOW", "allPickings size ahora: ${allPickings.size}")
                }

                Log.d("ODOO_FLOW", "return allPickings size: ${allPickings.size}")
                return allPickings

            } catch (e: Exception) {
                Log.e("ODOO_FLOW", "Error general retry $retryCount: ${e.message}", e)
                retryCount++
                delay(1000)
            }
        }
        Log.e("ODOO_FLOW", "Se agotaron los reintentos, devolviendo lista vacía")
        return emptyList()
    }
}