package com.example.recordatorios.ui.detalle

import com.example.recordatorios.domain.Recordatorio

sealed interface DetalleUiState {
    data object Cargando : DetalleUiState
    // procesando: hay una acción en curso (completar o eliminar). mensajeError: falló la última acción.
    data class ConDatos(
        val recordatorio: Recordatorio,
        val procesando: Boolean = false,
        val mensajeError: String? = null
    ) : DetalleUiState
    data class Error(val mensaje: String) : DetalleUiState
    data object Eliminado : DetalleUiState
}
