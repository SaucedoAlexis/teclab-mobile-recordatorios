package com.example.recordatorios.ui

import com.example.recordatorios.data.Categorias
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.EstadoSync
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.Recordatorio

// Datos fijos solo para las vistas previas (@Preview) de Android Studio. La app no los usa.
object EjemplosPreview {
    val recordatorio = Recordatorio(
        id = "ejemplo-1",
        titulo = "Entregar informe mensual",
        categoria = Categorias.trabajo,
        prioridad = Prioridad.Alta,
        estado = EstadoRecordatorio.Pendiente,
        descripcion = "Enviar al coordinador antes del viernes",
        fechaCreacion = 1_793_000_000_000L,
        actualizadoEn = 1_793_000_000_000L,
        estadoSync = EstadoSync.PENDIENTE
    )
    val lista = listOf(
        recordatorio,
        recordatorio.copy(id = "ejemplo-2", titulo = "Turno con el dentista", categoria = Categorias.salud,
            prioridad = Prioridad.Media, estadoSync = EstadoSync.SINCRONIZADO)
    )
}
