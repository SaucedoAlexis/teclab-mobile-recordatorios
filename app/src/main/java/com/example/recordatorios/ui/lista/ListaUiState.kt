package com.example.recordatorios.ui.lista

import com.example.recordatorios.domain.Recordatorio

// Estados mutuamente excluyentes del listado: nunca carga y error a la vez
sealed interface ListaUiState {
    data object Cargando : ListaUiState
    data object Vacio : ListaUiState
    data class ConDatos(val recordatorios: List<Recordatorio>) : ListaUiState
    data class Error(val mensaje: String) : ListaUiState
}
