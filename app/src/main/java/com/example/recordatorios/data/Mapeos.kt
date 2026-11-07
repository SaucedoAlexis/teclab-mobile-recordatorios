package com.example.recordatorios.data

import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.remoto.RecordatorioDto
import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.EstadoSync
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.Recordatorio

// Funciones de mapeo explícitas entre las tres representaciones de un recordatorio:
//   RecordatorioDto (servicio)  <->  RecordatorioEntity (Room)  <->  Recordatorio (app)

// ---------- Room -> app ----------
fun RecordatorioEntity.aModelo(): Recordatorio = Recordatorio(
    id = id,
    titulo = titulo,
    categoria = Categoria(categoria),
    // Si en la base hubiera una prioridad desconocida se usa Media en lugar de cerrar la app
    prioridad = Prioridad.desdeCodigo(prioridad) ?: Prioridad.Media,
    estado = if (completado) EstadoRecordatorio.Completado(fechaCompletado ?: actualizadoEn)
             else EstadoRecordatorio.Pendiente,
    descripcion = descripcion,
    fechaCreacion = fechaCreacion,
    actualizadoEn = actualizadoEn,
    estadoSync = estadoSync
)

// ---------- app -> Room ----------
fun Recordatorio.aEntidad(estadoSync: EstadoSync): RecordatorioEntity = RecordatorioEntity(
    id = id,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria.nombre,
    prioridad = prioridad.codigo,
    completado = estado is EstadoRecordatorio.Completado,
    fechaCompletado = (estado as? EstadoRecordatorio.Completado)?.fechaCompletado,
    fechaCreacion = fechaCreacion,
    actualizadoEn = actualizadoEn,
    estadoSync = estadoSync
)

// ---------- Room -> servicio ----------
fun RecordatorioEntity.aDto(): RecordatorioDto = RecordatorioDto(
    id = id,
    title = titulo,
    description = descripcion,
    category = categoria,
    priority = when (prioridad) {
        "ALTA" -> "HIGH"
        "BAJA" -> "LOW"
        else -> "MEDIUM"
    },
    done = completado,
    doneAt = fechaCompletado,
    createdAt = fechaCreacion,
    updatedAt = actualizadoEn
)

// ---------- servicio -> Room ----------
// Lo que llega del servidor ya está confirmado, por eso queda SINCRONIZADO
fun RecordatorioDto.aEntidad(): RecordatorioEntity = RecordatorioEntity(
    id = id,
    titulo = title,
    descripcion = description,
    categoria = category,
    prioridad = when (priority) {
        "HIGH" -> "ALTA"
        "LOW" -> "BAJA"
        else -> "MEDIA"   // valor desconocido: se corrige antes de guardarlo en Room
    },
    completado = done,
    fechaCompletado = doneAt,
    fechaCreacion = createdAt,
    actualizadoEn = updatedAt,
    estadoSync = EstadoSync.SINCRONIZADO
)

// Conflicto: el mismo recordatorio cambió en el teléfono y en el servidor.
// Estrategia "gana el último que escribió" (last-write-wins): se conserva la versión más reciente.
// Si empatan, gana la local, que todavía falta enviar.
fun resolverConflicto(local: RecordatorioEntity, remoto: RecordatorioDto): RecordatorioEntity =
    if (local.actualizadoEn >= remoto.updatedAt) local else remoto.aEntidad()
