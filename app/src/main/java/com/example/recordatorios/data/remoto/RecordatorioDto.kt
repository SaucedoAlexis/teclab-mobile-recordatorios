package com.example.recordatorios.data.remoto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DTO: representa el JSON que envía y recibe el servicio (su "contrato").
// El servidor usa nombres en inglés y otros códigos de prioridad, por eso NO es la misma clase
// que la entidad de Room ni que el modelo de la app. Nunca llega a las pantallas.
@Serializable
data class RecordatorioDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val category: String,
    val priority: String,                       // "HIGH", "MEDIUM" o "LOW"
    val done: Boolean = false,
    @SerialName("done_at") val doneAt: Long? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)
