package com.example.recordatorios.data.remoto

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

// Cada función es un endpoint del servicio. Son suspend para usarlas desde coroutines.
// Se devuelve Response<T> para poder leer el código HTTP y decidir qué hacer.
interface RecordatoriosApi {

    @GET("recordatorios")
    suspend fun listar(): Response<List<RecordatorioDto>>

    // PUT con el id en la ruta: crea el recurso si no existe o lo reemplaza si ya existe.
    // Enviar dos veces lo mismo no genera dos registros (operación idempotente).
    @PUT("recordatorios/{id}")
    suspend fun guardar(@Path("id") id: String, @Body recordatorio: RecordatorioDto): Response<RecordatorioDto>

    @DELETE("recordatorios/{id}")
    suspend fun eliminar(@Path("id") id: String): Response<Unit>
}
