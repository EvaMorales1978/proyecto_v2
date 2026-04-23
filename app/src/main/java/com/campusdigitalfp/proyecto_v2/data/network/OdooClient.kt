package com.campusdigitalfp.proyecto_v2.data.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class OdooClient(private val baseUrl: String) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 15000
        }
    }

    suspend fun authenticate(db: String, user: String, pass: String): Int {
        val response: HttpResponse = client.post("$baseUrl/jsonrpc") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("jsonrpc", "2.0")
                put("method", "call")
                put("params", buildJsonObject {
                    put("service", "common")
                    put("method", "login")
                    put("args", buildJsonArray {
                        add(db)
                        add(user)
                        add(pass)
                    })
                })
                put("id", 1)
            })
        }

        val responseText = response.bodyAsText()
        val body = Json.parseToJsonElement(responseText).jsonObject
        val result = body["result"]

        // Verificación de UID válida
        return if (result is JsonPrimitive && result.content != "false") {
            result.content.toInt()
        } else {
            throw Exception("Autenticación fallida")
        }
    }

    suspend fun searchRead(
        db: String,
        uid: Int,
        pass: String,
        model: String,
        fields: List<String>,
        domain: JsonArray = buildJsonArray { }
    ): JsonArray {
        val response: HttpResponse = client.post("$baseUrl/jsonrpc") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("jsonrpc", "2.0")
                put("method", "call")
                put("params", buildJsonObject {
                    put("service", "object")
                    put("method", "execute_kw")
                    put("args", buildJsonArray {
                        add(db)
                        add(uid)
                        add(pass)
                        add(model)
                        add("search_read")
                        add(domain) // Posición 6: El dominio (filtro)
                    })
                    // Los campos opcionales van aquí
                    put("kwargs", buildJsonObject {
                        put("fields", buildJsonArray {
                            fields.forEach { add(it) }
                        })
                    })
                })
                put("id", 2)
            })
        }
        val responseText = response.bodyAsText()
        val body = Json.parseToJsonElement(responseText).jsonObject
        return body["result"]?.jsonArray ?: JsonArray(emptyList())
    }
    /*suspend fun searchRead(
        db: String,
        uid: Int,
        pass: String,
        model: String,
        fields: List<String>
    ): JsonArray {
        val response: HttpResponse = client.post("$baseUrl/jsonrpc") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("jsonrpc", "2.0")
                put("method", "call")
                put("params", buildJsonObject {
                    put("service", "object")
                    put("method", "execute_kw")
                    put("args", buildJsonArray {
                        add(db)
                        add(uid)
                        add(pass)
                        add(model)
                        add("search_read")
                        add(buildJsonArray { }) // Dominio []
                        add(buildJsonObject {
                            put("fields", buildJsonArray { fields.forEach { add(it) } })
                        })
                    })
                })
                put("id", 2)
            })
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return body["result"]?.jsonArray ?: JsonArray(emptyList())
    }*/
}