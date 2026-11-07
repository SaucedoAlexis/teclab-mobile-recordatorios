package com.example.recordatorios.data

// Mismo patrón que ResultadoRemoto: un sealed interface con cada forma en que puede terminar algo.

// Resultado de guardar, editar o eliminar en Room
sealed interface ResultadoOperacion {
    data object Exito : ResultadoOperacion
    data class Error(val mensaje: String) : ResultadoOperacion
}

// Cómo terminó una sincronización completa (enviar pendientes + descargar cambios)
sealed interface ResultadoSincronizacion {
    data class Exito(val enviados: Int, val descargados: Int, val rechazados: Int) : ResultadoSincronizacion
    data object SinConexion : ResultadoSincronizacion
    data class ErrorReintentable(val motivo: String) : ResultadoSincronizacion  // timeout o 5xx: se puede reintentar
    data class ErrorPermanente(val motivo: String) : ResultadoSincronizacion    // 401, 4xx o datos inválidos: no se reintenta solo
    data object EnCurso : ResultadoSincronizacion                                // ya había otra sincronización corriendo
}

// Lo que ve la interfaz sobre la sincronización
sealed interface EstadoSincronizacion {
    data object Inactiva : EstadoSincronizacion
    data object EnCurso : EstadoSincronizacion
    data class Terminada(val resultado: ResultadoSincronizacion, val hora: Long) : EstadoSincronizacion
}
