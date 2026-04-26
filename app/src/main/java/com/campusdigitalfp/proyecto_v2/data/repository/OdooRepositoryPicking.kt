package com.campusdigitalfp.proyecto_v2.data.repository


import android.util.Log
import com.campusdigitalfp.proyecto_v2.data.network.OdooClient
import com.campusdigitalfp.proyecto_v2.domain.model.Product
import com.campusdigitalfp.proyecto_v2.domain.model.ResPartner
import com.campusdigitalfp.proyecto_v2.domain.model.StockMoveLine
import com.campusdigitalfp.proyecto_v2.domain.model.StockPicking
import kotlinx.coroutines.delay
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import java.io.EOFException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class OdooRepositoryPicking{
    //private val api = RetrofitClient.instance
    suspend fun getPickings(url: String,db: String, uid: Int, pass: String): List<StockPicking> {
        val client = OdooClient(url)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        /*val pickingDomain = listOf(
            "|",
            listOf("state", "=", "assigned"),
            listOf("date_done", ">=", "$today 00:00:00")
        )*/
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
        //val pickingFields = listOf("id", "name", "partner_id","state")
        var retryCount = 0
        val maxRetries = 3

        while (retryCount < maxRetries) {
            try {
              /*  val pickingArgs = mapOf("fields" to pickingFields, "limit" to 20)
                val finalArgs = listOf(db, uid, pass, "stock.picking", "search_read", listOf(pickingDomain), pickingArgs)
                val request = OdooRequest(params = mapOf("service" to "object", "method" to "execute_kw", "args" to finalArgs))

                val response = api.callRpc(request)
                if (response.error != null) return emptyList()

                val resultList = response.result as? List<*> ?: return emptyList()
                if (resultList.isEmpty()) return emptyList()
               */
                val allPickings = mutableListOf<StockPicking>()
                val pickingsDef = client.searchRead(
                        db ,
                        uid ,
                        pass ,
                        "stock.picking" ,
                        listOf("id" , "name" , "partner_id" , "state" , "move_line_ids")
                        , domain = pickingDomain
                    )
                for (item in pickingsDef) {
                    val pickingMap = item as? Map<*, *> ?: continue
                    val pickingId = (pickingMap["id"] as? Number)?.toInt() ?: 0

                    val partnerData = pickingMap["partner_id"] as? List<*>
                    val partnerId = (partnerData?.get(0) as? Number)?.toInt()
                    var fullPartner: ResPartner? = null

                    if (partnerId != null) {

                        var retryCount = 0
                        val maxRetries = 5
                        var success = false

                        while (retryCount < maxRetries && !success) {
                            try {
                                val partnersDef = client.searchRead(
                                        db ,
                                        uid ,
                                        pass ,
                                        "res.partner" ,
                                        listOf("id" , "name" , "street" , "city")
                                    )

                                val pMap = partnersDef?.getOrNull(0) as? Map<*, *>
                                if (pMap != null) {
                                    fullPartner = ResPartner(
                                        id = (pMap["id"] as? Number)?.toInt() ?: 0,
                                        name = pMap["name"]?.toString() ?: "",
                                        street = pMap["street"]?.toString() ?: "",
                                        city = pMap["city"]?.toString() ?: ""
                                    )
                                    success = true
                                }

                            } catch (e: Exception) {

                                val isNetworkError =
                                    e is ProtocolException ||
                                            e is EOFException ||
                                            e is SocketTimeoutException ||
                                            e.cause is ProtocolException

                                if (isNetworkError) {
                                    retryCount++
                                    Log.e("ODOO", "Retry $retryCount (${pickingMap["name"]}) - ${e.message}")
                                    delay(1000)
                                } else {
                                    Log.e("ODOO", "Error NO recuperable (${pickingMap["name"]})", e)
                                    break
                                }
                            }
                        }
                    }

                    val movelineList = mutableListOf<StockMoveLine>()

                    var retryCount = 0
                    val maxRetries = 5
                    var success = false

                    while (retryCount < maxRetries && !success) {
                        try {
                            val moveDomain = buildJsonArray {
                                add(buildJsonArray {
                                    add("picking_id")
                                    add("=")
                                    add(pickingId)
                                })
                            }
                            val linesDef = client.searchRead(
                                db ,
                                uid ,
                                pass ,
                                "stock.move.line" ,
                                listOf("id" , "product_id" , "reserved_qty" , "qty_done" , "lot_name" , "state"),
                                domain = moveDomain
                            )
                           /* val mArgs = listOf(
                                db, uid, pass,
                                "stock.move.line", "search_read",
                                listOf(listOf(listOf("picking_id", "=", pickingId))),
                                mapOf("fields" to listOf("id", "product_id", "reserved_qty", "qty_done","lot_name","state"))
                            )

                            val mRes = api.callRpc(
                                OdooRequest(params = mapOf(
                                    "service" to "object",
                                    "method" to "execute_kw",
                                    "args" to mArgs
                                ))
                            ).result as? List<*>*/

                            movelineList.clear()

                            linesDef?.forEach { moveObj ->
                                val mMap = moveObj as? Map<*, *> ?: return@forEach

                                val pData = mMap["product_id"] as? List<*>
                                val product = Product(
                                    (pData?.get(0) as? Number)?.toInt() ?: 0,
                                    pData?.get(1)?.toString() ?: ""
                                )

                                movelineList.add(
                                    StockMoveLine(
                                        id = (mMap["id"] as? Number)?.toInt() ?: 0,
                                        product_id = product,
                                        reserved_qty = (mMap["reserved_qty"] as? Number)?.toInt() ?: 0,
                                        qty_done = (mMap["qty_done"] as? Number)?.toInt() ?: 0,
                                        state = (mMap["state"] as? String)?:"",
                                        lot_name = "",
                                    )
                                )
                            }

                            success = true

                        } catch (e: Exception) {

                            val isNetworkError =
                                e is ProtocolException ||
                                        e is EOFException ||
                                        e is SocketTimeoutException ||
                                        e.cause is ProtocolException

                            if (isNetworkError) {
                                retryCount++
                                Log.e("ODOO", "Retry MOVES $retryCount ($pickingId) - ${e.message}")
                                delay(1000)
                            } else {
                                Log.e("ODOO", "Error MOVES NO recuperable ($pickingId)", e)
                                break
                            }
                        }
                    }

                    allPickings.add(StockPicking(
                        id = pickingId ,
                        name = pickingMap["name"]?.toString() ?: "" ,
                        partner_id = fullPartner ,
                        state = pickingMap["state"]?.toString() ?: "" ,
                        move_line_ids = movelineList
                    ))
                }

                return allPickings

            } catch (e: Exception) {
                retryCount++
                delay(1000)
            }
        }
        return emptyList()
    }

}
