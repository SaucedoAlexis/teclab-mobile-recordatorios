package com.example.recordatorios.domain

// Entidad simple e inmutable
data class Categoria(val nombre: String)

// Conjunto cerrado de prioridades. "peso" sirve para ordenar y "codigo" es el texto que se guarda en Room
sealed class Prioridad(val etiqueta: String, val peso: Int, val codigo: String) {
    data object Alta : Prioridad("Alta", 3, "ALTA")
    data object Media : Prioridad("Media", 2, "MEDIA")
    data object Baja : Prioridad("Baja", 1, "BAJA")

    companion object {
        // Devuelve null si el texto guardado no corresponde a ninguna prioridad
        fun desdeCodigo(codigo: String): Prioridad? = prioridadesDisponibles.find { it.codigo == codigo }
    }
}

// Opciones que se ofrecen en el formulario (se calcula con get() para no depender del orden de inicialización)
val prioridadesDisponibles: List<Prioridad>
    get() = listOf(Prioridad.Alta, Prioridad.Media, Prioridad.Baja)

// Cada estado transporta solo la información que necesita.
// En la Actividad 3 se quitó "Vencido": dependía de días inventados y la app todavía no maneja fechas límite.
sealed interface EstadoRecordatorio {
    data object Pendiente : EstadoRecordatorio
    data class Completado(val fechaCompletado: Long) : EstadoRecordatorio
}

// Indica si el registro local ya está igual en la nube (Firestore) o si falta enviarlo
enum class EstadoSync {
    SINCRONIZADO,       // local y remoto coinciden
    PENDIENTE,          // creado o modificado en el teléfono, falta enviarlo
    PENDIENTE_BORRAR,   // eliminado en el teléfono, falta borrarlo de la nube
    ERROR               // la nube lo rechazó (reglas o datos inválidos): necesita revisión
}

// Modelo que usa la app (pantallas y ViewModel). No conoce Room ni Retrofit.
data class Recordatorio(
    val id: String,                 // UUID generado en el teléfono: no cambia nunca
    val titulo: String,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: EstadoRecordatorio,
    val descripcion: String? = null,
    val fechaCreacion: Long = 0L,   // milisegundos desde 1970
    val actualizadoEn: Long = 0L,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE
) {
    val completado: Boolean get() = estado is EstadoRecordatorio.Completado
}

// Filtros disponibles en la interfaz
sealed class FiltroEstado(val etiqueta: String) {
    data object Todos : FiltroEstado("Todos")
    data object Pendientes : FiltroEstado("Pendientes")
    data object Completados : FiltroEstado("Completados")
}
