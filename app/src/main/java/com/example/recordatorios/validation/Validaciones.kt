package com.example.recordatorios.validation

import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.Recordatorio

sealed interface ResultadoValidacion {
    data object Valido : ResultadoValidacion
    data class Invalido(val errores: List<String>) : ResultadoValidacion
}

fun validarRecordatorio(r: Recordatorio): ResultadoValidacion {
    val errores = buildList {
        if (r.titulo.isBlank()) add("El título es obligatorio")
        else if (r.titulo.trim().length < 4) add("El título debe tener al menos 4 caracteres")
        if (r.titulo.length > 60) add("El título no puede superar 60 caracteres")
        if (r.descripcion != null && r.descripcion.length > 200) add("La descripción no puede superar 200 caracteres")
        if (r.estado is EstadoRecordatorio.Vencido && r.estado.diasDeRetraso <= 0) add("Un vencido debe tener días de retraso positivos")
    }
    return if (errores.isEmpty()) ResultadoValidacion.Valido else ResultadoValidacion.Invalido(errores)
}

fun List<Recordatorio>.validos(): List<Recordatorio> =
    filter { validarRecordatorio(it) is ResultadoValidacion.Valido }