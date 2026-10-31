package com.example.recordatorios.domain

// Entidad simple e inmutable
data class Categoria(val nombre: String)

// Conjunto cerrado de prioridades; "peso" sirve para ordenar
sealed class Prioridad(val etiqueta: String, val peso: Int) {
    data object Alta : Prioridad("Alta", 3)
    data object Media : Prioridad("Media", 2)
    data object Baja : Prioridad("Baja", 1)
}

// Opciones que se ofrecen en el formulario (se calcula con get() para no depender del orden de inicialización)
val prioridadesDisponibles: List<Prioridad>
    get() = listOf(Prioridad.Alta, Prioridad.Media, Prioridad.Baja)

// Cada estado transporta solo la información que necesita
sealed interface EstadoRecordatorio {
    data object Pendiente : EstadoRecordatorio
    data class Completado(val fechaCompletado: String) : EstadoRecordatorio
    data class Vencido(val diasDeRetraso: Int) : EstadoRecordatorio
}

// Entidad principal: todo val (inmutable); los datos opcionales son anulables
data class Recordatorio(
    val id: Long,
    val titulo: String,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: EstadoRecordatorio,
    val descripcion: String? = null,
    val ubicacion: String? = null,
    val fechaAviso: String? = null
)

// Filtros disponibles en la interfaz
sealed class FiltroEstado(val etiqueta: String) {
    data object Todos : FiltroEstado("Todos")
    data object Pendientes : FiltroEstado("Pendientes")
    data object Completados : FiltroEstado("Completados")
    data object Vencidos : FiltroEstado("Vencidos")
}