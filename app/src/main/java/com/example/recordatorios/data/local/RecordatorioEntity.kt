package com.example.recordatorios.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.recordatorios.domain.EstadoSync

// Una fila de la tabla "recordatorios". Responde al esquema de Room, no a la pantalla ni al servidor.
@Entity(
    tableName = "recordatorios",
    // Índice para que la búsqueda de pendientes de sincronizar sea rápida
    indices = [Index(value = ["estado_sync"])]
)
data class RecordatorioEntity(
    // Identificador estable (UUID) creado en el teléfono. El servidor usa el mismo id,
    // así que reenviar un recordatorio actualiza el mismo registro en vez de duplicarlo.
    @PrimaryKey
    val id: String,
    val titulo: String,
    val descripcion: String?,
    val categoria: String,
    val prioridad: String,                 // "ALTA", "MEDIA" o "BAJA"
    val completado: Boolean,
    @ColumnInfo(name = "fecha_completado")
    val fechaCompletado: Long?,
    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long,
    @ColumnInfo(name = "actualizado_en")
    val actualizadoEn: Long,               // se usa para resolver conflictos (gana el más reciente)
    @ColumnInfo(name = "estado_sync")
    val estadoSync: EstadoSync
)
