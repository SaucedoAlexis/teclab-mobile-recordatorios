package com.example.recordatorios.ui.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.RecordatorioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Recibe solo el id que llega por la ruta y le pide el registro actualizado al repositorio
class DetalleViewModel(
    private val idRecordatorio: Long,
    private val repositorio: RecordatorioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Cargando)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val recordatorio = repositorio.obtenerRecordatorio(idRecordatorio)
            _uiState.update {
                if (recordatorio != null) DetalleUiState.ConDatos(recordatorio)
                else DetalleUiState.Error("No se encontró el recordatorio solicitado.")
            }
        }
    }
}
