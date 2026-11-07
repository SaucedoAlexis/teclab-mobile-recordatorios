package com.example.recordatorios.domain

// Set: elimina categorías repetidas automáticamente
fun List<Recordatorio>.categoriasUnicas(): Set<Categoria> =
    map { it.categoria }.toSet()

// filter por estado, usando el when exhaustivo sobre la sealed class
fun List<Recordatorio>.filtrarPorEstado(filtro: FiltroEstado): List<Recordatorio> =
    when (filtro) {
        FiltroEstado.Todos -> this
        FiltroEstado.Pendientes -> filter { it.estado is EstadoRecordatorio.Pendiente }
        FiltroEstado.Completados -> filter { it.estado is EstadoRecordatorio.Completado }
    }

// filter por prioridad
fun List<Recordatorio>.filtrarPorPrioridad(prioridad: Prioridad): List<Recordatorio> =
    filter { it.prioridad == prioridad }

// Orden del listado según la preferencia elegida (los más nuevos primero dentro de cada grupo)
fun List<Recordatorio>.ordenarPorPrioridad(): List<Recordatorio> =
    sortedWith(compareByDescending<Recordatorio> { it.prioridad.peso }.thenByDescending { it.fechaCreacion })

fun List<Recordatorio>.ordenarPorFecha(): List<Recordatorio> =
    sortedByDescending { it.fechaCreacion }

fun List<Recordatorio>.ordenarPorTitulo(): List<Recordatorio> =
    sortedBy { it.titulo.lowercase() }

// Map: cantidad de recordatorios por prioridad
fun List<Recordatorio>.contarPorPrioridad(): Map<Prioridad, Int> =
    groupingBy { it.prioridad }.eachCount()
