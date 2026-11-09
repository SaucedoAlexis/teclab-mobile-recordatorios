package com.example.recordatorios.data

import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.remoto.RecordatorioRemoto
import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.EstadoSync
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.Recordatorio

// Funciones de mapeo explícitas entre las tres representaciones de un recordatorio:
//   RecordatorioRemoto (Firestore)  <->  RecordatorioEntity (Room)  <->  Recordatorio (app)

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
fun Recordatorio.aEntidad(estadoSync: EstadoSync, uid: String): RecordatorioEntity = RecordatorioEntity(
    id = id,
    uid = uid,
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

// ---------- Room -> nube (Firestore) ----------
fun RecordatorioEntity.aRemoto(): RecordatorioRemoto = RecordatorioRemoto(
    id = id,
    uid = uid,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria,
    prioridad = prioridad,
    completado = completado,
    fechaCompletado = fechaCompletado,
    fechaCreacion = fechaCreacion,
    actualizadoEn = actualizadoEn
)

// ---------- nube -> Room ----------
// Lo que llega de la nube ya está confirmado, por eso queda SINCRONIZADO.
// El uid se toma de la sesión (no del documento): una fila local siempre pertenece a la cuenta activa.
fun RecordatorioRemoto.aEntidad(uidSesion: String): RecordatorioEntity = RecordatorioEntity(
    id = id,
    uid = uidSesion,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria,
    // Valor desconocido: se corrige antes de guardarlo en Room
    prioridad = prioridad.takeIf { it in setOf("ALTA", "MEDIA", "BAJA") } ?: "MEDIA",
    completado = completado,
    fechaCompletado = fechaCompletado,
    fechaCreacion = fechaCreacion,
    actualizadoEn = actualizadoEn,
    estadoSync = EstadoSync.SINCRONIZADO
)

// Conflicto: el mismo recordatorio cambió en el teléfono y en la nube (por ejemplo, desde otro dispositivo).
// Estrategia "gana el último que escribió" (last-write-wins): se conserva la versión más reciente.
// Si empatan, gana la local, que todavía falta enviar.
fun resolverConflicto(local: RecordatorioEntity, remoto: RecordatorioRemoto): RecordatorioEntity =
    if (local.actualizadoEn >= remoto.actualizadoEn) local else remoto.aEntidad(local.uid)
