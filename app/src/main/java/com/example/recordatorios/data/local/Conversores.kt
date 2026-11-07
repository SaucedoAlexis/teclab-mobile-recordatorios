package com.example.recordatorios.data.local

import androidx.room.TypeConverter
import com.example.recordatorios.domain.EstadoSync

// SQLite no conoce los enum de Kotlin: se guardan como texto ("PENDIENTE", "SINCRONIZADO", ...)
class Conversores {
    @TypeConverter
    fun estadoSyncATexto(estado: EstadoSync): String = estado.name

    // Si en la base hubiera un texto desconocido, se toma como pendiente para volver a enviarlo
    @TypeConverter
    fun textoAEstadoSync(texto: String): EstadoSync =
        EstadoSync.entries.firstOrNull { it.name == texto } ?: EstadoSync.PENDIENTE
}
