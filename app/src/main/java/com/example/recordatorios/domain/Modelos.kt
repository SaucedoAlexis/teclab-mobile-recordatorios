package com.example.recordatorios.domain

// Entidad simple e inmutable
data class Categoria(val nombre: String)

// Conjunto cerrado de prioridades; "peso" sirve para ordenar
sealed class Prioridad(val etiqueta: String, val peso: Int) {
    data object Alta : Prioridad("Alta", 3)
    data object Media : Prioridad("Media", 2)
    data object Baja : Prioridad("Baja", 1)
}

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

// Estados posibles de la pantalla: nunca dos a la vez
sealed interface EstadoPantalla {
    data object Cargando : EstadoPantalla
    data class ConDatos(val recordatorios: List<Recordatorio>) : EstadoPantalla
    data object SinResultados : EstadoPantalla
    data class Error(val mensaje: String) : EstadoPantalla
}

// Filtros disponibles en la interfaz
sealed class FiltroEstado(val etiqueta: String) {
    data object Todos : FiltroEstado("Todos")
    data object Pendientes : FiltroEstado("Pendientes")
    data object Completados : FiltroEstado("Completados")
    data object Vencidos : FiltroEstado("Vencidos")
}