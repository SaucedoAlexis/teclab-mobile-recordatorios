package com.example.recordatorios.validation

import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.Recordatorio

const val MIN_CARACTERES_TITULO = 4
const val MAX_CARACTERES_DESCRIPCION = 200

sealed interface ResultadoValidacion {
    data object Valido : ResultadoValidacion
    data class Invalido(val errores: List<String>) : ResultadoValidacion
}

fun validarRecordatorio(r: Recordatorio): ResultadoValidacion {
    val errores = buildList {
        if (r.titulo.isBlank()) add("El título es obligatorio")
        else if (r.titulo.trim().length < MIN_CARACTERES_TITULO) add("El título debe tener al menos 4 caracteres")
        if (r.titulo.length > 60) add("El título no puede superar 60 caracteres")
        if (r.descripcion != null && r.descripcion.length > MAX_CARACTERES_DESCRIPCION) add("La descripción no puede superar 200 caracteres")
        if (r.estado is EstadoRecordatorio.Vencido && r.estado.diasDeRetraso <= 0) add("Un vencido debe tener días de retraso positivos")
    }
    return if (errores.isEmpty()) ResultadoValidacion.Valido else ResultadoValidacion.Invalido(errores)
}

fun List<Recordatorio>.validos(): List<Recordatorio> =
    filter { validarRecordatorio(it) is ResultadoValidacion.Valido }

// Un mensaje por campo; null significa "sin error"
data class ErroresFormulario(
    val titulo: String? = null,
    val descripcion: String? = null,
    val categoria: String? = null,
    val prioridad: String? = null
) {
    val esValido: Boolean
        get() = titulo == null && descripcion == null && categoria == null && prioridad == null
}

// Reglas del formulario de creación (Actividad 2, consigna 2)
fun validarFormulario(
    titulo: String,
    descripcion: String,
    categoria: Categoria?,
    prioridad: Prioridad?,
    categoriasDisponibles: List<Categoria>
): ErroresFormulario = ErroresFormulario(
    titulo = when {
        titulo.isBlank() -> "El título es obligatorio"
        titulo.trim().length < MIN_CARACTERES_TITULO ->
            "El título debe tener al menos $MIN_CARACTERES_TITULO caracteres (sin contar espacios al inicio y al final)"
        else -> null
    },
    descripcion = if (descripcion.length > MAX_CARACTERES_DESCRIPCION)
        "La descripción no puede superar los $MAX_CARACTERES_DESCRIPCION caracteres"
    else null,
    categoria = when {
        categoria == null -> "Seleccioná una categoría"
        categoria !in categoriasDisponibles -> "La categoría no pertenece a las opciones definidas"
        else -> null
    },
    prioridad = if (prioridad == null) "Seleccioná una prioridad" else null
)
