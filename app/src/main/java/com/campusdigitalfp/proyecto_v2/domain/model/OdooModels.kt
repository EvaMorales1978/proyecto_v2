package com.campusdigitalfp.proyecto_v2.domain.model

data class OdooDataPackage(
    val lots: List<StockLot>,
    val partners: List<ResPartner>,
    val pickings: List<StockPicking>
)

data class StockLot(val id: Int, val name: String)
data class ResPartner(val id: Int, val name: String, val email: String?)
data class StockPicking(val id: Int, val name: String, val state: String)