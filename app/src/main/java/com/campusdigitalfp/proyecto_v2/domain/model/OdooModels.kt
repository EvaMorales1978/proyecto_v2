package com.campusdigitalfp.proyecto_v2.domain.model

data class OdooDataPackage(
    val lots: List<StockLot>,
    val partners: List<ResPartner>,
    val products: List<Product>,
    val lines: List<StockMoveLine>,
    val pickings: List<StockPicking>
)
data class StockLot(
    val id: Int,
    val name: String,
)
data class ResPartner(
    val id: Int,
    val name: String,
    val street: String?,
    val city: String?
)
data class Product(
    val id: Int,
    val name: String,
)
data class StockMoveLine(
    val id: Int ,
    val product_id: Product,
    val reserved_qty: Int ,
    val qty_done: Int,
    var lot_name: String,
    val state:String
)
data class StockPicking(
    val id: Int ,
    val name: String ,
    val partner_id: ResPartner? ,
    val state: String ,
    val move_line_ids: List<StockMoveLine>
)