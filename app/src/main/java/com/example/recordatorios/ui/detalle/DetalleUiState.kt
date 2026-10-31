package com.example.recordatorios.ui.detalle

import com.example.recordatorios.domain.Recordatorio

sealed interface DetalleUiState {
    data object Cargando : DetalleUiState
    data class ConDatos(val recordatorio: Recordatorio) : DetalleUiState
    data class Error(val mensaje: String) : DetalleUiState
}
